package io.github.lldata.connect.runtime.grpc

import io.github.lldata.connect.runtime.StreamCallControl
import io.github.lldata.connect.spi.ConnectCallContext
import io.github.lldata.connect.spi.ConnectException
import io.github.lldata.connect.spi.ConnectStreamObserver
import io.grpc.Context
import io.grpc.Metadata
import io.grpc.MethodDescriptor
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor
import io.grpc.ServerInterceptors
import io.grpc.ServerMethodDefinition
import io.grpc.ServerServiceDefinition
import io.grpc.Status
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Invokes a gRPC server-streaming [ServerMethodDefinition] through optional interceptors,
 * forwarding each response message to [ConnectStreamObserver].
 */
object GrpcServerStreamInvoker {

    private val DEADLINE_SCHEDULER = Executors.newSingleThreadScheduledExecutor { r ->
        Thread(r, "connect-stream-deadline").apply { isDaemon = true }
    }

    fun <ReqT, RespT> invoke(
        method: ServerMethodDefinition<ReqT, RespT>,
        interceptors: List<ServerInterceptor>,
        request: ReqT,
        context: ConnectCallContext,
        observer: ConnectStreamObserver<RespT>,
        callControl: StreamCallControl? = null,
    ) {
        val definition = if (interceptors.isEmpty()) {
            ServerServiceDefinition.builder("connect-stream-bridge")
                .addMethod(method)
                .build()
                .getMethod(method.methodDescriptor.fullMethodName)
        } else {
            val base = ServerServiceDefinition.builder(method.methodDescriptor.serviceName)
                .addMethod(method)
                .build()
            val intercepted = ServerInterceptors.intercept(base, interceptors)
            intercepted.getMethod(method.methodDescriptor.fullMethodName)
                ?: error("Intercepted stream method missing: ${method.methodDescriptor.fullMethodName}")
        }

        @Suppress("UNCHECKED_CAST")
        val typed = definition as ServerMethodDefinition<ReqT, RespT>
        invokeHandler(typed.methodDescriptor, typed.serverCallHandler, request, context, observer, callControl)
    }

    private fun <ReqT, RespT> invokeHandler(
        descriptor: MethodDescriptor<ReqT, RespT>,
        handler: ServerCallHandler<ReqT, RespT>,
        request: ReqT,
        context: ConnectCallContext,
        observer: ConnectStreamObserver<RespT>,
        callControl: StreamCallControl?,
    ) {
        val timeoutMs = context.timeoutMs ?: 60_000L
        val deadlineCtx = Context.current().withDeadlineAfter(timeoutMs, TimeUnit.MILLISECONDS, DEADLINE_SCHEDULER)
        callControl?.attachGrpcCancel { deadlineCtx.cancel(Status.CANCELLED.asRuntimeException()) }
        val previous = deadlineCtx.attach()
        val finished = AtomicBoolean(false)

        val headers = toMetadata(context.requestHeaders)
        val call = object : ServerCall<ReqT, RespT>() {
            override fun request(numMessages: Int) = Unit

            override fun sendHeaders(headers: Metadata) {
                GrpcMetadataCopy.headersToContext(headers, context)
            }

            override fun sendMessage(message: RespT) {
                if (!finished.get() && callControl?.isCancelled() != true) {
                    observer.onNext(message)
                }
            }

            override fun close(status: Status, trailers: Metadata) {
                if (!finished.compareAndSet(false, true)) return
                try {
                    GrpcMetadataCopy.trailersToContext(trailers, context)
                    if (callControl?.isCancelled() == true) {
                        observer.onError(
                            ConnectException(
                                io.github.lldata.connect.spi.ConnectCode.CANCELED,
                                "Client cancelled the stream",
                            ),
                        )
                    } else if (status.isOk) {
                        observer.onCompleted()
                    } else {
                        observer.onError(GrpcStatusMapping.toConnectException(status.asRuntimeException(trailers)))
                    }
                } finally {
                    deadlineCtx.detach(previous)
                    deadlineCtx.cancel(null)
                }
            }

            override fun isCancelled(): Boolean =
                deadlineCtx.isCancelled || finished.get() || callControl?.isCancelled() == true

            override fun getMethodDescriptor(): MethodDescriptor<ReqT, RespT> = descriptor
        }

        try {
            val listener = handler.startCall(call, headers)
            listener.onReady()
            listener.onMessage(request)
            listener.onHalfClose()
        } catch (e: Exception) {
            if (finished.compareAndSet(false, true)) {
                deadlineCtx.detach(previous)
                deadlineCtx.cancel(null)
                observer.onError(GrpcStatusMapping.toConnectException(e))
            }
        }
    }

    private fun toMetadata(headers: Map<String, List<String>>): Metadata {
        val metadata = Metadata()
        for ((name, values) in headers) {
            val lower = name.lowercase()
            if (lower in HOP_BY_HOP || lower.startsWith(":")) continue
            if (lower.endsWith(Metadata.BINARY_HEADER_SUFFIX)) continue
            val key = Metadata.Key.of(lower, Metadata.ASCII_STRING_MARSHALLER)
            for (value in values) {
                metadata.put(key, value)
            }
        }
        return metadata
    }

    private val HOP_BY_HOP = setOf(
        "connection", "keep-alive", "proxy-authenticate", "proxy-authorization",
        "te", "trailer", "transfer-encoding", "upgrade", "host", "content-length",
        "content-type", "accept", "accept-encoding", "content-encoding",
        "connect-protocol-version", "connect-content-encoding",
        "connect-accept-encoding",
    )
}

package io.github.lldata.connect.runtime.grpc

import io.github.lldata.connect.spi.ConnectCallContext
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
 * Invokes a gRPC BIDI_STREAMING [ServerMethodDefinition] through optional interceptors.
 *
 * Vertical slice: all request messages are delivered up-front, then the call is
 * half-closed; each response message is forwarded to [observer].
 */
object GrpcBidiStreamInvoker {

    private val DEADLINE_SCHEDULER = Executors.newSingleThreadScheduledExecutor { r ->
        Thread(r, "connect-bidi-deadline").apply { isDaemon = true }
    }

    fun <ReqT, RespT> invoke(
        method: ServerMethodDefinition<ReqT, RespT>,
        interceptors: List<ServerInterceptor>,
        requests: List<ReqT>,
        context: ConnectCallContext,
        observer: ConnectStreamObserver<RespT>,
    ) {
        val definition = if (interceptors.isEmpty()) {
            ServerServiceDefinition.builder("connect-bidi-bridge")
                .addMethod(method)
                .build()
                .getMethod(method.methodDescriptor.fullMethodName)
        } else {
            val base = ServerServiceDefinition.builder(method.methodDescriptor.serviceName)
                .addMethod(method)
                .build()
            val intercepted = ServerInterceptors.intercept(base, interceptors)
            intercepted.getMethod(method.methodDescriptor.fullMethodName)
                ?: error("Intercepted bidi method missing: ${method.methodDescriptor.fullMethodName}")
        }

        @Suppress("UNCHECKED_CAST")
        val typed = definition as ServerMethodDefinition<ReqT, RespT>
        invokeHandler(typed.methodDescriptor, typed.serverCallHandler, requests, context, observer)
    }

    private fun <ReqT, RespT> invokeHandler(
        descriptor: MethodDescriptor<ReqT, RespT>,
        handler: ServerCallHandler<ReqT, RespT>,
        requests: List<ReqT>,
        context: ConnectCallContext,
        observer: ConnectStreamObserver<RespT>,
    ) {
        val timeoutMs = context.timeoutMs ?: 60_000L
        val deadlineCtx = Context.current().withDeadlineAfter(timeoutMs, TimeUnit.MILLISECONDS, DEADLINE_SCHEDULER)
        val previous = deadlineCtx.attach()
        val finished = AtomicBoolean(false)

        val headers = toMetadata(context.requestHeaders)
        val call = object : ServerCall<ReqT, RespT>() {
            override fun request(numMessages: Int) = Unit

            override fun sendHeaders(headers: Metadata) {
                for (key in headers.keys()) {
                    if (key.endsWith(Metadata.BINARY_HEADER_SUFFIX)) continue
                    val metaKey = Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER)
                    val value = headers.get(metaKey) ?: continue
                    context.responseHeader(key, value)
                }
            }

            override fun sendMessage(message: RespT) {
                if (!finished.get()) {
                    observer.onNext(message)
                }
            }

            override fun close(status: Status, trailers: Metadata) {
                if (!finished.compareAndSet(false, true)) return
                try {
                    for (key in trailers.keys()) {
                        if (key.endsWith(Metadata.BINARY_HEADER_SUFFIX)) continue
                        val metaKey = Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER)
                        val value = trailers.get(metaKey) ?: continue
                        context.responseTrailer(key, value)
                    }
                    if (status.isOk) {
                        observer.onCompleted()
                    } else {
                        observer.onError(GrpcStatusMapping.toConnectException(status.asRuntimeException(trailers)))
                    }
                } finally {
                    deadlineCtx.detach(previous)
                    deadlineCtx.cancel(null)
                }
            }

            override fun isCancelled(): Boolean = deadlineCtx.isCancelled || finished.get()

            override fun getMethodDescriptor(): MethodDescriptor<ReqT, RespT> = descriptor
        }

        try {
            val listener = handler.startCall(call, headers)
            listener.onReady()
            for (request in requests) {
                listener.onMessage(request)
            }
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
        "connect-protocol-version", "connect-timeout-ms", "connect-content-encoding",
        "connect-accept-encoding",
    )
}

package io.github.lldata.connect.runtime.grpc

import io.github.lldata.connect.spi.ConnectCallContext
import io.github.lldata.connect.spi.ConnectCode
import io.github.lldata.connect.spi.ConnectException
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
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Invokes a gRPC CLIENT_STREAMING [ServerMethodDefinition] through optional interceptors.
 */
object GrpcClientStreamInvoker {

    private val DEADLINE_SCHEDULER = Executors.newSingleThreadScheduledExecutor { r ->
        Thread(r, "connect-client-stream-deadline").apply { isDaemon = true }
    }

    fun <ReqT, RespT> invoke(
        method: ServerMethodDefinition<ReqT, RespT>,
        interceptors: List<ServerInterceptor>,
        requests: List<ReqT>,
        context: ConnectCallContext,
    ): RespT {
        val definition = if (interceptors.isEmpty()) {
            ServerServiceDefinition.builder("connect-client-stream-bridge")
                .addMethod(method)
                .build()
                .getMethod(method.methodDescriptor.fullMethodName)
        } else {
            val base = ServerServiceDefinition.builder(method.methodDescriptor.serviceName)
                .addMethod(method)
                .build()
            val intercepted = ServerInterceptors.intercept(base, interceptors)
            intercepted.getMethod(method.methodDescriptor.fullMethodName)
                ?: error("Intercepted client-stream method missing: ${method.methodDescriptor.fullMethodName}")
        }

        @Suppress("UNCHECKED_CAST")
        val typed = definition as ServerMethodDefinition<ReqT, RespT>
        return invokeHandler(typed.methodDescriptor, typed.serverCallHandler, requests, context)
    }

    private fun <ReqT, RespT> invokeHandler(
        descriptor: MethodDescriptor<ReqT, RespT>,
        handler: ServerCallHandler<ReqT, RespT>,
        requests: List<ReqT>,
        context: ConnectCallContext,
    ): RespT {
        val responseRef = AtomicReference<RespT>()
        val errorRef = AtomicReference<Throwable>()
        val latch = CountDownLatch(1)

        val headers = toMetadata(context.requestHeaders)
        val timeoutMs = context.timeoutMs ?: 60_000L
        val deadlineCtx = Context.current().withDeadlineAfter(timeoutMs, TimeUnit.MILLISECONDS, DEADLINE_SCHEDULER)
        val previous = deadlineCtx.attach()
        try {
            val call = object : ServerCall<ReqT, RespT>() {
                private var closed = false

                override fun request(numMessages: Int) = Unit

                override fun sendHeaders(headers: Metadata) {
                    GrpcMetadataCopy.headersToContext(headers, context)
                }

                override fun sendMessage(message: RespT) {
                    responseRef.set(message)
                }

                override fun close(status: Status, trailers: Metadata) {
                    if (closed) return
                    closed = true
                    GrpcMetadataCopy.trailersToContext(trailers, context)
                    if (!status.isOk) {
                        errorRef.set(status.asRuntimeException(trailers))
                    }
                    latch.countDown()
                }

                override fun isCancelled(): Boolean = deadlineCtx.isCancelled

                override fun getMethodDescriptor(): MethodDescriptor<ReqT, RespT> = descriptor
            }

            val listener = handler.startCall(call, headers)
            listener.onReady()
            for (request in requests) {
                listener.onMessage(request)
            }
            listener.onHalfClose()

            if (!latch.await(timeoutMs, TimeUnit.MILLISECONDS)) {
                throw ConnectException(ConnectCode.DEADLINE_EXCEEDED, "RPC timed out after ${timeoutMs}ms")
            }

            errorRef.get()?.let { throw GrpcStatusMapping.toConnectException(it) }
            return responseRef.get()
                ?: throw ConnectException(
                    ConnectCode.INTERNAL,
                    "gRPC client-stream handler completed without a response message",
                )
        } finally {
            deadlineCtx.detach(previous)
            deadlineCtx.cancel(null)
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

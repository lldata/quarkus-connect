package io.github.lldata.connect.runtime.grpc

import com.google.protobuf.Message
import io.github.lldata.connect.runtime.security.ConnectSecuritySupport
import io.github.lldata.connect.spi.ConnectCallContext
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
 * Invokes a unary gRPC [ServerMethodDefinition] (optionally through [ServerInterceptor]s)
 * and returns the response message or throws [ConnectException].
 *
 * Designed to run off the Vert.x event loop (e.g. via `executeBlocking`) because Mutiny /
 * async gRPC handlers complete on another thread while this waits on a latch.
 */
object GrpcUnaryInvoker {

    fun <ReqT, RespT> invoke(
        method: ServerMethodDefinition<ReqT, RespT>,
        interceptors: List<ServerInterceptor>,
        request: ReqT,
        context: ConnectCallContext,
    ): RespT = ConnectSecuritySupport.withRequestContext {
        val definition = if (interceptors.isEmpty()) {
            ServerServiceDefinition.builder("connect-bridge")
                .addMethod(method)
                .build()
                .getMethod(method.methodDescriptor.fullMethodName)
        } else {
            val base = ServerServiceDefinition.builder(method.methodDescriptor.serviceName)
                .addMethod(method)
                .build()
            val intercepted = ServerInterceptors.intercept(base, interceptors)
            intercepted.getMethod(method.methodDescriptor.fullMethodName)
                ?: error("Intercepted method missing: ${method.methodDescriptor.fullMethodName}")
        }

        @Suppress("UNCHECKED_CAST")
        val typed = definition as ServerMethodDefinition<ReqT, RespT>
        invokeHandler(typed.methodDescriptor, typed.serverCallHandler, request, context)
    }

    private fun <ReqT, RespT> invokeHandler(
        descriptor: MethodDescriptor<ReqT, RespT>,
        handler: ServerCallHandler<ReqT, RespT>,
        request: ReqT,
        context: ConnectCallContext,
    ): RespT {
        val responseRef = AtomicReference<RespT>()
        val errorRef = AtomicReference<Throwable>()
        val latch = CountDownLatch(1)

        val headers = toMetadata(context.requestHeaders)
        val timeoutMs = context.timeoutMs ?: 30_000L
        val deadlineCtx = Context.current().withDeadlineAfter(
            timeoutMs,
            TimeUnit.MILLISECONDS,
            DEADLINE_SCHEDULER,
        )
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
            listener.onMessage(request)
            listener.onHalfClose()

            if (!latch.await(timeoutMs, TimeUnit.MILLISECONDS)) {
                throw ConnectException(
                    io.github.lldata.connect.spi.ConnectCode.DEADLINE_EXCEEDED,
                    "RPC timed out after ${timeoutMs}ms",
                )
            }

            errorRef.get()?.let { throw GrpcStatusMapping.toConnectException(it) }
            @Suppress("UNCHECKED_CAST")
            return responseRef.get()
                ?: throw ConnectException(
                    io.github.lldata.connect.spi.ConnectCode.INTERNAL,
                    "gRPC handler completed without a response message",
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
            // Skip hop-by-hop / HTTP headers that must not become gRPC metadata.
            if (lower in HOP_BY_HOP || lower.startsWith(":")) continue
            if (lower.endsWith(Metadata.BINARY_HEADER_SUFFIX)) {
                val key = Metadata.Key.of(lower, Metadata.BINARY_BYTE_MARSHALLER)
                for (value in values) {
                    metadata.put(key, java.util.Base64.getDecoder().decode(value))
                }
            } else {
                val key = Metadata.Key.of(lower, Metadata.ASCII_STRING_MARSHALLER)
                for (value in values) {
                    metadata.put(key, value)
                }
            }
        }
        return metadata
    }

    private val DEADLINE_SCHEDULER = Executors.newSingleThreadScheduledExecutor { r ->
        Thread(r, "connect-deadline").apply { isDaemon = true }
    }

    private val HOP_BY_HOP = setOf(
        "connection",
        "keep-alive",
        "proxy-authenticate",
        "proxy-authorization",
        "te",
        "trailer",
        "transfer-encoding",
        "upgrade",
        "host",
        "content-length",
        "content-type",
        "accept",
        "accept-encoding",
        "content-encoding",
        "connect-protocol-version",
        // connect-timeout-ms is forwarded so handlers can echo the client timeout
        // (deadline is still applied separately from ConnectCallContext.timeoutMs).
    )
}

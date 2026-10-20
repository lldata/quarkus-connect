package io.github.lldata.connect.runtime.grpc

import com.google.protobuf.Message
import io.github.lldata.connect.spi.ConnectCallContext
import io.github.lldata.connect.spi.ConnectException
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
    ): RespT {
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
        return invokeHandler(typed.methodDescriptor, typed.serverCallHandler, request, context)
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
        val call = object : ServerCall<ReqT, RespT>() {
            private var closed = false

            override fun request(numMessages: Int) = Unit

            override fun sendHeaders(headers: Metadata) {
                for (key in headers.keys()) {
                    if (key.endsWith(Metadata.BINARY_HEADER_SUFFIX)) {
                        val metaKey = Metadata.Key.of(key, Metadata.BINARY_BYTE_MARSHALLER)
                        val value = headers.get(metaKey) ?: continue
                        context.responseHeader(key, java.util.Base64.getEncoder().encodeToString(value))
                    } else {
                        val metaKey = Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER)
                        val value = headers.get(metaKey) ?: continue
                        context.responseHeader(key, value)
                    }
                }
            }

            override fun sendMessage(message: RespT) {
                responseRef.set(message)
            }

            override fun close(status: Status, trailers: Metadata) {
                if (closed) return
                closed = true
                for (key in trailers.keys()) {
                    if (key.endsWith(Metadata.BINARY_HEADER_SUFFIX)) {
                        val metaKey = Metadata.Key.of(key, Metadata.BINARY_BYTE_MARSHALLER)
                        val value = trailers.get(metaKey) ?: continue
                        context.responseTrailer(key, java.util.Base64.getEncoder().encodeToString(value))
                    } else {
                        val metaKey = Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER)
                        val value = trailers.get(metaKey) ?: continue
                        context.responseTrailer(key, value)
                    }
                }
                if (!status.isOk) {
                    errorRef.set(status.asRuntimeException(trailers))
                }
                latch.countDown()
            }

            override fun isCancelled(): Boolean = false

            override fun getMethodDescriptor(): MethodDescriptor<ReqT, RespT> = descriptor
        }

        val listener = handler.startCall(call, headers)
        listener.onReady()
        listener.onMessage(request)
        listener.onHalfClose()

        val timeoutMs = context.timeoutMs ?: 30_000L
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
        "connect-timeout-ms",
    )
}

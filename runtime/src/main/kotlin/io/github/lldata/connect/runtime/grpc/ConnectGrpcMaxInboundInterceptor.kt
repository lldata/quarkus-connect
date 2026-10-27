package io.github.lldata.connect.runtime.grpc

import com.google.protobuf.MessageLite
import io.github.lldata.connect.runtime.ConnectRuntimeConfig
import io.grpc.ForwardingServerCallListener
import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor
import io.grpc.Status
import io.quarkus.grpc.GlobalInterceptor
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject

/**
 * Enforces [ConnectRuntimeConfig.maxInboundMessageSize] on native gRPC after the message is
 * deserialized (post-decompress). Quarkus/Netty's `quarkus.grpc.server.max-inbound-message-size`
 * measures the framed/compressed length; gzip cases that decompress above the limit still need
 * this check so PROTOCOL_GRPC matches Connect / gRPC-Web.
 */
@ApplicationScoped
@GlobalInterceptor
class ConnectGrpcMaxInboundInterceptor
@Inject
constructor(
    private val config: ConnectRuntimeConfig,
) : ServerInterceptor {
    override fun <ReqT, RespT> interceptCall(
        call: ServerCall<ReqT, RespT>,
        headers: Metadata,
        next: ServerCallHandler<ReqT, RespT>,
    ): ServerCall.Listener<ReqT> {
        val max = config.maxInboundMessageSize()
        val listener = next.startCall(call, headers)
        if (max <= 0L) {
            return listener
        }
        return object : ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT>(listener) {
            private var closed = false

            override fun onMessage(message: ReqT) {
                if (closed) return
                val size = measure(message)
                if (size > max) {
                    closed = true
                    call.close(
                        Status.RESOURCE_EXHAUSTED.withDescription(
                            "message size $size exceeds limit $max bytes",
                        ),
                        Metadata(),
                    )
                    return
                }
                super.onMessage(message)
            }
        }
    }

    private fun measure(message: Any?): Long =
        when (message) {
            is MessageLite -> message.serializedSize.toLong()
            is ByteArray -> message.size.toLong()
            else -> 0L
        }
}

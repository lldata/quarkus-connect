package io.github.lldata.connect.conformance

import io.grpc.Context
import io.grpc.Contexts
import io.grpc.ForwardingServerCall
import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor
import io.grpc.Status
import io.quarkus.grpc.GlobalInterceptor
import jakarta.enterprise.context.ApplicationScoped

/** Holds response headers/trailers the service wants attached on sendHeaders/close. */
class PendingResponseMeta {
    @Volatile
    var headers: Metadata? = null

    @Volatile
    var trailers: Metadata? = null
}

object ConformanceGrpcKeys {
    val REQUEST_HEADERS: Context.Key<Metadata> = Context.key("conformance-request-headers")
    val PENDING: Context.Key<PendingResponseMeta> = Context.key("conformance-pending-meta")
}

/**
 * Captures inbound metadata and merges service-provided response headers/trailers.
 * Required because Mutiny `@GrpcService` handlers cannot call [ServerCall.sendHeaders] directly.
 */
@ApplicationScoped
@GlobalInterceptor
class ConformanceMetaInterceptor : ServerInterceptor {
    override fun <ReqT, RespT> interceptCall(
        call: ServerCall<ReqT, RespT>,
        headers: Metadata,
        next: ServerCallHandler<ReqT, RespT>,
    ): ServerCall.Listener<ReqT> {
        val pending = PendingResponseMeta()
        val ctx =
            Context.current()
                .withValue(ConformanceGrpcKeys.REQUEST_HEADERS, headers)
                .withValue(ConformanceGrpcKeys.PENDING, pending)

        val forwarding =
            object : ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT>(call) {
                @Volatile
                private var headersSent = false

                override fun sendHeaders(responseHeaders: Metadata) {
                    pending.headers?.let { responseHeaders.merge(it) }
                    headersSent = true
                    super.sendHeaders(responseHeaders)
                }

                override fun close(status: Status, trailers: Metadata) {
                    pending.trailers?.let { trailers.merge(it) }
                    // Error / empty-stream paths often close without sendHeaders; flush pending
                    // response headers so the Connect bridge can mirror them on the HTTP response.
                    if (!headersSent) {
                        val hdrs = Metadata()
                        pending.headers?.let { hdrs.merge(it) }
                        headersSent = true
                        super.sendHeaders(hdrs)
                    }
                    super.close(status, trailers)
                }
            }
        return Contexts.interceptCall(ctx, forwarding, headers, next)
    }
}

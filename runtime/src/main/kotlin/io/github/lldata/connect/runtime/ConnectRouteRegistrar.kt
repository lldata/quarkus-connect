package io.github.lldata.connect.runtime

import io.github.lldata.connect.protocol.ProtobufCodecSupport
import io.vertx.core.Handler
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpHeaders
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Observes
import jakarta.inject.Inject
import org.jboss.logging.Logger
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Registers Vert.x routes for Connect + gRPC-Web on the Quarkus HTTP port.
 *
 * quarkus-grpc (`use-separate-server=false`) installs a catch-all at default order 0 whose
 * `isGrpc` gate matches `^application/grpc.*` on HTTP/2 only. That pattern also matches
 * `application/grpc-web*`, so Vert.x GrpcServer would otherwise steal gRPC-Web on h2c and
 * respond with `application/grpc` (GOAWAY / wrong content-type). We claim every
 * `application/grpc-web*` request first at a negative order and leave native
 * `application/grpc` to quarkus-grpc via [ConnectUnaryHttpHandler]'s `ctx.next()`.
 */
@ApplicationScoped
class ConnectRouteRegistrar {

    @Inject
    private lateinit var registry: ConnectMethodRegistry

    @Inject
    private lateinit var config: ConnectRuntimeConfig

    private val log = Logger.getLogger(ConnectRouteRegistrar::class.java)

    fun register(@Observes router: Router) {
        val handler = ConnectUnaryHttpHandler(
            registry,
            ProtobufCodecSupport(preserveProtoFieldNames = config.json().preserveProtoFieldNames()),
            maxInboundMessageSize = config.maxInboundMessageSize(),
        )

        // Before quarkus-grpc (order 0): own all gRPC-Web Content-Types by prefix.
        // Enumerated Route.consumes is insufficient — e.g. application/grpc-web+foo still
        // matches quarkus-grpc's ^application/grpc.* and GOAWAYs the HTTP/2 connection.
        router.routeWithRegex("^/.+/.+$")
            .order(GRPC_WEB_PREEMPT_ORDER)
            .handler(GrpcWebPreemptHandler(handler))

        // After quarkus-grpc declines (HTTP/1 or non-grpc Content-Type): Connect (+ gRPC-Web backup).
        router.routeWithRegex("^/.+/.+$")
            .order(CONNECT_FALLBACK_ORDER)
            .handler(BodyHandler.create())
            .handler(handler)

        log.info(
            "Connect RPC enabled; unary=${registry.procedures()} " +
                "serverStreams=${registry.streamProcedures()} " +
                "clientStreams=${registry.clientStreamProcedures()} " +
                "bidiStreams=${registry.bidiStreamProcedures()}",
        )
    }

    companion object {
        /** Must run before quarkus-grpc's default route order (0). */
        const val GRPC_WEB_PREEMPT_ORDER = -10_000

        /** After quarkus-grpc so native `application/grpc` on h2 is not body-buffered here. */
        const val CONNECT_FALLBACK_ORDER = 10_000
    }
}

/**
 * Single-handler early route: non-gRPC-Web `next()` goes to the next route without buffering.
 *
 * For `application/grpc-web*`, resume the (possibly paused) request, buffer the body, and
 * dispatch to [connectHandler] so quarkus-grpc never sees the request on HTTP/2.
 *
 * Note: [BodyHandler] cannot be used via a `RoutingContext` delegate — it casts to
 * `RoutingContextInternal`.
 */
internal class GrpcWebPreemptHandler(
    private val connectHandler: Handler<RoutingContext>,
) : Handler<RoutingContext> {

    override fun handle(ctx: RoutingContext) {
        val media = mediaType(ctx.request().getHeader(HttpHeaders.CONTENT_TYPE))
        if (!media.startsWith("application/grpc-web")) {
            ctx.next()
            return
        }
        val req = ctx.request()
        val done = AtomicBoolean(false)
        req.body()
            .onSuccess { buffer ->
                if (!done.compareAndSet(false, true)) return@onSuccess
                ctx.setBody(buffer ?: Buffer.buffer())
                connectHandler.handle(ctx)
            }
            .onFailure { err ->
                if (!done.compareAndSet(false, true)) return@onFailure
                ctx.fail(err)
            }
        // Vert.x Web pauses the request until a body-consuming handler resumes it.
        if (!req.isEnded) {
            req.resume()
        }
    }

    private fun mediaType(contentType: String?): String {
        if (contentType.isNullOrBlank()) return ""
        return contentType.substringBefore(';').trim().lowercase()
    }
}

package io.github.lldata.connect.runtime

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

/**
 * Registers Vert.x routes for Connect + gRPC-Web on the Quarkus HTTP port.
 *
 * quarkus-grpc (`use-separate-server=false`) installs a catch-all at default order 0 whose
 * `isGrpc` gate matches `^application/grpc.*` on HTTP/2 only. That pattern also matches
 * `application/grpc-web*`, so Vert.x GrpcServer would otherwise steal gRPC-Web on h2c and
 * respond with `application/grpc` (GOAWAY / wrong content-type). We claim gRPC-Web first at
 * a negative order and leave native `application/grpc` to quarkus-grpc via
 * [ConnectUnaryHttpHandler]'s `ctx.next()`.
 */
@ApplicationScoped
class ConnectRouteRegistrar {

    @Inject
    private lateinit var registry: ConnectMethodRegistry

    private val log = Logger.getLogger(ConnectRouteRegistrar::class.java)

    fun register(@Observes router: Router) {
        val handler = ConnectUnaryHttpHandler(registry)

        // Before quarkus-grpc (order 0): own gRPC-Web. Route matching uses consumes so
        // native application/grpc never enters this route (BodyHandler must not run for it).
        val grpcWeb = router.routeWithRegex("^/.+/.+$").order(GRPC_WEB_PREEMPT_ORDER)
        for (ct in GRPC_WEB_CONTENT_TYPES) {
            grpcWeb.consumes(ct)
        }
        grpcWeb.handler(BodyHandler.create()).handler(handler)

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

        /** MIME types Vert.x `consumes` must list explicitly (no prefix match for `+` suffixes). */
        val GRPC_WEB_CONTENT_TYPES = listOf(
            "application/grpc-web",
            "application/grpc-web+proto",
            "application/grpc-web+json",
            "application/grpc-web-text",
            "application/grpc-web-text+proto",
            "application/grpc-web-text+json",
        )
    }
}

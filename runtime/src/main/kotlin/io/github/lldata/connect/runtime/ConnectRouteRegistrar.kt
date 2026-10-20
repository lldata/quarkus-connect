package io.github.lldata.connect.runtime

import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Observes
import jakarta.inject.Inject
import org.jboss.logging.Logger

/**
 * Registers a catch-all Vert.x route for Connect unary procedures, similar in spirit
 * to quarkus-grpc's HTTP/2 + Vert.x integration (HTTP/1.1 unary works for Connect).
 *
 * Observes [Router] (Quarkus fires this when the HTTP router is ready).
 */
@ApplicationScoped
class ConnectRouteRegistrar {

    @Inject
    private lateinit var registry: ConnectMethodRegistry

    private val log = Logger.getLogger(ConnectRouteRegistrar::class.java)

    fun register(@Observes router: Router) {
        val handler = ConnectUnaryHttpHandler(registry)
        // Match /package.Service/Method — BodyHandler so POST bodies are available
        router.routeWithRegex("^/.+/.+$")
            .order(10_000)
            .handler(BodyHandler.create())
            .handler(handler)

        log.info(
            "Connect RPC enabled; unary=${registry.procedures()} streams=${registry.streamProcedures()}",
        )
    }
}

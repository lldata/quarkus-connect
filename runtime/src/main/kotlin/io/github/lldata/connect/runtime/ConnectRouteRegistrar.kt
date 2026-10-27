package io.github.lldata.connect.runtime

import io.github.lldata.connect.protocol.ConnectEnvelope
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
 *
 * Client and bidi streaming skip [BodyHandler] / full-body buffering and read envelopes
 * incrementally (see [StreamingRequestReader]).
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

        router.routeWithRegex("^/.+/.+$")
            .order(GRPC_WEB_PREEMPT_ORDER)
            .handler(GrpcWebPreemptHandler(handler, registry))

        router.routeWithRegex("^/.+/.+$")
            .order(CONNECT_FALLBACK_ORDER)
            .handler(ConnectBodyGateHandler(registry, handler))
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
        const val GRPC_WEB_PREEMPT_ORDER = -10_000
        const val CONNECT_FALLBACK_ORDER = 10_000
    }
}

/**
 * For Connect client/bidi streaming Content-Types, skip [BodyHandler] and dispatch
 * directly with [StreamingRequestReader.STREAM_BODY_KEY] set.
 */
internal class ConnectBodyGateHandler(
    private val registry: ConnectMethodRegistry,
    private val connectHandler: Handler<RoutingContext>,
) : Handler<RoutingContext> {

    override fun handle(ctx: RoutingContext) {
        if (shouldStreamRequestBody(registry, ctx)) {
            ctx.put(StreamingRequestReader.STREAM_BODY_KEY, true)
            connectHandler.handle(ctx)
            return
        }
        ctx.next()
    }
}

/**
 * Early route: non-gRPC-Web `next()` without buffering.
 * gRPC-Web unary/server-stream buffers; client/bidi streams envelopes.
 */
internal class GrpcWebPreemptHandler(
    private val connectHandler: Handler<RoutingContext>,
    private val registry: ConnectMethodRegistry,
) : Handler<RoutingContext> {

    override fun handle(ctx: RoutingContext) {
        val media = mediaType(ctx.request().getHeader(HttpHeaders.CONTENT_TYPE))
        if (!media.startsWith("application/grpc-web")) {
            ctx.next()
            return
        }
        if (shouldStreamRequestBody(registry, ctx)) {
            ctx.put(StreamingRequestReader.STREAM_BODY_KEY, true)
            connectHandler.handle(ctx)
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
        if (!req.isEnded) {
            req.resume()
        }
    }
}

internal fun shouldStreamRequestBody(registry: ConnectMethodRegistry, ctx: RoutingContext): Boolean {
    val media = mediaType(ctx.request().getHeader(HttpHeaders.CONTENT_TYPE))
    val procedure = ConnectMethodRegistry.normalizeProcedure(ctx.request().path())
    val clientOrBidi =
        registry.findClientStream(procedure) != null || registry.findBidiStream(procedure) != null
    if (!clientOrBidi) return false
    return ConnectEnvelope.isConnectStreamingContentType(media) ||
        media.startsWith("application/grpc-web")
}

private fun mediaType(contentType: String?): String {
    if (contentType.isNullOrBlank()) return ""
    return contentType.substringBefore(';').trim().lowercase()
}

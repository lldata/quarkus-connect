package io.github.lldata.connect.runtime

import com.google.protobuf.Message
import io.github.lldata.connect.protocol.ConnectErrorJson
import io.github.lldata.connect.protocol.ConnectProtocolConstants
import io.github.lldata.connect.protocol.ProtobufCodecSupport
import io.github.lldata.connect.spi.ConnectCodec
import io.github.lldata.connect.spi.ConnectCode
import io.github.lldata.connect.spi.ConnectException
import io.github.lldata.connect.spi.ConnectUnaryMethod
import io.vertx.core.Handler
import io.vertx.core.http.HttpHeaders
import io.vertx.core.http.HttpMethod
import io.vertx.ext.web.RoutingContext
import org.jboss.logging.Logger
import java.nio.charset.StandardCharsets

/**
 * Vert.x route handler implementing Connect unary POST (+ optional GET for idempotent methods later).
 *
 * Content types: [ConnectCodec.PROTO] / [ConnectCodec.JSON].
 * Errors: non-200 + `application/json` Connect error body.
 * Trailers: emitted as ordinary response headers prefixed with `trailer-`.
 */
class ConnectUnaryHttpHandler(
    private val registry: ConnectMethodRegistry,
) : Handler<RoutingContext> {

    private val log = Logger.getLogger(ConnectUnaryHttpHandler::class.java)

    override fun handle(ctx: RoutingContext) {
        val request = ctx.request()
        if (request.method() != HttpMethod.POST && request.method() != HttpMethod.GET) {
            writeError(ctx, ConnectException(ConnectCode.UNIMPLEMENTED, "Only GET and POST are supported for Connect unary"))
            return
        }

        val procedure = ConnectMethodRegistry.normalizeProcedure(request.path())
        val method = registry.find(procedure)
        if (method == null) {
            // Do not steal unrelated Vert.x / REST routes.
            ctx.next()
            return
        }

        if (request.method() == HttpMethod.GET) {
            handleGet(ctx, procedure, method)
            return
        }

        val version = request.getHeader(ConnectProtocolConstants.PROTOCOL_VERSION_HEADER)
        if (version != null && version != ConnectProtocolConstants.PROTOCOL_VERSION) {
            writeError(ctx, ConnectException.invalidArgument("Unsupported connect-protocol-version: $version"))
            return
        }

        val codec = ConnectCodec.fromContentType(request.getHeader(HttpHeaders.CONTENT_TYPE))
        if (codec == null) {
            ctx.response()
                .setStatusCode(415)
                .putHeader(HttpHeaders.CONTENT_TYPE, ConnectProtocolConstants.CONTENT_TYPE_JSON)
                .end(ConnectErrorJson.toJson(ConnectException.invalidArgument("Unsupported Content-Type; use application/proto or application/json")))
            return
        }

        val encoding = request.getHeader(HttpHeaders.CONTENT_ENCODING)
        if (encoding != null && !encoding.equals("identity", ignoreCase = true)) {
            writeError(
                ctx,
                ConnectException(
                    ConnectCode.UNIMPLEMENTED,
                    "Unsupported Content-Encoding '$encoding'; supported: identity",
                ),
            )
            return
        }

        val timeoutMs = parseTimeout(request.getHeader(ConnectProtocolConstants.TIMEOUT_HEADER))
        val headers = collectHeaders(ctx)
        val body = ctx.body().buffer()?.bytes ?: ByteArray(0)
        invokeMethod(ctx, procedure, method, codec, headers, timeoutMs, body)
    }

    private fun handleGet(
        ctx: RoutingContext,
        procedure: String,
        method: ConnectUnaryMethod<out Message, out Message>,
    ) {
        val params = ctx.queryParams()
        val connectVersion = params.get("connect")
        if (connectVersion != null && connectVersion != ConnectProtocolConstants.PROTOCOL_VERSION) {
            writeError(ctx, ConnectException.invalidArgument("Unsupported connect query version: $connectVersion"))
            return
        }

        val codec = ConnectCodec.fromWireName(params.get("encoding"))
            ?: ConnectCodec.JSON
        val compression = params.get("compression") ?: "identity"
        if (!compression.equals("identity", ignoreCase = true)) {
            writeError(
                ctx,
                ConnectException(
                    ConnectCode.UNIMPLEMENTED,
                    "Unsupported compression '$compression'; supported: identity",
                ),
            )
            return
        }

        val messageParam = params.get("message") ?: ""
        val useBase64 = params.contains("base64")
        val body = try {
            if (useBase64) {
                java.util.Base64.getUrlDecoder().decode(messageParam.replace(' ', '+'))
            } else {
                messageParam.toByteArray(StandardCharsets.UTF_8)
            }
        } catch (e: IllegalArgumentException) {
            writeError(ctx, ConnectException.invalidArgument("Invalid base64 message: ${e.message}"))
            return
        }

        val timeoutMs = parseTimeout(ctx.request().getHeader(ConnectProtocolConstants.TIMEOUT_HEADER))
        invokeMethod(ctx, procedure, method, codec, collectHeaders(ctx), timeoutMs, body)
    }

    private fun invokeMethod(
        ctx: RoutingContext,
        procedure: String,
        method: ConnectUnaryMethod<out Message, out Message>,
        codec: ConnectCodec,
        headers: Map<String, List<String>>,
        timeoutMs: Long?,
        body: ByteArray,
    ) {
        val callContext = DefaultConnectCallContext(procedure, codec, headers, timeoutMs)
        try {
            @Suppress("UNCHECKED_CAST")
            val typed = method as ConnectUnaryMethod<Message, Message>
            val requestMessage = ProtobufCodecSupport.parse(body, typed.requestType(), codec)
            val responseMessage = typed.handle(requestMessage, callContext)
            val responseBytes = ProtobufCodecSupport.encode(responseMessage, codec)

            val response = ctx.response().setStatusCode(200)
            response.putHeader(HttpHeaders.CONTENT_TYPE, codec.contentType)
            writeMetadata(response, callContext)
            response.end(io.vertx.core.buffer.Buffer.buffer(responseBytes))
        } catch (e: ConnectException) {
            writeError(ctx, e, callContext)
        } catch (e: Exception) {
            log.error("Unhandled Connect handler failure for /$procedure", e)
            writeError(ctx, ConnectException.internal(e.message ?: "Internal error", e), callContext)
        }
    }

    private fun writeError(
        ctx: RoutingContext,
        error: ConnectException,
        callContext: DefaultConnectCallContext? = null,
    ) {
        if (ctx.response().headWritten()) {
            log.warn("Cannot write Connect error; response already started: ${error.code.wireName}")
            return
        }
        val response = ctx.response().setStatusCode(error.code.httpStatus)
        response.putHeader(HttpHeaders.CONTENT_TYPE, ConnectProtocolConstants.CONTENT_TYPE_JSON)
        if (callContext != null) {
            writeMetadata(response, callContext)
        }
        response.end(ConnectErrorJson.toJson(error))
    }

    private fun writeMetadata(
        response: io.vertx.core.http.HttpServerResponse,
        callContext: DefaultConnectCallContext,
    ) {
        for ((name, values) in callContext.snapshotHeaders()) {
            for (value in values) {
                response.putHeader(name, value)
            }
        }
        for ((name, values) in callContext.snapshotTrailers()) {
            for (value in values) {
                response.putHeader(ConnectProtocolConstants.TRAILER_PREFIX + name, value)
            }
        }
    }

    private fun collectHeaders(ctx: RoutingContext): Map<String, List<String>> {
        val map = linkedMapOf<String, MutableList<String>>()
        for (name in ctx.request().headers().names()) {
            map[name.lowercase()] = ctx.request().headers().getAll(name).toMutableList()
        }
        return map
    }

    private fun parseTimeout(raw: String?): Long? {
        if (raw.isNullOrBlank()) return null
        return raw.toLongOrNull()?.takeIf { it > 0 }
    }
}

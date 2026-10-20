package io.github.lldata.connect.runtime

import com.google.protobuf.Message
import io.github.lldata.connect.protocol.ConnectEnvelope
import io.github.lldata.connect.protocol.ConnectErrorJson
import io.github.lldata.connect.protocol.ConnectProtocolConstants
import io.github.lldata.connect.protocol.ProtobufCodecSupport
import io.github.lldata.connect.runtime.grpc.GrpcStatusMapping
import io.github.lldata.connect.runtime.grpc.GrpcWebFraming
import io.github.lldata.connect.spi.ConnectCodec
import io.github.lldata.connect.spi.ConnectCode
import io.github.lldata.connect.spi.ConnectException
import io.github.lldata.connect.spi.ConnectServerStreamMethod
import io.github.lldata.connect.spi.ConnectStreamObserver
import io.github.lldata.connect.spi.ConnectUnaryMethod
import io.vertx.core.Handler
import io.vertx.core.http.HttpHeaders
import io.vertx.core.http.HttpMethod
import io.vertx.ext.web.RoutingContext
import org.jboss.logging.Logger
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Vert.x route handler for Connect unary, Connect server streaming, and gRPC-Web unary
 * on the Quarkus HTTP port.
 *
 * Native `application/grpc` is left to quarkus-grpc (`use-separate-server=false`).
 */
class ConnectUnaryHttpHandler(
    private val registry: ConnectMethodRegistry,
) : Handler<RoutingContext> {

    private val log = Logger.getLogger(ConnectUnaryHttpHandler::class.java)

    override fun handle(ctx: RoutingContext) {
        val request = ctx.request()
        if (request.method() != HttpMethod.POST && request.method() != HttpMethod.GET) {
            writeConnectError(ctx, ConnectException(ConnectCode.UNIMPLEMENTED, "Only GET and POST are supported"))
            return
        }

        val contentType = request.getHeader(HttpHeaders.CONTENT_TYPE)
        val wire = detectWireProtocol(contentType, request.method())
        if (wire == WireProtocol.GRPC_NATIVE) {
            ctx.next()
            return
        }

        val procedure = ConnectMethodRegistry.normalizeProcedure(request.path())

        if (wire == WireProtocol.CONNECT_STREAM) {
            val stream = registry.findStream(procedure)
            if (stream == null) {
                // Unknown stream procedure — do not steal unrelated routes.
                if (registry.find(procedure) != null) {
                    writeConnectError(
                        ctx,
                        ConnectException(
                            ConnectCode.UNIMPLEMENTED,
                            "Procedure /$procedure is unary; use application/proto or application/json",
                        ),
                    )
                } else {
                    ctx.next()
                }
                return
            }
            if (request.method() != HttpMethod.POST) {
                writeConnectError(ctx, ConnectException.invalidArgument("Connect streaming requires POST"))
                return
            }
            handleConnectStream(ctx, procedure, stream, contentType)
            return
        }

        val method = registry.find(procedure)
        if (method == null) {
            ctx.next()
            return
        }

        if (request.method() == HttpMethod.GET) {
            if (wire != WireProtocol.CONNECT) {
                writeConnectError(ctx, ConnectException.invalidArgument("GET is only supported for Connect unary"))
                return
            }
            handleConnectGet(ctx, procedure, method)
            return
        }

        when (wire) {
            WireProtocol.CONNECT -> handleConnectPost(ctx, procedure, method, contentType)
            WireProtocol.CONNECT_STREAM -> Unit // handled above
            WireProtocol.GRPC_WEB -> handleGrpcWebPost(ctx, procedure, method, contentType)
            WireProtocol.GRPC_NATIVE -> ctx.next()
        }
    }

    private fun handleConnectStream(
        ctx: RoutingContext,
        procedure: String,
        method: ConnectServerStreamMethod<out Message, out Message>,
        contentType: String?,
    ) {
        val version = ctx.request().getHeader(ConnectProtocolConstants.PROTOCOL_VERSION_HEADER)
        if (version != null && version != ConnectProtocolConstants.PROTOCOL_VERSION) {
            writeConnectStreamError(
                ctx,
                ConnectException.invalidArgument("Unsupported connect-protocol-version: $version"),
                contentType,
            )
            return
        }

        val codec = ConnectCodec.fromWireName(ConnectEnvelope.codecWireName(contentType))
            ?: ConnectCodec.PROTO
        val timeoutMs = parseTimeout(ctx.request().getHeader(ConnectProtocolConstants.TIMEOUT_HEADER))
        val headers = collectHeaders(ctx)
        val body = ctx.body().buffer()?.bytes ?: ByteArray(0)

        val requestBytes = try {
            ConnectEnvelope.decodeFirstDataMessage(body)
        } catch (e: ConnectException) {
            writeConnectStreamError(ctx, e, contentType)
            return
        }

        val callContext = DefaultConnectCallContext(procedure, codec, headers, timeoutMs)
        val responseCt = ConnectEnvelope.normalizeContentType(contentType)
        val response = ctx.response()
            .setStatusCode(200)
            .putHeader(HttpHeaders.CONTENT_TYPE, responseCt)
            .setChunked(true)

        val finished = AtomicBoolean(false)

        fun writeEnvelope(bytes: ByteArray) {
            ctx.vertx().runOnContext {
                if (!response.ended()) {
                    response.write(io.vertx.core.buffer.Buffer.buffer(bytes))
                }
            }
        }

        fun endWith(error: ConnectException?) {
            if (!finished.compareAndSet(false, true)) return
            val end = ConnectEnvelope.encodeEndStream(error, callContext.snapshotTrailers())
            ctx.vertx().runOnContext {
                if (!response.ended()) {
                    response.end(io.vertx.core.buffer.Buffer.buffer(end))
                }
            }
        }

        ctx.vertx().executeBlocking(
            java.util.concurrent.Callable {
                @Suppress("UNCHECKED_CAST")
                val typed = method as ConnectServerStreamMethod<Message, Message>
                val requestMessage = ProtobufCodecSupport.parse(requestBytes, typed.requestType(), codec)
                val observer = object : ConnectStreamObserver<Message> {
                    override fun onNext(value: Message) {
                        val payload = ProtobufCodecSupport.encode(value, codec)
                        writeEnvelope(ConnectEnvelope.encodeData(payload))
                    }

                    override fun onError(error: ConnectException) {
                        endWith(error)
                    }

                    override fun onCompleted() {
                        endWith(null)
                    }
                }
                typed.handle(requestMessage, callContext, observer)
                null
            },
            false,
        ).onComplete { ar ->
            if (ar.failed() && finished.compareAndSet(false, true)) {
                val error = GrpcStatusMapping.toConnectException(ar.cause())
                val end = ConnectEnvelope.encodeEndStream(error, callContext.snapshotTrailers())
                if (!response.ended()) {
                    response.end(io.vertx.core.buffer.Buffer.buffer(end))
                }
            }
        }
    }

    private fun writeConnectStreamError(
        ctx: RoutingContext,
        error: ConnectException,
        contentType: String?,
    ) {
        // Streaming errors use HTTP 200 + end-stream envelope (protocol requirement).
        val response = ctx.response()
            .setStatusCode(200)
            .putHeader(HttpHeaders.CONTENT_TYPE, ConnectEnvelope.normalizeContentType(contentType))
        response.end(io.vertx.core.buffer.Buffer.buffer(ConnectEnvelope.encodeEndStream(error)))
    }

    private fun handleConnectPost(
        ctx: RoutingContext,
        procedure: String,
        method: ConnectUnaryMethod<out Message, out Message>,
        contentType: String?,
    ) {
        val version = ctx.request().getHeader(ConnectProtocolConstants.PROTOCOL_VERSION_HEADER)
        if (version != null && version != ConnectProtocolConstants.PROTOCOL_VERSION) {
            writeConnectError(ctx, ConnectException.invalidArgument("Unsupported connect-protocol-version: $version"))
            return
        }

        val codec = ConnectCodec.fromContentType(contentType)
        if (codec == null) {
            ctx.response()
                .setStatusCode(415)
                .putHeader(HttpHeaders.CONTENT_TYPE, ConnectProtocolConstants.CONTENT_TYPE_JSON)
                .end(
                    ConnectErrorJson.toJson(
                        ConnectException.invalidArgument("Unsupported Content-Type; use application/proto or application/json"),
                    ),
                )
            return
        }

        val encoding = ctx.request().getHeader(HttpHeaders.CONTENT_ENCODING)
        if (encoding != null && !encoding.equals("identity", ignoreCase = true)) {
            writeConnectError(
                ctx,
                ConnectException(ConnectCode.UNIMPLEMENTED, "Unsupported Content-Encoding '$encoding'; supported: identity"),
            )
            return
        }

        val timeoutMs = parseTimeout(ctx.request().getHeader(ConnectProtocolConstants.TIMEOUT_HEADER))
        val headers = collectHeaders(ctx)
        val body = ctx.body().buffer()?.bytes ?: ByteArray(0)
        invokeAsync(ctx, procedure, method, codec, headers, timeoutMs, body, WireProtocol.CONNECT)
    }

    private fun handleConnectGet(
        ctx: RoutingContext,
        procedure: String,
        method: ConnectUnaryMethod<out Message, out Message>,
    ) {
        val params = ctx.queryParams()
        val connectVersion = params.get("connect")
        if (connectVersion != null && connectVersion != ConnectProtocolConstants.PROTOCOL_VERSION) {
            writeConnectError(ctx, ConnectException.invalidArgument("Unsupported connect query version: $connectVersion"))
            return
        }

        val codec = ConnectCodec.fromWireName(params.get("encoding")) ?: ConnectCodec.JSON
        val compression = params.get("compression") ?: "identity"
        if (!compression.equals("identity", ignoreCase = true)) {
            writeConnectError(
                ctx,
                ConnectException(ConnectCode.UNIMPLEMENTED, "Unsupported compression '$compression'; supported: identity"),
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
            writeConnectError(ctx, ConnectException.invalidArgument("Invalid base64 message: ${e.message}"))
            return
        }

        val timeoutMs = parseTimeout(ctx.request().getHeader(ConnectProtocolConstants.TIMEOUT_HEADER))
        invokeAsync(ctx, procedure, method, codec, collectHeaders(ctx), timeoutMs, body, WireProtocol.CONNECT)
    }

    private fun handleGrpcWebPost(
        ctx: RoutingContext,
        procedure: String,
        method: ConnectUnaryMethod<out Message, out Message>,
        contentType: String?,
    ) {
        val codec = grpcWebCodec(contentType)
        val timeoutMs = parseTimeout(ctx.request().getHeader("grpc-timeout"))
            ?: parseTimeout(ctx.request().getHeader(ConnectProtocolConstants.TIMEOUT_HEADER))
        val body = try {
            GrpcWebFraming.decodeRequestMessage(ctx.body().buffer()?.bytes ?: ByteArray(0))
        } catch (e: ConnectException) {
            writeGrpcWebError(ctx, e, contentType)
            return
        }
        invokeAsync(ctx, procedure, method, codec, collectHeaders(ctx), timeoutMs, body, WireProtocol.GRPC_WEB, contentType)
    }

    private fun invokeAsync(
        ctx: RoutingContext,
        procedure: String,
        method: ConnectUnaryMethod<out Message, out Message>,
        codec: ConnectCodec,
        headers: Map<String, List<String>>,
        timeoutMs: Long?,
        body: ByteArray,
        wire: WireProtocol,
        grpcWebContentType: String? = null,
    ) {
        val callContext = DefaultConnectCallContext(procedure, codec, headers, timeoutMs)
        val future = ctx.vertx().executeBlocking(
            java.util.concurrent.Callable {
                @Suppress("UNCHECKED_CAST")
                val typed = method as ConnectUnaryMethod<Message, Message>
                val requestMessage = ProtobufCodecSupport.parse(body, typed.requestType(), codec)
                val responseMessage = typed.handle(requestMessage, callContext)
                ProtobufCodecSupport.encode(responseMessage, codec)
            },
            false,
        )
        future.onComplete { ar ->
            if (ar.succeeded()) {
                when (wire) {
                    WireProtocol.CONNECT -> writeConnectSuccess(ctx, codec, callContext, ar.result())
                    WireProtocol.GRPC_WEB -> writeGrpcWebSuccess(ctx, callContext, ar.result(), grpcWebContentType)
                    WireProtocol.CONNECT_STREAM, WireProtocol.GRPC_NATIVE -> Unit
                }
            } else {
                val error = GrpcStatusMapping.toConnectException(ar.cause())
                when (wire) {
                    WireProtocol.CONNECT -> writeConnectError(ctx, error, callContext)
                    WireProtocol.GRPC_WEB -> writeGrpcWebError(ctx, error, grpcWebContentType, callContext)
                    WireProtocol.CONNECT_STREAM, WireProtocol.GRPC_NATIVE -> Unit
                }
            }
        }
    }

    private fun writeConnectSuccess(
        ctx: RoutingContext,
        codec: ConnectCodec,
        callContext: DefaultConnectCallContext,
        responseBytes: ByteArray,
    ) {
        val response = ctx.response().setStatusCode(200)
        response.putHeader(HttpHeaders.CONTENT_TYPE, codec.contentType)
        writeMetadata(response, callContext)
        response.end(io.vertx.core.buffer.Buffer.buffer(responseBytes))
    }

    private fun writeConnectError(
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

    private fun writeGrpcWebSuccess(
        ctx: RoutingContext,
        callContext: DefaultConnectCallContext,
        protoBytes: ByteArray,
        contentType: String?,
    ) {
        val trailers = callContext.snapshotTrailers().mapValues { it.value.firstOrNull() ?: "" }
            .filterValues { it.isNotEmpty() }
        val body = GrpcWebFraming.encodeUnarySuccess(protoBytes, trailers)
        val response = ctx.response().setStatusCode(200)
        response.putHeader(HttpHeaders.CONTENT_TYPE, normalizeGrpcWebContentType(contentType))
        for ((name, values) in callContext.snapshotHeaders()) {
            for (value in values) {
                response.putHeader(name, value)
            }
        }
        response.end(io.vertx.core.buffer.Buffer.buffer(body))
    }

    private fun writeGrpcWebError(
        ctx: RoutingContext,
        error: ConnectException,
        contentType: String?,
        callContext: DefaultConnectCallContext? = null,
    ) {
        if (ctx.response().headWritten()) {
            log.warn("Cannot write gRPC-Web error; response already started: ${error.code.wireName}")
            return
        }
        val statusCode = GrpcStatusMapping.toGrpcStatus(error.code).value()
        val body = GrpcWebFraming.encodeUnaryError(statusCode, error.message)
        val response = ctx.response().setStatusCode(200)
        response.putHeader(HttpHeaders.CONTENT_TYPE, normalizeGrpcWebContentType(contentType))
        if (callContext != null) {
            for ((name, values) in callContext.snapshotHeaders()) {
                for (value in values) {
                    response.putHeader(name, value)
                }
            }
        }
        response.end(io.vertx.core.buffer.Buffer.buffer(body))
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
        // grpc-timeout uses units (e.g. 10S); connect-timeout-ms is milliseconds.
        val msMatch = raw.toLongOrNull()
        if (msMatch != null) return msMatch.takeIf { it > 0 }
        val unit = raw.lastOrNull() ?: return null
        val amount = raw.dropLast(1).toLongOrNull() ?: return null
        val millis = when (unit) {
            'n' -> amount / 1_000_000
            'u' -> amount / 1_000
            'm' -> amount
            'S' -> amount * 1_000
            'M' -> amount * 60_000
            'H' -> amount * 3_600_000
            else -> return null
        }
        return millis.takeIf { it > 0 }
    }

    companion object {
        fun detectWireProtocol(contentType: String?, method: HttpMethod): WireProtocol {
            if (method == HttpMethod.GET) return WireProtocol.CONNECT
            val media = contentType?.substringBefore(';')?.trim()?.lowercase().orEmpty()
            return when {
                ConnectEnvelope.isConnectStreamingContentType(media) -> WireProtocol.CONNECT_STREAM
                media.startsWith("application/grpc-web") -> WireProtocol.GRPC_WEB
                media == "application/grpc" || media.startsWith("application/grpc+") -> WireProtocol.GRPC_NATIVE
                else -> WireProtocol.CONNECT
            }
        }

        fun grpcWebCodec(contentType: String?): ConnectCodec {
            val media = contentType?.substringBefore(';')?.trim()?.lowercase().orEmpty()
            return when {
                media.endsWith("+json") || media.contains("grpc-web-text") -> ConnectCodec.JSON
                else -> ConnectCodec.PROTO
            }
        }

        fun normalizeGrpcWebContentType(contentType: String?): String {
            val media = contentType?.substringBefore(';')?.trim()?.lowercase()
            return when {
                media.isNullOrBlank() -> "application/grpc-web+proto"
                media == "application/grpc-web" -> "application/grpc-web+proto"
                else -> media
            }
        }
    }
}

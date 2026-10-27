package io.github.lldata.connect.runtime

import com.google.protobuf.Message
import io.github.lldata.connect.protocol.ConnectCompression
import io.github.lldata.connect.protocol.ConnectEnvelope
import io.github.lldata.connect.protocol.ConnectErrorJson
import io.github.lldata.connect.protocol.ConnectMessageLimits
import io.github.lldata.connect.protocol.ConnectProtocolConstants
import io.github.lldata.connect.protocol.ProtobufCodecSupport
import io.github.lldata.connect.runtime.grpc.GrpcStatusMapping
import io.github.lldata.connect.runtime.grpc.GrpcWebFraming
import io.github.lldata.connect.runtime.security.ConnectSecuritySupport
import io.github.lldata.connect.spi.ConnectCodec
import io.github.lldata.connect.spi.ConnectCode
import io.github.lldata.connect.spi.ConnectException
import io.github.lldata.connect.spi.ConnectBidiStreamMethod
import io.github.lldata.connect.spi.ConnectClientStreamMethod
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
 *
 * When the app enables Quarkus Security, [ConnectSecuritySupport] propagates
 * `SecurityIdentity` so `@RolesAllowed` / `@Authenticated` on `@GrpcService` apply
 * to Connect and gRPC-Web the same way as native gRPC.
 */
class ConnectUnaryHttpHandler(
    private val registry: ConnectMethodRegistry,
    private val codecs: ProtobufCodecSupport,
    private val maxInboundMessageSize: Long = ConnectMessageLimits.DEFAULT_MAX_INBOUND_MESSAGE_SIZE,
) : Handler<RoutingContext> {

    private val log = Logger.getLogger(ConnectUnaryHttpHandler::class.java)

    override fun handle(ctx: RoutingContext) {
        // Opt-in: no-op when Security is absent; required when HTTP auth established identity.
        ConnectSecuritySupport.propagateIdentity(ctx)

        val request = ctx.request()
        val contentType = request.getHeader(HttpHeaders.CONTENT_TYPE)
        val wire = detectWireProtocol(contentType, request.method())
        if (wire == WireProtocol.GRPC_NATIVE) {
            ctx.next()
            return
        }

        val procedure = ConnectMethodRegistry.normalizeProcedure(request.path())
        val unary = registry.find(procedure)
        val serverStream = registry.findStream(procedure)
        val clientStream = registry.findClientStream(procedure)
        val bidiStream = registry.findBidiStream(procedure)
        val isStreamingProc = serverStream != null || clientStream != null || bidiStream != null

        // connect-go: wrong verb → HTTP 405 + Allow (empty body; client maps to unknown).
        if (request.method() != HttpMethod.POST && request.method() != HttpMethod.GET) {
            if (unary == null && !isStreamingProc && registry.unsupportedType(procedure) == null) {
                ctx.next()
                return
            }
            writeMethodNotAllowed(ctx, if (isStreamingProc) "POST" else "GET, POST")
            return
        }

        if (wire == WireProtocol.CONNECT_STREAM) {
            if (request.method() != HttpMethod.POST) {
                writeMethodNotAllowed(ctx, "POST")
                return
            }
            if (serverStream != null) {
                handleConnectStream(ctx, procedure, serverStream, contentType)
                return
            }
            if (clientStream != null) {
                handleConnectClientStream(ctx, procedure, clientStream, contentType)
                return
            }
            if (bidiStream != null) {
                handleConnectBidiStream(ctx, procedure, bidiStream, contentType)
                return
            }
            when {
                unary != null -> writeConnectError(
                    ctx,
                    ConnectException(
                        ConnectCode.UNIMPLEMENTED,
                        "Procedure /$procedure is unary; use application/proto or application/json",
                    ),
                )
                registry.unsupportedType(procedure) != null -> writeConnectStreamError(
                    ctx,
                    unimplementedStreaming(procedure, registry.unsupportedType(procedure)!!),
                    contentType,
                )
                else -> ctx.next()
            }
            return
        }

        if (wire == WireProtocol.GRPC_WEB) {
            if (request.method() != HttpMethod.POST) {
                writeMethodNotAllowed(ctx, "POST")
                return
            }
            if (serverStream != null) {
                handleGrpcWebServerStream(ctx, procedure, serverStream, contentType)
                return
            }
            if (clientStream != null) {
                handleGrpcWebClientStream(ctx, procedure, clientStream, contentType)
                return
            }
            if (bidiStream != null) {
                handleGrpcWebBidiStream(ctx, procedure, bidiStream, contentType)
                return
            }
            if (unary != null) {
                handleGrpcWebPost(ctx, procedure, unary, contentType)
                return
            }
            val unsupported = registry.unsupportedType(procedure)
            if (unsupported != null) {
                writeGrpcWebError(ctx, unimplementedStreaming(procedure, unsupported), contentType)
                return
            }
            // Do not fall through to quarkus-grpc: on HTTP/2 its isGrpc gate matches
            // application/grpc-web* and answers with application/grpc (wrong wire).
            writeGrpcWebError(
                ctx,
                ConnectException.unimplemented("Procedure not found: /$procedure"),
                contentType,
            )
            return
        }

        if (unary == null) {
            // Unary Content-Type against a streaming procedure → clear error.
            if (isStreamingProc) {
                writeConnectError(
                    ctx,
                    ConnectException(
                        ConnectCode.UNIMPLEMENTED,
                        "Procedure /$procedure is streaming; use application/connect+json or application/connect+proto",
                    ),
                )
                return
            }
            val unsupported = registry.unsupportedType(procedure)
            if (unsupported != null) {
                writeConnectError(ctx, unimplementedStreaming(procedure, unsupported))
                return
            }
            ctx.next()
            return
        }

        if (request.method() == HttpMethod.GET) {
            if (wire != WireProtocol.CONNECT) {
                writeConnectError(ctx, ConnectException.invalidArgument("GET is only supported for Connect unary"))
                return
            }
            handleConnectGet(ctx, procedure, unary)
            return
        }

        when (wire) {
            WireProtocol.CONNECT -> handleConnectPost(ctx, procedure, unary, contentType)
            WireProtocol.CONNECT_STREAM, WireProtocol.GRPC_WEB -> Unit // handled above
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

        val requestEncoding = try {
            ConnectCompression.requireSupported(
                ctx.request().getHeader(ConnectProtocolConstants.STREAMING_CONTENT_ENCODING_HEADER),
                "connect-content-encoding",
            )
        } catch (e: ConnectException) {
            writeConnectStreamError(ctx, e, contentType)
            return
        }

        val responseEncoding = ConnectCompression.negotiateResponseEncoding(
            ctx.request().getHeader(ConnectProtocolConstants.STREAMING_ACCEPT_ENCODING_HEADER),
            requestEncoding,
        )

        val codec = ConnectCodec.fromWireName(ConnectEnvelope.codecWireName(contentType))
            ?: ConnectCodec.PROTO
        val timeoutMs = try {
            parseConnectTimeoutMs(ctx.request().getHeader(ConnectProtocolConstants.TIMEOUT_HEADER))
        } catch (e: ConnectException) {
            writeConnectStreamError(ctx, e, contentType, responseEncoding)
            return
        }
        val headers = collectHeaders(ctx)
        val body = ctx.body().buffer()?.bytes ?: ByteArray(0)

        val requestBytes = try {
            ConnectEnvelope.decodeFirstDataMessage(body, requestEncoding, maxInboundMessageSize)
        } catch (e: ConnectException) {
            writeConnectStreamError(ctx, e, contentType, responseEncoding)
            return
        }

        val callControl = StreamCallControl()
        val clientGone = AtomicBoolean(false)
        val callContext = DefaultConnectCallContext(procedure, codec, headers, timeoutMs, callControl)
        val responseCt = ConnectEnvelope.normalizeContentType(contentType)
        val response = ctx.response()
            .setStatusCode(200)
            .putHeader(HttpHeaders.CONTENT_TYPE, responseCt)
            .putHeader(ConnectProtocolConstants.STREAMING_ACCEPT_ENCODING_HEADER, ConnectCompression.ACCEPT_ADVERTISEMENT)
        if (responseEncoding != ConnectCompression.IDENTITY) {
            response.putHeader(ConnectProtocolConstants.STREAMING_CONTENT_ENCODING_HEADER, responseEncoding)
        }
        response.setChunked(true)

        val finished = AtomicBoolean(false)
        val headersFlushed = AtomicBoolean(false)
        fun markClientGone() {
            clientGone.set(true)
            callControl.cancelFromClient()
        }
        response.closeHandler { markClientGone() }
        response.exceptionHandler { markClientGone() }

        var timeoutTimerId = -1L

        fun flushLeadingHeaders() {
            if (!headersFlushed.compareAndSet(false, true)) return
            if (response.headWritten()) return
            writeStreamResponseHeaders(response, callContext)
        }

        fun writeEnvelope(bytes: ByteArray) {
            ctx.vertx().runOnContext {
                if (!response.ended() && !clientGone.get()) {
                    flushLeadingHeaders()
                    response.write(io.vertx.core.buffer.Buffer.buffer(bytes))
                }
            }
        }

        fun endWith(error: ConnectException?) {
            if (!finished.compareAndSet(false, true)) return
            if (timeoutTimerId >= 0) {
                ctx.vertx().cancelTimer(timeoutTimerId)
            }
            if (clientGone.get()) return
            // End-stream envelopes stay uncompressed (small JSON); compressed bit left unset.
            val end = ConnectEnvelope.encodeEndStream(error, callContext.snapshotTrailers())
            ctx.vertx().runOnContext {
                if (!response.ended()) {
                    flushLeadingHeaders()
                    response.end(io.vertx.core.buffer.Buffer.buffer(end))
                }
            }
        }

        if (timeoutMs != null) {
            timeoutTimerId = ctx.vertx().setTimer(timeoutMs) {
                callControl.cancelFromClient()
                endWith(
                    ConnectException(
                        ConnectCode.DEADLINE_EXCEEDED,
                        "RPC timed out after ${timeoutMs}ms",
                    ),
                )
            }
        }

        ctx.vertx().executeBlocking(
            java.util.concurrent.Callable {
                @Suppress("UNCHECKED_CAST")
                val typed = method as ConnectServerStreamMethod<Message, Message>
                val requestMessage = codecs.parse(requestBytes, typed.requestType(), codec)
                val observer = object : ConnectStreamObserver<Message> {
                    override fun onNext(value: Message) {
                        val payload = codecs.encode(value, codec)
                        writeEnvelope(ConnectEnvelope.encodeDataMaybeCompressed(payload, responseEncoding))
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
            if (ar.failed() && !clientGone.get()) {
                endWith(GrpcStatusMapping.toConnectException(ar.cause()))
            }
        }
    }

    private fun handleConnectClientStream(
        ctx: RoutingContext,
        procedure: String,
        method: ConnectClientStreamMethod<out Message, out Message>,
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

        val requestEncoding = try {
            ConnectCompression.requireSupported(
                ctx.request().getHeader(ConnectProtocolConstants.STREAMING_CONTENT_ENCODING_HEADER),
                "connect-content-encoding",
            )
        } catch (e: ConnectException) {
            writeConnectStreamError(ctx, e, contentType)
            return
        }

        val responseEncoding = ConnectCompression.negotiateResponseEncoding(
            ctx.request().getHeader(ConnectProtocolConstants.STREAMING_ACCEPT_ENCODING_HEADER),
            requestEncoding,
        )

        val codec = ConnectCodec.fromWireName(ConnectEnvelope.codecWireName(contentType))
            ?: ConnectCodec.PROTO
        val timeoutMs = try {
            parseConnectTimeoutMs(ctx.request().getHeader(ConnectProtocolConstants.TIMEOUT_HEADER))
        } catch (e: ConnectException) {
            writeConnectStreamError(ctx, e, contentType, responseEncoding)
            return
        }
        val headers = collectHeaders(ctx)
        val body = ctx.body().buffer()?.bytes ?: ByteArray(0)

        val requestPayloads = try {
            ConnectEnvelope.decodeAllDataMessages(body, requestEncoding, maxInboundMessageSize)
        } catch (e: ConnectException) {
            writeConnectStreamError(ctx, e, contentType, responseEncoding)
            return
        }

        val callContext = DefaultConnectCallContext(procedure, codec, headers, timeoutMs)
        val responseCt = ConnectEnvelope.normalizeContentType(contentType)

        ctx.vertx().executeBlocking(
            java.util.concurrent.Callable {
                @Suppress("UNCHECKED_CAST")
                val typed = method as ConnectClientStreamMethod<Message, Message>
                val requests = requestPayloads.map { codecs.parse(it, typed.requestType(), codec) }
                val responseMessage = typed.handle(requests, callContext)
                codecs.encode(responseMessage, codec)
            },
            false,
        ).onComplete { ar ->
            val response = ctx.response()
                .setStatusCode(200)
                .putHeader(HttpHeaders.CONTENT_TYPE, responseCt)
                .putHeader(ConnectProtocolConstants.STREAMING_ACCEPT_ENCODING_HEADER, ConnectCompression.ACCEPT_ADVERTISEMENT)
            if (responseEncoding != ConnectCompression.IDENTITY) {
                response.putHeader(ConnectProtocolConstants.STREAMING_CONTENT_ENCODING_HEADER, responseEncoding)
            }
            writeStreamResponseHeaders(response, callContext)
            if (ar.succeeded()) {
                val data = ConnectEnvelope.encodeDataMaybeCompressed(ar.result(), responseEncoding)
                val end = ConnectEnvelope.encodeEndStream(null, callContext.snapshotTrailers())
                response.end(io.vertx.core.buffer.Buffer.buffer(data + end))
            } else {
                val error = GrpcStatusMapping.toConnectException(ar.cause())
                response.end(
                    io.vertx.core.buffer.Buffer.buffer(
                        ConnectEnvelope.encodeEndStream(error, callContext.snapshotTrailers()),
                    ),
                )
            }
        }
    }

    private fun handleConnectBidiStream(
        ctx: RoutingContext,
        procedure: String,
        method: ConnectBidiStreamMethod<out Message, out Message>,
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

        val requestEncoding = try {
            ConnectCompression.requireSupported(
                ctx.request().getHeader(ConnectProtocolConstants.STREAMING_CONTENT_ENCODING_HEADER),
                "connect-content-encoding",
            )
        } catch (e: ConnectException) {
            writeConnectStreamError(ctx, e, contentType)
            return
        }

        val responseEncoding = ConnectCompression.negotiateResponseEncoding(
            ctx.request().getHeader(ConnectProtocolConstants.STREAMING_ACCEPT_ENCODING_HEADER),
            requestEncoding,
        )

        val codec = ConnectCodec.fromWireName(ConnectEnvelope.codecWireName(contentType))
            ?: ConnectCodec.PROTO
        val timeoutMs = try {
            parseConnectTimeoutMs(ctx.request().getHeader(ConnectProtocolConstants.TIMEOUT_HEADER))
        } catch (e: ConnectException) {
            writeConnectStreamError(ctx, e, contentType, responseEncoding)
            return
        }
        val headers = collectHeaders(ctx)
        val body = ctx.body().buffer()?.bytes ?: ByteArray(0)

        val requestPayloads = try {
            ConnectEnvelope.decodeAllDataMessages(body, requestEncoding, maxInboundMessageSize)
        } catch (e: ConnectException) {
            writeConnectStreamError(ctx, e, contentType, responseEncoding)
            return
        }

        val callControl = StreamCallControl()
        val clientGone = AtomicBoolean(false)
        val callContext = DefaultConnectCallContext(procedure, codec, headers, timeoutMs, callControl)
        val responseCt = ConnectEnvelope.normalizeContentType(contentType)
        val response = ctx.response()
            .setStatusCode(200)
            .putHeader(HttpHeaders.CONTENT_TYPE, responseCt)
            .putHeader(ConnectProtocolConstants.STREAMING_ACCEPT_ENCODING_HEADER, ConnectCompression.ACCEPT_ADVERTISEMENT)
        if (responseEncoding != ConnectCompression.IDENTITY) {
            response.putHeader(ConnectProtocolConstants.STREAMING_CONTENT_ENCODING_HEADER, responseEncoding)
        }
        response.setChunked(true)

        val finished = AtomicBoolean(false)
        val headersFlushed = AtomicBoolean(false)
        fun markClientGone() {
            clientGone.set(true)
            callControl.cancelFromClient()
        }
        response.closeHandler { markClientGone() }
        response.exceptionHandler { markClientGone() }

        var timeoutTimerId = -1L

        fun flushLeadingHeaders() {
            if (!headersFlushed.compareAndSet(false, true)) return
            if (response.headWritten()) return
            writeStreamResponseHeaders(response, callContext)
        }

        fun writeEnvelope(bytes: ByteArray) {
            ctx.vertx().runOnContext {
                if (!response.ended() && !clientGone.get()) {
                    flushLeadingHeaders()
                    response.write(io.vertx.core.buffer.Buffer.buffer(bytes))
                }
            }
        }

        fun endWith(error: ConnectException?) {
            if (!finished.compareAndSet(false, true)) return
            if (timeoutTimerId >= 0) {
                ctx.vertx().cancelTimer(timeoutTimerId)
            }
            if (clientGone.get()) return
            val end = ConnectEnvelope.encodeEndStream(error, callContext.snapshotTrailers())
            ctx.vertx().runOnContext {
                if (!response.ended()) {
                    flushLeadingHeaders()
                    response.end(io.vertx.core.buffer.Buffer.buffer(end))
                }
            }
        }

        if (timeoutMs != null) {
            timeoutTimerId = ctx.vertx().setTimer(timeoutMs) {
                callControl.cancelFromClient()
                endWith(
                    ConnectException(
                        ConnectCode.DEADLINE_EXCEEDED,
                        "RPC timed out after ${timeoutMs}ms",
                    ),
                )
            }
        }

        ctx.vertx().executeBlocking(
            java.util.concurrent.Callable {
                @Suppress("UNCHECKED_CAST")
                val typed = method as ConnectBidiStreamMethod<Message, Message>
                val requests = requestPayloads.map { codecs.parse(it, typed.requestType(), codec) }
                val observer = object : ConnectStreamObserver<Message> {
                    override fun onNext(value: Message) {
                        val payload = codecs.encode(value, codec)
                        writeEnvelope(ConnectEnvelope.encodeDataMaybeCompressed(payload, responseEncoding))
                    }

                    override fun onError(error: ConnectException) {
                        endWith(error)
                    }

                    override fun onCompleted() {
                        endWith(null)
                    }
                }
                typed.handle(requests, callContext, observer)
                null
            },
            false,
        ).onComplete { ar ->
            if (ar.failed() && !clientGone.get()) {
                endWith(GrpcStatusMapping.toConnectException(ar.cause()))
            }
        }
    }

    private fun handleGrpcWebServerStream(
        ctx: RoutingContext,
        procedure: String,
        method: ConnectServerStreamMethod<out Message, out Message>,
        contentType: String?,
    ) {
        val codec = grpcWebCodec(contentType)
        val requestEncoding = try {
            ConnectCompression.requireSupported(
                ctx.request().getHeader("grpc-encoding"),
                "grpc-encoding",
            )
        } catch (e: ConnectException) {
            writeGrpcWebError(ctx, e, contentType)
            return
        }
        val responseEncoding = ConnectCompression.negotiateResponseEncoding(
            ctx.request().getHeader("grpc-accept-encoding"),
            requestEncoding,
        )
        val timeoutMs = parseTimeout(ctx.request().getHeader("grpc-timeout"))
            ?: parseTimeout(ctx.request().getHeader(ConnectProtocolConstants.TIMEOUT_HEADER))
        val headers = collectHeaders(ctx)
        val body = ctx.body().buffer()?.bytes ?: ByteArray(0)
        val requestBytes = try {
            GrpcWebFraming.decodeExactlyOneDataMessage(body, requestEncoding, maxInboundMessageSize)
        } catch (e: ConnectException) {
            writeGrpcWebError(ctx, e, contentType)
            return
        }

        val callControl = StreamCallControl()
        val clientGone = AtomicBoolean(false)
        val callContext = DefaultConnectCallContext(procedure, codec, headers, timeoutMs, callControl)
        val response = ctx.response()
            .setStatusCode(200)
            .putHeader(HttpHeaders.CONTENT_TYPE, normalizeGrpcWebContentType(contentType))
            .putHeader("grpc-accept-encoding", GrpcWebFraming.ACCEPT_ENCODING_ADVERTISEMENT)
        if (responseEncoding != ConnectCompression.IDENTITY) {
            response.putHeader("grpc-encoding", responseEncoding)
        }
        response.setChunked(true)

        val finished = AtomicBoolean(false)
        val headersFlushed = AtomicBoolean(false)
        fun markClientGone() {
            clientGone.set(true)
            callControl.cancelFromClient()
        }
        response.closeHandler { markClientGone() }
        response.exceptionHandler { markClientGone() }

        var timeoutTimerId = -1L

        fun flushLeadingHeaders() {
            if (!headersFlushed.compareAndSet(false, true)) return
            if (response.headWritten()) return
            writeStreamResponseHeaders(response, callContext)
        }

        fun writeFrame(bytes: ByteArray) {
            ctx.vertx().runOnContext {
                if (!response.ended() && !clientGone.get()) {
                    flushLeadingHeaders()
                    response.write(io.vertx.core.buffer.Buffer.buffer(bytes))
                }
            }
        }

        fun endWith(error: ConnectException?) {
            if (!finished.compareAndSet(false, true)) return
            if (timeoutTimerId >= 0) {
                ctx.vertx().cancelTimer(timeoutTimerId)
            }
            if (clientGone.get()) return
            val status = if (error == null) 0 else GrpcStatusMapping.toGrpcStatus(error.code).value()
            val trailer = GrpcWebFraming.encodeTrailerFrame(
                status,
                error?.message,
                callContext.snapshotTrailers(),
            )
            ctx.vertx().runOnContext {
                if (!response.ended()) {
                    flushLeadingHeaders()
                    response.end(io.vertx.core.buffer.Buffer.buffer(trailer))
                }
            }
        }

        if (timeoutMs != null) {
            timeoutTimerId = ctx.vertx().setTimer(timeoutMs) {
                callControl.cancelFromClient()
                endWith(
                    ConnectException(
                        ConnectCode.DEADLINE_EXCEEDED,
                        "RPC timed out after ${timeoutMs}ms",
                    ),
                )
            }
        }

        ctx.vertx().executeBlocking(
            java.util.concurrent.Callable {
                @Suppress("UNCHECKED_CAST")
                val typed = method as ConnectServerStreamMethod<Message, Message>
                val requestMessage = codecs.parse(requestBytes, typed.requestType(), codec)
                val observer = object : ConnectStreamObserver<Message> {
                    override fun onNext(value: Message) {
                        val payload = codecs.encode(value, codec)
                        writeFrame(GrpcWebFraming.encodeDataMaybeCompressed(payload, responseEncoding))
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
            if (ar.failed() && !clientGone.get()) {
                endWith(GrpcStatusMapping.toConnectException(ar.cause()))
            }
        }
    }

    private fun handleGrpcWebClientStream(
        ctx: RoutingContext,
        procedure: String,
        method: ConnectClientStreamMethod<out Message, out Message>,
        contentType: String?,
    ) {
        val codec = grpcWebCodec(contentType)
        val requestEncoding = try {
            ConnectCompression.requireSupported(
                ctx.request().getHeader("grpc-encoding"),
                "grpc-encoding",
            )
        } catch (e: ConnectException) {
            writeGrpcWebError(ctx, e, contentType)
            return
        }
        val responseEncoding = ConnectCompression.negotiateResponseEncoding(
            ctx.request().getHeader("grpc-accept-encoding"),
            requestEncoding,
        )
        val timeoutMs = parseTimeout(ctx.request().getHeader("grpc-timeout"))
            ?: parseTimeout(ctx.request().getHeader(ConnectProtocolConstants.TIMEOUT_HEADER))
        val headers = collectHeaders(ctx)
        val body = ctx.body().buffer()?.bytes ?: ByteArray(0)
        val requestPayloads = try {
            GrpcWebFraming.decodeAllDataMessages(body, requestEncoding, maxInboundMessageSize)
        } catch (e: ConnectException) {
            writeGrpcWebError(ctx, e, contentType)
            return
        }

        val callContext = DefaultConnectCallContext(procedure, codec, headers, timeoutMs)

        ctx.vertx().executeBlocking(
            java.util.concurrent.Callable {
                @Suppress("UNCHECKED_CAST")
                val typed = method as ConnectClientStreamMethod<Message, Message>
                val requests = requestPayloads.map { codecs.parse(it, typed.requestType(), codec) }
                val responseMessage = typed.handle(requests, callContext)
                codecs.encode(responseMessage, codec)
            },
            false,
        ).onComplete { ar ->
            val response = ctx.response()
                .setStatusCode(200)
                .putHeader(HttpHeaders.CONTENT_TYPE, normalizeGrpcWebContentType(contentType))
                .putHeader("grpc-accept-encoding", GrpcWebFraming.ACCEPT_ENCODING_ADVERTISEMENT)
            if (responseEncoding != ConnectCompression.IDENTITY) {
                response.putHeader("grpc-encoding", responseEncoding)
            }
            writeStreamResponseHeaders(response, callContext)
            if (ar.succeeded()) {
                val data = GrpcWebFraming.encodeDataMaybeCompressed(ar.result(), responseEncoding)
                val trailer = GrpcWebFraming.encodeTrailerFrame(0, null, callContext.snapshotTrailers())
                response.end(io.vertx.core.buffer.Buffer.buffer(data + trailer))
            } else {
                val error = GrpcStatusMapping.toConnectException(ar.cause())
                val status = GrpcStatusMapping.toGrpcStatus(error.code).value()
                response.end(
                    io.vertx.core.buffer.Buffer.buffer(
                        GrpcWebFraming.encodeTrailerFrame(status, error.message, callContext.snapshotTrailers()),
                    ),
                )
            }
        }
    }

    private fun handleGrpcWebBidiStream(
        ctx: RoutingContext,
        procedure: String,
        method: ConnectBidiStreamMethod<out Message, out Message>,
        contentType: String?,
    ) {
        val codec = grpcWebCodec(contentType)
        val requestEncoding = try {
            ConnectCompression.requireSupported(
                ctx.request().getHeader("grpc-encoding"),
                "grpc-encoding",
            )
        } catch (e: ConnectException) {
            writeGrpcWebError(ctx, e, contentType)
            return
        }
        val responseEncoding = ConnectCompression.negotiateResponseEncoding(
            ctx.request().getHeader("grpc-accept-encoding"),
            requestEncoding,
        )
        val timeoutMs = parseTimeout(ctx.request().getHeader("grpc-timeout"))
            ?: parseTimeout(ctx.request().getHeader(ConnectProtocolConstants.TIMEOUT_HEADER))
        val headers = collectHeaders(ctx)
        val body = ctx.body().buffer()?.bytes ?: ByteArray(0)
        val requestPayloads = try {
            GrpcWebFraming.decodeAllDataMessages(body, requestEncoding, maxInboundMessageSize)
        } catch (e: ConnectException) {
            writeGrpcWebError(ctx, e, contentType)
            return
        }

        val callControl = StreamCallControl()
        val clientGone = AtomicBoolean(false)
        val callContext = DefaultConnectCallContext(procedure, codec, headers, timeoutMs, callControl)
        val response = ctx.response()
            .setStatusCode(200)
            .putHeader(HttpHeaders.CONTENT_TYPE, normalizeGrpcWebContentType(contentType))
            .putHeader("grpc-accept-encoding", GrpcWebFraming.ACCEPT_ENCODING_ADVERTISEMENT)
        if (responseEncoding != ConnectCompression.IDENTITY) {
            response.putHeader("grpc-encoding", responseEncoding)
        }
        response.setChunked(true)

        val finished = AtomicBoolean(false)
        val headersFlushed = AtomicBoolean(false)
        fun markClientGone() {
            clientGone.set(true)
            callControl.cancelFromClient()
        }
        response.closeHandler { markClientGone() }
        response.exceptionHandler { markClientGone() }

        var timeoutTimerId = -1L

        fun flushLeadingHeaders() {
            if (!headersFlushed.compareAndSet(false, true)) return
            if (response.headWritten()) return
            writeStreamResponseHeaders(response, callContext)
        }

        fun writeFrame(bytes: ByteArray) {
            ctx.vertx().runOnContext {
                if (!response.ended() && !clientGone.get()) {
                    flushLeadingHeaders()
                    response.write(io.vertx.core.buffer.Buffer.buffer(bytes))
                }
            }
        }

        fun endWith(error: ConnectException?) {
            if (!finished.compareAndSet(false, true)) return
            if (timeoutTimerId >= 0) {
                ctx.vertx().cancelTimer(timeoutTimerId)
            }
            if (clientGone.get()) return
            val status = if (error == null) 0 else GrpcStatusMapping.toGrpcStatus(error.code).value()
            val trailer = GrpcWebFraming.encodeTrailerFrame(
                status,
                error?.message,
                callContext.snapshotTrailers(),
            )
            ctx.vertx().runOnContext {
                if (!response.ended()) {
                    flushLeadingHeaders()
                    response.end(io.vertx.core.buffer.Buffer.buffer(trailer))
                }
            }
        }

        if (timeoutMs != null) {
            timeoutTimerId = ctx.vertx().setTimer(timeoutMs) {
                callControl.cancelFromClient()
                endWith(
                    ConnectException(
                        ConnectCode.DEADLINE_EXCEEDED,
                        "RPC timed out after ${timeoutMs}ms",
                    ),
                )
            }
        }

        ctx.vertx().executeBlocking(
            java.util.concurrent.Callable {
                @Suppress("UNCHECKED_CAST")
                val typed = method as ConnectBidiStreamMethod<Message, Message>
                val requests = requestPayloads.map { codecs.parse(it, typed.requestType(), codec) }
                val observer = object : ConnectStreamObserver<Message> {
                    override fun onNext(value: Message) {
                        val payload = codecs.encode(value, codec)
                        writeFrame(GrpcWebFraming.encodeDataMaybeCompressed(payload, responseEncoding))
                    }

                    override fun onError(error: ConnectException) {
                        endWith(error)
                    }

                    override fun onCompleted() {
                        endWith(null)
                    }
                }
                typed.handle(requests, callContext, observer)
                null
            },
            false,
        ).onComplete { ar ->
            if (ar.failed() && !clientGone.get()) {
                endWith(GrpcStatusMapping.toConnectException(ar.cause()))
            }
        }
    }

    private fun writeConnectStreamError(
        ctx: RoutingContext,
        error: ConnectException,
        contentType: String?,
        responseEncoding: String = ConnectCompression.IDENTITY,
    ) {
        // Streaming errors use HTTP 200 + end-stream envelope (protocol requirement).
        val response = ctx.response()
            .setStatusCode(200)
            .putHeader(HttpHeaders.CONTENT_TYPE, ConnectEnvelope.normalizeContentType(contentType))
            .putHeader(ConnectProtocolConstants.STREAMING_ACCEPT_ENCODING_HEADER, ConnectCompression.ACCEPT_ADVERTISEMENT)
        if (responseEncoding != ConnectCompression.IDENTITY) {
            response.putHeader(ConnectProtocolConstants.STREAMING_CONTENT_ENCODING_HEADER, responseEncoding)
        }
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
            // connect-go: unsupported Content-Type → HTTP 415 + Accept-Post, empty body
            // (client maps 415 → unknown).
            writeUnsupportedMediaType(ctx, ACCEPT_POST_UNARY)
            return
        }

        val requestEncoding = try {
            ConnectCompression.requireSupported(
                ctx.request().getHeader(HttpHeaders.CONTENT_ENCODING),
                "Content-Encoding",
            )
        } catch (e: ConnectException) {
            writeConnectError(ctx, e)
            return
        }

        val responseEncoding = ConnectCompression.negotiateResponseEncoding(
            ctx.request().getHeader(HttpHeaders.ACCEPT_ENCODING),
            requestEncoding,
        )

        val timeoutMs = try {
            parseConnectTimeoutMs(ctx.request().getHeader(ConnectProtocolConstants.TIMEOUT_HEADER))
        } catch (e: ConnectException) {
            writeConnectError(ctx, e)
            return
        }
        val headers = collectHeaders(ctx)
        val rawBody = ctx.body().buffer()?.bytes ?: ByteArray(0)
        val body = try {
            val decompressed = ConnectCompression.decompress(requestEncoding, rawBody)
            ConnectMessageLimits.requireWithinLimit(decompressed.size, maxInboundMessageSize)
            decompressed
        } catch (e: ConnectException) {
            writeConnectError(ctx, e)
            return
        }
        invokeAsync(
            ctx,
            procedure,
            method,
            codec,
            headers,
            timeoutMs,
            body,
            WireProtocol.CONNECT,
            responseEncoding = responseEncoding,
        )
    }

    private fun handleConnectGet(
        ctx: RoutingContext,
        procedure: String,
        method: ConnectUnaryMethod<out Message, out Message>,
    ) {
        val params = ctx.queryParams()
        val connectVersion = params.get("connect")
        if (!ConnectProtocolConstants.isSupportedGetConnectVersion(connectVersion)) {
            writeConnectError(
                ctx,
                ConnectException.invalidArgument(
                    "Unsupported connect query version: $connectVersion (expected ${ConnectProtocolConstants.PROTOCOL_VERSION_GET})",
                ),
                varyAcceptEncoding = true,
            )
            return
        }

        val encodingParam = params.get("encoding")
        val codec = when {
            encodingParam.isNullOrBlank() -> ConnectCodec.JSON
            else -> ConnectCodec.fromWireName(encodingParam)
        }
        if (codec == null) {
            // Spec: unsupported Message-Codec → HTTP 415.
            val response = ctx.response()
                .setStatusCode(415)
                .putHeader(HttpHeaders.CONTENT_TYPE, ConnectProtocolConstants.CONTENT_TYPE_JSON)
                .putHeader(HttpHeaders.ACCEPT_ENCODING, ConnectCompression.ACCEPT_ADVERTISEMENT)
            response.headers().add(HttpHeaders.VARY, "Accept-Encoding")
            response.end(
                ConnectErrorJson.toJson(
                    ConnectException.invalidArgument(
                        "Unsupported encoding '$encodingParam'; use json or proto",
                    ),
                ),
            )
            return
        }

        val requestEncoding = try {
            ConnectCompression.requireSupported(params.get("compression"), "compression")
        } catch (e: ConnectException) {
            writeConnectError(ctx, e, varyAcceptEncoding = true)
            return
        }

        val responseEncoding = ConnectCompression.negotiateResponseEncoding(
            ctx.request().getHeader(HttpHeaders.ACCEPT_ENCODING),
            requestEncoding,
        )

        val messageParam = params.get("message") ?: ""
        // Spec: Base64-Query → "base64=1" (URL-safe base64, typically unpadded for proto).
        val useBase64 = params.get("base64") == "1" || params.get("base64").equals("true", ignoreCase = true)
        val rawMessage = try {
            if (useBase64) {
                val normalized = messageParam.replace(' ', '+')
                java.util.Base64.getUrlDecoder().decode(normalized)
            } else {
                messageParam.toByteArray(StandardCharsets.UTF_8)
            }
        } catch (e: IllegalArgumentException) {
            writeConnectError(
                ctx,
                ConnectException.invalidArgument("Invalid base64 message: ${e.message}"),
                varyAcceptEncoding = true,
            )
            return
        }

        val body = try {
            val decompressed = ConnectCompression.decompress(requestEncoding, rawMessage)
            ConnectMessageLimits.requireWithinLimit(decompressed.size, maxInboundMessageSize)
            decompressed
        } catch (e: ConnectException) {
            writeConnectError(ctx, e, varyAcceptEncoding = true)
            return
        }

        val timeoutMs = try {
            parseConnectTimeoutMs(ctx.request().getHeader(ConnectProtocolConstants.TIMEOUT_HEADER))
        } catch (e: ConnectException) {
            writeConnectError(ctx, e, varyAcceptEncoding = true)
            return
        }
        invokeAsync(
            ctx,
            procedure,
            method,
            codec,
            collectHeaders(ctx),
            timeoutMs,
            body,
            WireProtocol.CONNECT,
            varyAcceptEncoding = true,
            responseEncoding = responseEncoding,
        )
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
        val requestEncoding = try {
            ConnectCompression.requireSupported(
                ctx.request().getHeader("grpc-encoding"),
                "grpc-encoding",
            )
        } catch (e: ConnectException) {
            writeGrpcWebError(ctx, e, contentType)
            return
        }
        val responseEncoding = ConnectCompression.negotiateResponseEncoding(
            ctx.request().getHeader("grpc-accept-encoding"),
            requestEncoding,
        )
        val body = try {
            GrpcWebFraming.decodeRequestMessage(
                ctx.body().buffer()?.bytes ?: ByteArray(0),
                requestEncoding,
                maxInboundMessageSize,
            )
        } catch (e: ConnectException) {
            writeGrpcWebError(ctx, e, contentType)
            return
        }
        invokeAsync(
            ctx,
            procedure,
            method,
            codec,
            collectHeaders(ctx),
            timeoutMs,
            body,
            WireProtocol.GRPC_WEB,
            contentType,
            responseEncoding = responseEncoding,
        )
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
        varyAcceptEncoding: Boolean = false,
        responseEncoding: String = ConnectCompression.IDENTITY,
    ) {
        val callContext = DefaultConnectCallContext(procedure, codec, headers, timeoutMs)
        val future = ctx.vertx().executeBlocking(
            java.util.concurrent.Callable {
                @Suppress("UNCHECKED_CAST")
                val typed = method as ConnectUnaryMethod<Message, Message>
                val requestMessage = codecs.parse(body, typed.requestType(), codec)
                val responseMessage = typed.handle(requestMessage, callContext)
                codecs.encode(responseMessage, codec)
            },
            false,
        )
        future.onComplete { ar ->
            if (ar.succeeded()) {
                when (wire) {
                    WireProtocol.CONNECT -> writeConnectSuccess(
                        ctx,
                        codec,
                        callContext,
                        ar.result(),
                        varyAcceptEncoding,
                        responseEncoding,
                    )
                    WireProtocol.GRPC_WEB -> writeGrpcWebSuccess(
                        ctx,
                        callContext,
                        ar.result(),
                        grpcWebContentType,
                        responseEncoding,
                    )
                    WireProtocol.CONNECT_STREAM, WireProtocol.GRPC_NATIVE -> Unit
                }
            } else {
                val error = GrpcStatusMapping.toConnectException(ar.cause())
                when (wire) {
                    WireProtocol.CONNECT -> writeConnectError(
                        ctx,
                        error,
                        callContext,
                        varyAcceptEncoding,
                        responseEncoding,
                    )
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
        varyAcceptEncoding: Boolean = false,
        responseEncoding: String = ConnectCompression.IDENTITY,
    ) {
        val wireBody = ConnectCompression.compress(responseEncoding, responseBytes)
        val response = ctx.response().setStatusCode(200)
        response.putHeader(HttpHeaders.CONTENT_TYPE, codec.contentType)
        response.putHeader(HttpHeaders.ACCEPT_ENCODING, ConnectCompression.ACCEPT_ADVERTISEMENT)
        if (responseEncoding != ConnectCompression.IDENTITY) {
            response.putHeader(HttpHeaders.CONTENT_ENCODING, responseEncoding)
        }
        if (varyAcceptEncoding) {
            response.headers().add(HttpHeaders.VARY, "Accept-Encoding")
        }
        writeMetadata(response, callContext)
        response.end(io.vertx.core.buffer.Buffer.buffer(wireBody))
    }

    private fun writeConnectError(
        ctx: RoutingContext,
        error: ConnectException,
        callContext: DefaultConnectCallContext? = null,
        varyAcceptEncoding: Boolean = false,
        responseEncoding: String = ConnectCompression.IDENTITY,
    ) {
        if (ctx.response().headWritten()) {
            log.warn("Cannot write Connect error; response already started: ${error.code.wireName}")
            return
        }
        val json = ConnectErrorJson.toJson(error).toByteArray(StandardCharsets.UTF_8)
        val wireBody = ConnectCompression.compress(responseEncoding, json)
        val response = ctx.response().setStatusCode(error.code.httpStatus)
        response.putHeader(HttpHeaders.CONTENT_TYPE, ConnectProtocolConstants.CONTENT_TYPE_JSON)
        response.putHeader(HttpHeaders.ACCEPT_ENCODING, ConnectCompression.ACCEPT_ADVERTISEMENT)
        if (responseEncoding != ConnectCompression.IDENTITY) {
            response.putHeader(HttpHeaders.CONTENT_ENCODING, responseEncoding)
        }
        if (varyAcceptEncoding) {
            response.headers().add(HttpHeaders.VARY, "Accept-Encoding")
        }
        if (callContext != null) {
            writeMetadata(response, callContext)
        }
        response.end(io.vertx.core.buffer.Buffer.buffer(wireBody))
    }

    private fun writeGrpcWebSuccess(
        ctx: RoutingContext,
        callContext: DefaultConnectCallContext,
        protoBytes: ByteArray,
        contentType: String?,
        responseEncoding: String = ConnectCompression.IDENTITY,
    ) {
        val body = GrpcWebFraming.encodeUnarySuccess(
            protoBytes,
            callContext.snapshotTrailers(),
            responseEncoding,
        )
        val response = ctx.response().setStatusCode(200)
        response.putHeader(HttpHeaders.CONTENT_TYPE, normalizeGrpcWebContentType(contentType))
        response.putHeader("grpc-accept-encoding", GrpcWebFraming.ACCEPT_ENCODING_ADVERTISEMENT)
        if (responseEncoding != ConnectCompression.IDENTITY) {
            response.putHeader("grpc-encoding", responseEncoding)
        }
        val hdrs = response.headers()
        for ((name, values) in callContext.snapshotHeaders()) {
            for (value in values) {
                hdrs.add(name, value)
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
        val trailers = callContext?.snapshotTrailers() ?: emptyMap()
        val body = GrpcWebFraming.encodeUnaryError(statusCode, error.message, trailers)
        val response = ctx.response().setStatusCode(200)
        response.putHeader(HttpHeaders.CONTENT_TYPE, normalizeGrpcWebContentType(contentType))
        response.putHeader("grpc-accept-encoding", GrpcWebFraming.ACCEPT_ENCODING_ADVERTISEMENT)
        if (callContext != null) {
            val hdrs = response.headers()
            for ((name, values) in callContext.snapshotHeaders()) {
                for (value in values) {
                    hdrs.add(name, value)
                }
            }
        }
        response.end(io.vertx.core.buffer.Buffer.buffer(body))
    }

    /** HTTP 405 with Allow; empty body so clients map status → [ConnectCode.UNKNOWN]. */
    private fun writeMethodNotAllowed(ctx: RoutingContext, allow: String) {
        ctx.response()
            .setStatusCode(405)
            .putHeader("Allow", allow)
            .end()
    }

    /** HTTP 415 with Accept-Post; empty body so clients map status → [ConnectCode.UNKNOWN]. */
    private fun writeUnsupportedMediaType(ctx: RoutingContext, acceptPost: String) {
        ctx.response()
            .setStatusCode(415)
            .putHeader("Accept-Post", acceptPost)
            .end()
    }

    private fun writeMetadata(
        response: io.vertx.core.http.HttpServerResponse,
        callContext: DefaultConnectCallContext,
    ) {
        // Use headers().add — putHeader replaces prior values for the same name.
        val headers = response.headers()
        for ((name, values) in callContext.snapshotHeaders()) {
            for (value in values) {
                headers.add(name, value)
            }
        }
        for ((name, values) in callContext.snapshotTrailers()) {
            for (value in values) {
                headers.add(ConnectProtocolConstants.TRAILER_PREFIX + name, value)
            }
        }
    }

    /** Connect streaming leading metadata (HTTP response headers). Trailers go in end-stream JSON. */
    private fun writeStreamResponseHeaders(
        response: io.vertx.core.http.HttpServerResponse,
        callContext: DefaultConnectCallContext,
    ) {
        val headers = response.headers()
        for ((name, values) in callContext.snapshotHeaders()) {
            for (value in values) {
                headers.add(name, value)
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

    /**
     * Connect `connect-timeout-ms`: positive integer, at most 10 ASCII digits (protocol ABNF).
     * Omitted → null (no deadline from the Connect header).
     */
    private fun parseConnectTimeoutMs(raw: String?): Long? {
        if (raw.isNullOrBlank()) return null
        if (raw.length > 10 || raw.any { !it.isDigit() }) {
            throw ConnectException.invalidArgument(
                "Invalid connect-timeout-ms '$raw' (positive integer, at most 10 digits)",
            )
        }
        val ms = raw.toLong()
        if (ms <= 0L) {
            throw ConnectException.invalidArgument(
                "Invalid connect-timeout-ms '$raw' (must be positive)",
            )
        }
        return ms
    }

    /** gRPC-Web `grpc-timeout` (units) or legacy millisecond strings. */
    private fun parseTimeout(raw: String?): Long? {
        if (raw.isNullOrBlank()) return null
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

    private fun unimplementedStreaming(procedure: String, grpcType: String): ConnectException =
        ConnectException(
            ConnectCode.UNIMPLEMENTED,
            "Connect does not support $grpcType for /$procedure yet",
        )

    companion object {
        private const val ACCEPT_POST_UNARY = "application/json, application/proto"
        private const val ACCEPT_POST_STREAM =
            "application/connect+json, application/connect+proto"

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

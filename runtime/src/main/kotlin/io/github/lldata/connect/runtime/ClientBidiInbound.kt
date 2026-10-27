package io.github.lldata.connect.runtime

import com.google.protobuf.Message
import io.github.lldata.connect.protocol.IncrementalEnvelopeParser
import io.github.lldata.connect.protocol.ProtobufCodecSupport
import io.github.lldata.connect.runtime.grpc.GrpcBridgedBidiStreamMethod
import io.github.lldata.connect.runtime.grpc.GrpcBridgedClientStreamMethod
import io.github.lldata.connect.runtime.grpc.GrpcStatusMapping
import io.github.lldata.connect.runtime.grpc.OpenBidiStream
import io.github.lldata.connect.runtime.grpc.OpenClientStream
import io.github.lldata.connect.spi.ConnectBidiStreamMethod
import io.github.lldata.connect.spi.ConnectCallContext
import io.github.lldata.connect.spi.ConnectClientStreamMethod
import io.github.lldata.connect.spi.ConnectCodec
import io.github.lldata.connect.spi.ConnectException
import io.github.lldata.connect.spi.ConnectStreamObserver
import io.vertx.ext.web.RoutingContext
import java.util.concurrent.atomic.AtomicReference

/**
 * Shared inbound wiring for Connect / gRPC-Web client and bidi streaming:
 * incremental envelopes → gRPC bridge session (or SPI list collected after decode).
 */
internal object ClientBidiInbound {

    fun handleClientStream(
        ctx: RoutingContext,
        method: ConnectClientStreamMethod<out Message, out Message>,
        callContext: ConnectCallContext,
        codecs: ProtobufCodecSupport,
        codec: ConnectCodec,
        mode: IncrementalEnvelopeParser.Mode,
        requestEncoding: String?,
        maxInboundMessageSize: Long,
        onResponse: (ByteArray) -> Unit,
        onError: (ConnectException) -> Unit,
    ) {
        @Suppress("UNCHECKED_CAST")
        val typed = method as ConnectClientStreamMethod<Message, Message>
        val req = ctx.request()
        val vertx = ctx.vertx()

        if (typed is GrpcBridgedClientStreamMethod<*, *>) {
            @Suppress("UNCHECKED_CAST")
            val bridge = typed as GrpcBridgedClientStreamMethod<Message, Message>
            val sessionRef = AtomicReference<OpenClientStream<Message, Message>>()
            val held = AtomicReference<Message?>(null)

            fun resumeIfPossible() {
                vertx.runOnContext {
                    val session = sessionRef.get() ?: return@runOnContext
                    val pending = held.getAndSet(null)
                    if (pending != null) {
                        if (session.trySend(pending)) {
                            if (!req.isEnded) req.resume()
                        } else {
                            held.set(pending)
                        }
                    } else if (!req.isEnded) {
                        req.resume()
                    }
                }
            }

            val session = bridge.open(callContext, onWritable = ::resumeIfPossible)
            sessionRef.set(session)

            StreamingRequestReader.read(
                ctx = ctx,
                mode = mode,
                requestEncoding = requestEncoding,
                maxInboundMessageSize = maxInboundMessageSize,
                onData = { payload ->
                    val msg = codecs.parse(payload, typed.requestType(), codec)
                    if (!session.trySend(msg)) {
                        held.set(msg)
                        req.pause()
                    }
                },
                onEnd = {
                    session.halfClose()
                    vertx.executeBlocking(
                        java.util.concurrent.Callable {
                            codecs.encode(session.awaitResponse(), codec)
                        },
                        false,
                    ).onComplete { ar ->
                        if (ar.succeeded()) {
                            onResponse(ar.result())
                        } else {
                            onError(GrpcStatusMapping.toConnectException(ar.cause()))
                        }
                    }
                },
                onError = { err ->
                    session.cancel()
                    onError(err)
                },
            )
            return
        }

        // SPI beans: collect decoded messages (not the raw HTTP body), then handle(List).
        val collected = mutableListOf<Message>()
        StreamingRequestReader.read(
            ctx = ctx,
            mode = mode,
            requestEncoding = requestEncoding,
            maxInboundMessageSize = maxInboundMessageSize,
            onData = { payload ->
                collected += codecs.parse(payload, typed.requestType(), codec)
            },
            onEnd = {
                vertx.executeBlocking(
                    java.util.concurrent.Callable {
                        codecs.encode(typed.handle(collected, callContext), codec)
                    },
                    false,
                ).onComplete { ar ->
                    if (ar.succeeded()) {
                        onResponse(ar.result())
                    } else {
                        onError(GrpcStatusMapping.toConnectException(ar.cause()))
                    }
                }
            },
            onError = onError,
        )
    }

    fun handleBidiStream(
        ctx: RoutingContext,
        method: ConnectBidiStreamMethod<out Message, out Message>,
        callContext: ConnectCallContext,
        codecs: ProtobufCodecSupport,
        codec: ConnectCodec,
        mode: IncrementalEnvelopeParser.Mode,
        requestEncoding: String?,
        maxInboundMessageSize: Long,
        observer: ConnectStreamObserver<Message>,
        onInboundError: (ConnectException) -> Unit,
    ) {
        @Suppress("UNCHECKED_CAST")
        val typed = method as ConnectBidiStreamMethod<Message, Message>
        val req = ctx.request()
        val vertx = ctx.vertx()

        if (typed is GrpcBridgedBidiStreamMethod<*, *>) {
            @Suppress("UNCHECKED_CAST")
            val bridge = typed as GrpcBridgedBidiStreamMethod<Message, Message>
            val sessionRef = AtomicReference<OpenBidiStream<Message, Message>>()
            val held = AtomicReference<Message?>(null)

            fun resumeIfPossible() {
                vertx.runOnContext {
                    val session = sessionRef.get() ?: return@runOnContext
                    val pending = held.getAndSet(null)
                    if (pending != null) {
                        if (session.trySend(pending)) {
                            if (!req.isEnded) req.resume()
                        } else {
                            held.set(pending)
                        }
                    } else if (!req.isEnded) {
                        req.resume()
                    }
                }
            }

            val session = bridge.open(callContext, observer, onWritable = ::resumeIfPossible)
            sessionRef.set(session)

            StreamingRequestReader.read(
                ctx = ctx,
                mode = mode,
                requestEncoding = requestEncoding,
                maxInboundMessageSize = maxInboundMessageSize,
                onData = { payload ->
                    val msg = codecs.parse(payload, typed.requestType(), codec)
                    if (!session.trySend(msg)) {
                        held.set(msg)
                        req.pause()
                    }
                },
                onEnd = {
                    session.halfClose()
                },
                onError = { err ->
                    session.cancel()
                    onInboundError(err)
                },
            )
            return
        }

        val collected = mutableListOf<Message>()
        StreamingRequestReader.read(
            ctx = ctx,
            mode = mode,
            requestEncoding = requestEncoding,
            maxInboundMessageSize = maxInboundMessageSize,
            onData = { payload ->
                collected += codecs.parse(payload, typed.requestType(), codec)
            },
            onEnd = {
                vertx.executeBlocking(
                    java.util.concurrent.Callable {
                        typed.handle(collected, callContext, observer)
                        null
                    },
                    false,
                ).onComplete { ar ->
                    if (ar.failed()) {
                        onInboundError(GrpcStatusMapping.toConnectException(ar.cause()))
                    }
                }
            },
            onError = onInboundError,
        )
    }
}

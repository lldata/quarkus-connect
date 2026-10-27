package io.github.lldata.connect.runtime

import io.github.lldata.connect.protocol.IncrementalEnvelopeParser
import io.github.lldata.connect.spi.ConnectCode
import io.github.lldata.connect.spi.ConnectException
import io.vertx.ext.web.RoutingContext
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Pipes an HTTP request body into an [IncrementalEnvelopeParser].
 *
 * Callers apply back-pressure by pausing [io.vertx.core.http.HttpServerRequest] when their
 * downstream queue is full and resuming when drained.
 */
internal object StreamingRequestReader {

    /** RoutingContext key: body is not pre-buffered; read from the raw request. */
    const val STREAM_BODY_KEY = "io.github.lldata.connect.streamBody"

    /**
     * Invoke [onData] for each decoded envelope payload as it completes, then [onEnd]
     * when the request finishes (after [IncrementalEnvelopeParser.finish]).
     */
    fun read(
        ctx: RoutingContext,
        mode: IncrementalEnvelopeParser.Mode,
        requestEncoding: String?,
        maxInboundMessageSize: Long,
        onData: (ByteArray) -> Unit,
        onEnd: () -> Unit,
        onError: (ConnectException) -> Unit,
    ) {
        val parser = IncrementalEnvelopeParser(mode, requestEncoding, maxInboundMessageSize)
        val req = ctx.request()
        val done = AtomicBoolean(false)

        fun fail(err: ConnectException) {
            if (!done.compareAndSet(false, true)) return
            try {
                req.pause()
            } catch (_: Exception) {
                // ignore
            }
            onError(err)
        }

        fun accept(chunk: ByteArray) {
            if (done.get()) return
            try {
                for (payload in parser.feed(chunk)) {
                    onData(payload)
                }
            } catch (e: ConnectException) {
                fail(e)
            } catch (e: Exception) {
                fail(ConnectException(ConnectCode.INTERNAL, e.message, e))
            }
        }

        fun complete() {
            if (!done.compareAndSet(false, true)) return
            try {
                for (payload in parser.finish()) {
                    onData(payload)
                }
                onEnd()
            } catch (e: ConnectException) {
                onError(e)
            } catch (e: Exception) {
                onError(ConnectException(ConnectCode.INTERNAL, e.message, e))
            }
        }

        // Prior BodyHandler path: feed the already-buffered body in one shot.
        val buffered = ctx.body()?.buffer()
        if (buffered != null && ctx.get<Boolean>(STREAM_BODY_KEY) != true) {
            accept(buffered.bytes ?: ByteArray(0))
            complete()
            return
        }

        req.handler { buffer ->
            accept(buffer.bytes ?: ByteArray(0))
        }
        req.endHandler {
            complete()
        }
        req.exceptionHandler { err ->
            fail(
                when (err) {
                    is ConnectException -> err
                    else -> ConnectException(ConnectCode.INTERNAL, err.message, err)
                },
            )
        }
        if (!req.isEnded) {
            req.resume()
        }
    }
}

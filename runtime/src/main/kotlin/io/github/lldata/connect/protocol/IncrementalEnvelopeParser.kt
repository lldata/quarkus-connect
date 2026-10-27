package io.github.lldata.connect.protocol

import io.github.lldata.connect.spi.ConnectCode
import io.github.lldata.connect.spi.ConnectException
import java.nio.ByteBuffer

/**
 * Incremental Connect / gRPC-Web envelope parser (shared 5-byte framing).
 *
 * Feed request bytes as they arrive; each completed data envelope is returned from [feed]
 * as a decoded payload (compression applied). Partial envelopes are retained until the
 * next chunk. Call [finish] when the HTTP request ends to reject a truncated trailing frame.
 */
class IncrementalEnvelopeParser(
    private val mode: Mode,
    private val requestEncoding: String? = ConnectCompression.IDENTITY,
    private val maxInboundMessageSize: Long = ConnectMessageLimits.DEFAULT_MAX_INBOUND_MESSAGE_SIZE,
) {
    enum class Mode {
        /** Connect streaming: end-stream bit in requests is invalid. */
        CONNECT,

        /** gRPC-Web: trailer frames (`0x80`) are skipped. */
        GRPC_WEB,
    }

    private var buf = ByteArray(256)
    private var size = 0
    private var finished = false

    /**
     * Consume [chunk] and return every newly completed **data** message payload
     * (decompressed, size-checked).
     */
    fun feed(chunk: ByteArray): List<ByteArray> {
        check(!finished) { "parser already finished" }
        if (chunk.isEmpty()) return emptyList()
        ensureCapacity(size + chunk.size)
        System.arraycopy(chunk, 0, buf, size, chunk.size)
        size += chunk.size
        return drainComplete()
    }

    /** Signal end-of-request. Throws if a partial envelope remains. */
    fun finish(): List<ByteArray> {
        check(!finished) { "parser already finished" }
        finished = true
        val out = drainComplete()
        if (size != 0) {
            throw ConnectException(
                ConnectCode.INVALID_ARGUMENT,
                if (mode == Mode.CONNECT) "Truncated Connect envelope" else "Truncated gRPC-Web frame",
            )
        }
        return out
    }

    private fun drainComplete(): List<ByteArray> {
        val out = mutableListOf<ByteArray>()
        var offset = 0
        while (true) {
            if (size - offset < 5) break
            val flag = buf[offset].toInt() and 0xff
            val length = ByteBuffer.wrap(buf, offset + 1, 4).int
            ConnectMessageLimits.requireEnvelopeLength(length, maxInboundMessageSize)
            if (size - offset < 5 + length) break

            val payload = buf.copyOfRange(offset + 5, offset + 5 + length)
            offset += 5 + length

            if (mode == Mode.CONNECT) {
                if (flag and ConnectEnvelope.FLAG_END_STREAM != 0) {
                    throw ConnectException(
                        ConnectCode.INVALID_ARGUMENT,
                        "Request stream must not send end-stream",
                    )
                }
                out += decodePayload(flag, payload)
            } else if (flag and GRPC_WEB_TRAILER_FLAG != 0) {
                continue
            } else {
                out += decodePayload(flag, payload)
            }
        }
        if (offset > 0) {
            val remain = size - offset
            if (remain > 0) {
                System.arraycopy(buf, offset, buf, 0, remain)
            }
            size = remain
        }
        return out
    }

    private fun decodePayload(flag: Int, payload: ByteArray): ByteArray {
        val compressed = flag and FLAG_COMPRESSED != 0
        val decoded = if (!compressed) {
            payload
        } else {
            val encoding = ConnectCompression.normalize(requestEncoding)
            if (encoding == ConnectCompression.IDENTITY) {
                throw ConnectException(
                    ConnectCode.INTERNAL,
                    "protocol error: sent compressed message without compression support",
                )
            }
            ConnectCompression.decompress(encoding, payload)
        }
        ConnectMessageLimits.requireWithinLimit(decoded.size, maxInboundMessageSize)
        return decoded
    }

    private fun ensureCapacity(min: Int) {
        if (buf.size >= min) return
        var cap = buf.size
        while (cap < min) {
            cap *= 2
            if (cap < 0 || cap > MAX_BUFFER) {
                throw ConnectException(
                    ConnectCode.RESOURCE_EXHAUSTED,
                    "inbound framing buffer exceeds $MAX_BUFFER bytes",
                )
            }
        }
        buf = buf.copyOf(cap)
    }

    companion object {
        private const val FLAG_COMPRESSED = 0x01
        private const val GRPC_WEB_TRAILER_FLAG = 0x80
        private const val MAX_BUFFER = Int.MAX_VALUE / 2
    }
}

package io.github.lldata.connect.runtime.grpc

import io.github.lldata.connect.protocol.ConnectCompression
import io.github.lldata.connect.protocol.ConnectMessageLimits
import io.github.lldata.connect.spi.ConnectCode
import io.github.lldata.connect.spi.ConnectException
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

/**
 * gRPC-Web framing: 5-byte envelopes (flag + big-endian length + payload).
 * Flag bit0 = compressed; bit7 (`0x80`) = trailers.
 */
object GrpcWebFraming {

    const val FLAG_DATA: Int = 0x00
    const val FLAG_COMPRESSED: Int = 0x01
    const val FLAG_TRAILER: Int = 0x80

    /** Advertised on `grpc-accept-encoding`. */
    const val ACCEPT_ENCODING_ADVERTISEMENT = "${ConnectCompression.GZIP}, ${ConnectCompression.IDENTITY}"

    fun encodeDataFrame(payload: ByteArray, compressed: Boolean = false): ByteArray {
        val flags = if (compressed) FLAG_COMPRESSED else FLAG_DATA
        return encodeFrame(flags, payload)
    }

    fun encodeDataMaybeCompressed(payload: ByteArray, responseEncoding: String?): ByteArray {
        val encoding = ConnectCompression.normalize(responseEncoding)
        if (encoding == ConnectCompression.IDENTITY || payload.isEmpty()) {
            return encodeDataFrame(payload, compressed = false)
        }
        return encodeDataFrame(ConnectCompression.compress(encoding, payload), compressed = true)
    }

    fun encodeTrailerFrame(
        statusCode: Int,
        message: String? = null,
        extra: Map<String, List<String>> = emptyMap(),
    ): ByteArray {
        val sb = StringBuilder()
        sb.append("grpc-status: ").append(statusCode).append("\r\n")
        if (!message.isNullOrEmpty()) {
            sb.append("grpc-message: ").append(percentEncode(message)).append("\r\n")
        }
        for ((k, values) in extra) {
            val name = k.lowercase()
            if (name == "grpc-status" || name == "grpc-message") continue
            for (v in values) {
                sb.append(name).append(": ").append(v).append("\r\n")
            }
        }
        return encodeFrame(FLAG_TRAILER, sb.toString().toByteArray(StandardCharsets.UTF_8))
    }

    fun encodeUnarySuccess(
        message: ByteArray,
        trailers: Map<String, List<String>> = emptyMap(),
        responseEncoding: String? = ConnectCompression.IDENTITY,
    ): ByteArray {
        val data = encodeDataMaybeCompressed(message, responseEncoding)
        val trailer = encodeTrailerFrame(0, null, trailers)
        return data + trailer
    }

    fun encodeUnaryError(
        statusCode: Int,
        message: String?,
        trailers: Map<String, List<String>> = emptyMap(),
    ): ByteArray = encodeTrailerFrame(statusCode, message, trailers)

    /**
     * Decode every data frame from a gRPC-Web request body.
     * Trailer frames in requests are ignored (clients should not send them).
     */
    fun decodeAllDataMessages(
        body: ByteArray,
        grpcEncoding: String? = ConnectCompression.IDENTITY,
        maxInboundMessageSize: Long = ConnectMessageLimits.DEFAULT_MAX_INBOUND_MESSAGE_SIZE,
    ): List<ByteArray> {
        if (body.isEmpty()) return emptyList()
        val out = mutableListOf<ByteArray>()
        var offset = 0
        while (offset + 5 <= body.size) {
            val flag = body[offset].toInt() and 0xff
            val length = ByteBuffer.wrap(body, offset + 1, 4).int
            ConnectMessageLimits.requireEnvelopeLength(length, maxInboundMessageSize)
            if (offset + 5 + length > body.size) {
                throw ConnectException(ConnectCode.INVALID_ARGUMENT, "Invalid gRPC-Web frame length: $length")
            }
            val payload = body.copyOfRange(offset + 5, offset + 5 + length)
            offset += 5 + length
            if (flag and FLAG_TRAILER != 0) {
                continue
            }
            val decoded = decodePayload(flag, payload, grpcEncoding)
            ConnectMessageLimits.requireWithinLimit(decoded.size, maxInboundMessageSize)
            out += decoded
        }
        if (offset != body.size) {
            throw ConnectException(ConnectCode.INVALID_ARGUMENT, "Truncated gRPC-Web frame")
        }
        return out
    }

    /**
     * Unary / server-stream request cardinality: exactly one data message
     * ([ConnectCode.UNIMPLEMENTED] on violation — gRPC cardinality guidance).
     */
    fun decodeExactlyOneDataMessage(
        body: ByteArray,
        grpcEncoding: String? = ConnectCompression.IDENTITY,
        maxInboundMessageSize: Long = ConnectMessageLimits.DEFAULT_MAX_INBOUND_MESSAGE_SIZE,
    ): ByteArray {
        val all = decodeAllDataMessages(body, grpcEncoding, maxInboundMessageSize)
        return when (all.size) {
            0 -> throw ConnectException(
                ConnectCode.UNIMPLEMENTED,
                "unary request has zero messages",
            )
            1 -> all[0]
            else -> throw ConnectException(
                ConnectCode.UNIMPLEMENTED,
                "unary request has multiple messages",
            )
        }
    }

    /**
     * Extract the single data-frame payload from a gRPC-Web unary request body.
     */
    fun decodeRequestMessage(
        body: ByteArray,
        grpcEncoding: String? = ConnectCompression.IDENTITY,
        maxInboundMessageSize: Long = ConnectMessageLimits.DEFAULT_MAX_INBOUND_MESSAGE_SIZE,
    ): ByteArray = decodeExactlyOneDataMessage(body, grpcEncoding, maxInboundMessageSize)

    private fun decodePayload(flag: Int, payload: ByteArray, grpcEncoding: String?): ByteArray {
        if (flag and FLAG_COMPRESSED == 0) return payload
        val encoding = ConnectCompression.normalize(grpcEncoding)
        if (encoding == ConnectCompression.IDENTITY) {
            throw ConnectException(
                ConnectCode.INTERNAL,
                "protocol error: sent compressed message without compression support",
            )
        }
        return ConnectCompression.decompress(encoding, payload)
    }

    private fun encodeFrame(flag: Int, payload: ByteArray): ByteArray {
        val out = ByteArrayOutputStream(5 + payload.size)
        out.write(flag)
        out.write((payload.size ushr 24) and 0xff)
        out.write((payload.size ushr 16) and 0xff)
        out.write((payload.size ushr 8) and 0xff)
        out.write(payload.size and 0xff)
        out.write(payload)
        return out.toByteArray()
    }

    private fun percentEncode(value: String): String {
        val bytes = value.toByteArray(StandardCharsets.UTF_8)
        val sb = StringBuilder(bytes.size * 3)
        for (b in bytes) {
            val c = b.toInt() and 0xff
            if (c in 0x20..0x7e && c != '%'.code && c != '\\'.code) {
                sb.append(c.toChar())
            } else {
                sb.append('%')
                sb.append("0123456789ABCDEF"[c ushr 4])
                sb.append("0123456789ABCDEF"[c and 0x0f])
            }
        }
        return sb.toString()
    }
}

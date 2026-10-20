package io.github.lldata.connect.runtime.grpc

import io.github.lldata.connect.spi.ConnectCode
import io.github.lldata.connect.spi.ConnectException
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

/**
 * gRPC-Web unary framing: 5-byte envelopes (flag + big-endian length + payload).
 * Flag `0x00` = data, `0x80` = trailers.
 */
object GrpcWebFraming {

    const val FLAG_DATA: Int = 0x00
    const val FLAG_TRAILER: Int = 0x80

    fun encodeDataFrame(payload: ByteArray): ByteArray = encodeFrame(FLAG_DATA, payload)

    fun encodeTrailerFrame(statusCode: Int, message: String? = null, extra: Map<String, String> = emptyMap()): ByteArray {
        val sb = StringBuilder()
        sb.append("grpc-status: ").append(statusCode).append("\r\n")
        if (!message.isNullOrEmpty()) {
            sb.append("grpc-message: ").append(percentEncode(message)).append("\r\n")
        }
        for ((k, v) in extra) {
            sb.append(k.lowercase()).append(": ").append(v).append("\r\n")
        }
        return encodeFrame(FLAG_TRAILER, sb.toString().toByteArray(StandardCharsets.UTF_8))
    }

    fun encodeUnarySuccess(message: ByteArray, trailers: Map<String, String> = emptyMap()): ByteArray {
        val data = encodeDataFrame(message)
        val trailer = encodeTrailerFrame(0, null, trailers)
        return data + trailer
    }

    fun encodeUnaryError(statusCode: Int, message: String?): ByteArray =
        encodeTrailerFrame(statusCode, message)

    /**
     * Extract the first data-frame payload from a gRPC-Web request body.
     */
    fun decodeRequestMessage(body: ByteArray): ByteArray {
        if (body.isEmpty()) {
            return ByteArray(0)
        }
        if (body.size < 5) {
            throw ConnectException(ConnectCode.INVALID_ARGUMENT, "Truncated gRPC-Web frame")
        }
        val flag = body[0].toInt() and 0xff
        if (flag and FLAG_TRAILER != 0) {
            throw ConnectException(ConnectCode.INVALID_ARGUMENT, "gRPC-Web request must start with a data frame")
        }
        if (flag and 0x01 != 0) {
            throw ConnectException(ConnectCode.UNIMPLEMENTED, "Compressed gRPC-Web frames are not supported")
        }
        val length = ByteBuffer.wrap(body, 1, 4).int
        if (length < 0 || 5 + length > body.size) {
            throw ConnectException(ConnectCode.INVALID_ARGUMENT, "Invalid gRPC-Web frame length: $length")
        }
        return body.copyOfRange(5, 5 + length)
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

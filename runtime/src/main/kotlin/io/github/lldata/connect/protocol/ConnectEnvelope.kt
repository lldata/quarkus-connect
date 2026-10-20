package io.github.lldata.connect.protocol

import io.github.lldata.connect.spi.ConnectCode
import io.github.lldata.connect.spi.ConnectException
import io.vertx.core.json.JsonObject
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

/**
 * Connect streaming envelopes: 1 flag byte + 4-byte BE length + payload.
 * Flag bit0 = compressed; bit1 = end-stream (response only).
 */
object ConnectEnvelope {

    const val FLAG_COMPRESSED: Int = 0x01
    const val FLAG_END_STREAM: Int = 0x02

    const val CONTENT_TYPE_CONNECT_PROTO = "application/connect+proto"
    const val CONTENT_TYPE_CONNECT_JSON = "application/connect+json"

    fun encodeData(payload: ByteArray, compressed: Boolean = false): ByteArray {
        val flags = if (compressed) FLAG_COMPRESSED else 0
        return encode(flags, payload)
    }

    fun encodeEndStream(error: ConnectException? = null, metadata: Map<String, List<String>> = emptyMap()): ByteArray {
        val obj = JsonObject()
        if (error != null) {
            val err = JsonObject().put("code", error.code.wireName)
            if (!error.message.isNullOrEmpty()) {
                err.put("message", error.message)
            }
            if (error.details.isNotEmpty()) {
                val details = io.vertx.core.json.JsonArray()
                for (d in error.details) {
                    details.add(
                        JsonObject()
                            .put("type", d.type)
                            .put("value", d.valueBase64),
                    )
                }
                err.put("details", details)
            }
            obj.put("error", err)
        }
        if (metadata.isNotEmpty()) {
            val meta = JsonObject()
            for ((k, values) in metadata) {
                meta.put(k, io.vertx.core.json.JsonArray(values))
            }
            obj.put("metadata", meta)
        }
        return encode(FLAG_END_STREAM, obj.encode().toByteArray(StandardCharsets.UTF_8))
    }

    /**
     * Decode the first data envelope from a Connect streaming request body.
     */
    fun decodeFirstDataMessage(body: ByteArray): ByteArray {
        if (body.size < 5) {
            throw ConnectException(ConnectCode.INVALID_ARGUMENT, "Truncated Connect envelope")
        }
        val flag = body[0].toInt() and 0xff
        if (flag and FLAG_COMPRESSED != 0) {
            throw ConnectException(ConnectCode.UNIMPLEMENTED, "Compressed Connect envelopes are not supported")
        }
        if (flag and FLAG_END_STREAM != 0) {
            throw ConnectException(ConnectCode.INVALID_ARGUMENT, "Request stream must not send end-stream")
        }
        val length = ByteBuffer.wrap(body, 1, 4).int
        if (length < 0 || 5 + length > body.size) {
            throw ConnectException(ConnectCode.INVALID_ARGUMENT, "Invalid Connect envelope length: $length")
        }
        return body.copyOfRange(5, 5 + length)
    }

    fun isConnectStreamingContentType(contentType: String?): Boolean {
        val media = contentType?.substringBefore(';')?.trim()?.lowercase().orEmpty()
        return media == CONTENT_TYPE_CONNECT_PROTO || media == CONTENT_TYPE_CONNECT_JSON
    }

    fun codecWireName(contentType: String?): String {
        val media = contentType?.substringBefore(';')?.trim()?.lowercase().orEmpty()
        return if (media.endsWith("+json")) "json" else "proto"
    }

    fun normalizeContentType(contentType: String?): String {
        val media = contentType?.substringBefore(';')?.trim()?.lowercase().orEmpty()
        return when (media) {
            CONTENT_TYPE_CONNECT_JSON -> CONTENT_TYPE_CONNECT_JSON
            else -> CONTENT_TYPE_CONNECT_PROTO
        }
    }

    private fun encode(flags: Int, payload: ByteArray): ByteArray {
        val out = ByteArrayOutputStream(5 + payload.size)
        out.write(flags)
        out.write((payload.size ushr 24) and 0xff)
        out.write((payload.size ushr 16) and 0xff)
        out.write((payload.size ushr 8) and 0xff)
        out.write(payload.size and 0xff)
        out.write(payload)
        return out.toByteArray()
    }
}

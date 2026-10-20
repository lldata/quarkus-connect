package io.github.lldata.connect.protocol

import io.github.lldata.connect.spi.ConnectCode
import io.github.lldata.connect.spi.ConnectException
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * Connect content coding for unary (HTTP `Content-Encoding` / `Accept-Encoding`) and
 * streaming (`connect-content-encoding` / `connect-accept-encoding`).
 *
 * v1 supports **identity** and **gzip**. Other codings (including `br`) return
 * `unimplemented` — see `docs/COMPLIANCE.md` for why brotli is deferred.
 */
object ConnectCompression {

    const val IDENTITY = ConnectProtocolConstants.IDENTITY_ENCODING
    const val GZIP = "gzip"

    /** Advertised on unary `Accept-Encoding` and streaming `connect-accept-encoding`. */
    const val ACCEPT_ADVERTISEMENT = "$GZIP, $IDENTITY"

    fun isSupported(encoding: String?): Boolean {
        if (encoding.isNullOrBlank()) return true
        val name = encoding.trim().lowercase()
        return name == IDENTITY || name == GZIP
    }

    fun normalize(encoding: String?): String {
        if (encoding.isNullOrBlank()) return IDENTITY
        return encoding.trim().lowercase()
    }

    /**
     * Decompress [data] according to [encoding]. Zero-length payloads are returned as-is
     * (spec: servers must not attempt to decompress zero-length content).
     */
    fun decompress(encoding: String?, data: ByteArray): ByteArray {
        val name = normalize(encoding)
        if (data.isEmpty() || name == IDENTITY) return data
        return when (name) {
            GZIP -> gunzip(data)
            else -> throw ConnectException(
                ConnectCode.UNIMPLEMENTED,
                unsupportedMessage(encoding),
            )
        }
    }

    fun compress(encoding: String?, data: ByteArray): ByteArray {
        val name = normalize(encoding)
        if (data.isEmpty() || name == IDENTITY) return data
        return when (name) {
            GZIP -> gzip(data)
            else -> throw ConnectException(
                ConnectCode.UNIMPLEMENTED,
                unsupportedMessage(encoding),
            )
        }
    }

    fun requireSupported(encoding: String?, headerLabel: String): String {
        val name = normalize(encoding)
        if (!isSupported(name)) {
            throw ConnectException(ConnectCode.UNIMPLEMENTED, unsupportedMessage(encoding, headerLabel))
        }
        return name
    }

    /**
     * Negotiate response coding from the client's Accept-Encoding (or streaming
     * connect-accept-encoding) list.
     *
     * Spec: if Accept-Encoding is omitted, assume the client accepts the request
     * Content-Encoding when present; always accept identity as least preferred.
     */
    fun negotiateResponseEncoding(acceptEncodingHeader: String?, requestEncoding: String?): String {
        val preferred = parseAcceptList(acceptEncodingHeader)
        if (preferred.isEmpty()) {
            val request = normalize(requestEncoding)
            return if (request != IDENTITY && isSupported(request)) request else IDENTITY
        }
        for (coding in preferred) {
            if (isSupported(coding) && coding != IDENTITY) {
                return coding
            }
        }
        return IDENTITY
    }

    /**
     * Ordered list of content codings from an Accept-Encoding style header.
     * Quality values are ignored (Connect subset); empty tokens skipped.
     */
    fun parseAcceptList(header: String?): List<String> {
        if (header.isNullOrBlank()) return emptyList()
        return header.split(',')
            .map { it.substringBefore(';').trim().lowercase() }
            .filter { it.isNotEmpty() && it != "*" }
    }

    fun unsupportedMessage(encoding: String?, headerLabel: String = "encoding"): String =
        "Unsupported $headerLabel '${encoding?.trim()}'; supported: $ACCEPT_ADVERTISEMENT"

    private fun gzip(data: ByteArray): ByteArray {
        val out = ByteArrayOutputStream(data.size + 32)
        GZIPOutputStream(out).use { it.write(data) }
        return out.toByteArray()
    }

    private fun gunzip(data: ByteArray): ByteArray {
        try {
            return GZIPInputStream(ByteArrayInputStream(data)).use { it.readBytes() }
        } catch (e: Exception) {
            throw ConnectException(
                ConnectCode.INVALID_ARGUMENT,
                "Invalid gzip payload: ${e.message}",
            )
        }
    }
}

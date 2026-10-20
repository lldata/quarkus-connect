package io.github.lldata.connect.spi

/**
 * Wire codecs for unary Connect RPCs.
 */
enum class ConnectCodec(val contentType: String, val wireName: String) {
    PROTO("application/proto", "proto"),
    JSON("application/json", "json");

    companion object {
        @JvmStatic
        fun fromContentType(contentType: String?): ConnectCodec? {
            if (contentType.isNullOrBlank()) return null
            val media = contentType.substringBefore(';').trim().lowercase()
            return entries.firstOrNull { it.contentType == media }
        }

        @JvmStatic
        fun fromWireName(name: String?): ConnectCodec? =
            entries.firstOrNull { it.wireName.equals(name, ignoreCase = true) }
    }
}

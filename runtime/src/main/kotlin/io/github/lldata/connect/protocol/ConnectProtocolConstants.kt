package io.github.lldata.connect.protocol

object ConnectProtocolConstants {
    const val PROTOCOL_VERSION_HEADER = "connect-protocol-version"
    /** Value of [PROTOCOL_VERSION_HEADER] (POST unary / streaming). */
    const val PROTOCOL_VERSION = "1"
    /**
     * Unary GET query `connect=` value per the Connect protocol reference (`connect=v1`).
     * Distinct from the header value `"1"`.
     */
    const val PROTOCOL_VERSION_GET = "v1"
    const val TIMEOUT_HEADER = "connect-timeout-ms"
    /** Streaming message compression (not whole-body Content-Encoding). */
    const val STREAMING_CONTENT_ENCODING_HEADER = "connect-content-encoding"
    const val STREAMING_ACCEPT_ENCODING_HEADER = "connect-accept-encoding"
    const val TRAILER_PREFIX = "trailer-"
    const val CONTENT_TYPE_PROTO = "application/proto"
    const val CONTENT_TYPE_JSON = "application/json"
    const val IDENTITY_ENCODING = "identity"

    /** True when a GET `connect` query is absent or a supported version. */
    @JvmStatic
    fun isSupportedGetConnectVersion(connectQuery: String?): Boolean {
        if (connectQuery.isNullOrBlank()) return true
        return connectQuery == PROTOCOL_VERSION_GET || connectQuery == PROTOCOL_VERSION
    }
}

package io.github.lldata.connect.spi

/**
 * Per-call metadata available to unary handlers.
 * Headers are lower-cased ASCII names; binary metadata uses `-bin` suffixes (base64 values).
 */
interface ConnectCallContext {
    val procedure: String
    val codec: ConnectCodec
    val requestHeaders: Map<String, List<String>>
    val timeoutMs: Long?

    fun responseHeader(name: String, value: String)
    fun responseTrailer(name: String, value: String)
}

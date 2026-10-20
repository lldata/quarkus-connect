package io.github.lldata.connect.runtime

import io.github.lldata.connect.spi.ConnectCallContext
import io.github.lldata.connect.spi.ConnectCodec

class DefaultConnectCallContext(
    override val procedure: String,
    override val codec: ConnectCodec,
    override val requestHeaders: Map<String, List<String>>,
    override val timeoutMs: Long?,
) : ConnectCallContext {

    private val responseHeaders = linkedMapOf<String, MutableList<String>>()
    private val responseTrailers = linkedMapOf<String, MutableList<String>>()

    override fun responseHeader(name: String, value: String) {
        responseHeaders.getOrPut(name.lowercase()) { mutableListOf() }.add(value)
    }

    override fun responseTrailer(name: String, value: String) {
        responseTrailers.getOrPut(name.lowercase()) { mutableListOf() }.add(value)
    }

    fun snapshotHeaders(): Map<String, List<String>> =
        responseHeaders.mapValues { it.value.toList() }

    fun snapshotTrailers(): Map<String, List<String>> =
        responseTrailers.mapValues { it.value.toList() }
}

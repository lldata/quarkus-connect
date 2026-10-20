package io.github.lldata.connect.conformance

import connectrpc.conformance.v1.ServerCompatRequest

/** Filled by [ConformanceMain] before Quarkus starts; read on [io.quarkus.runtime.StartupEvent]. */
object CompatState {
    @Volatile
    var port: Int = 0

    @Volatile
    var request: ServerCompatRequest? = null

    @Volatile
    var responseWritten: Boolean = false
}

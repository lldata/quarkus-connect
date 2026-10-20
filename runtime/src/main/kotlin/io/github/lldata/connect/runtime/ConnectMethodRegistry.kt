package io.github.lldata.connect.runtime

import com.google.protobuf.Message
import io.github.lldata.connect.runtime.grpc.GrpcServiceConnectBridge
import io.github.lldata.connect.spi.ConnectServerStreamMethod
import io.github.lldata.connect.spi.ConnectUnaryMethod
import jakarta.annotation.PostConstruct
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Instance
import jakarta.inject.Inject

/**
 * Indexes Connect unary and server-streaming procedures from CDI SPI beans and
 * bridged `@GrpcService` methods.
 */
@ApplicationScoped
class ConnectMethodRegistry {

    @Inject
    private lateinit var methods: Instance<ConnectUnaryMethod<*, *>>

    @Inject
    private lateinit var streamMethods: Instance<ConnectServerStreamMethod<*, *>>

    @Inject
    private lateinit var grpcBridge: GrpcServiceConnectBridge

    private lateinit var byProcedure: Map<String, ConnectUnaryMethod<out Message, out Message>>
    private lateinit var streamsByProcedure: Map<String, ConnectServerStreamMethod<out Message, out Message>>
    /** procedure → gRPC MethodType name (CLIENT_STREAMING / BIDI_STREAMING). */
    private lateinit var unsupportedByProcedure: Map<String, String>

    @PostConstruct
    fun init() {
        val unary = linkedMapOf<String, ConnectUnaryMethod<out Message, out Message>>()
        for (method in methods) {
            putUnary(unary, method)
        }
        for (method in grpcBridge.bridgedMethods()) {
            putUnary(unary, method)
        }
        byProcedure = unary.toMap()

        val streams = linkedMapOf<String, ConnectServerStreamMethod<out Message, out Message>>()
        for (method in streamMethods) {
            putStream(streams, method)
        }
        for (method in grpcBridge.bridgedStreamMethods()) {
            putStream(streams, method)
        }
        streamsByProcedure = streams.toMap()

        unsupportedByProcedure = grpcBridge.unsupportedConnectProcedures()
            .mapKeys { normalizeProcedure(it.key) }
    }

    fun find(procedure: String): ConnectUnaryMethod<out Message, out Message>? =
        byProcedure[normalizeProcedure(procedure)]

    fun findStream(procedure: String): ConnectServerStreamMethod<out Message, out Message>? =
        streamsByProcedure[normalizeProcedure(procedure)]

    /** Non-null when the procedure exists on a `@GrpcService` but is not Connect-bridged. */
    fun unsupportedType(procedure: String): String? =
        unsupportedByProcedure[normalizeProcedure(procedure)]

    fun procedures(): Set<String> = byProcedure.keys

    fun streamProcedures(): Set<String> = streamsByProcedure.keys

    private fun putUnary(
        map: MutableMap<String, ConnectUnaryMethod<out Message, out Message>>,
        method: ConnectUnaryMethod<*, *>,
    ) {
        val procedure = normalizeProcedure(method.procedure())
        require(procedure !in map) { "Duplicate Connect procedure registration: /$procedure" }
        @Suppress("UNCHECKED_CAST")
        map[procedure] = method as ConnectUnaryMethod<out Message, out Message>
    }

    private fun putStream(
        map: MutableMap<String, ConnectServerStreamMethod<out Message, out Message>>,
        method: ConnectServerStreamMethod<*, *>,
    ) {
        val procedure = normalizeProcedure(method.procedure())
        require(procedure !in map) { "Duplicate Connect stream procedure registration: /$procedure" }
        @Suppress("UNCHECKED_CAST")
        map[procedure] = method as ConnectServerStreamMethod<out Message, out Message>
    }

    companion object {
        @JvmStatic
        fun normalizeProcedure(procedure: String): String =
            procedure.trim().removePrefix("/")
    }
}

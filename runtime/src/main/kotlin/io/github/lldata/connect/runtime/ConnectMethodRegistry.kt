package io.github.lldata.connect.runtime

import com.google.protobuf.Message
import io.github.lldata.connect.runtime.grpc.GrpcServiceConnectBridge
import io.github.lldata.connect.spi.ConnectUnaryMethod
import jakarta.annotation.PostConstruct
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Instance
import jakarta.inject.Inject

/**
 * Indexes unary Connect procedures from:
 * 1. Explicit CDI [ConnectUnaryMethod] beans (legacy / low-level SPI)
 * 2. Bridged `@GrpcService` methods via [GrpcServiceConnectBridge]
 */
@ApplicationScoped
class ConnectMethodRegistry {

    @Inject
    private lateinit var methods: Instance<ConnectUnaryMethod<*, *>>

    @Inject
    private lateinit var grpcBridge: GrpcServiceConnectBridge

    private lateinit var byProcedure: Map<String, ConnectUnaryMethod<out Message, out Message>>

    @PostConstruct
    fun init() {
        val map = linkedMapOf<String, ConnectUnaryMethod<out Message, out Message>>()

        for (method in methods) {
            put(map, method)
        }
        for (method in grpcBridge.bridgedMethods()) {
            put(map, method)
        }

        byProcedure = map.toMap()
    }

    fun find(procedure: String): ConnectUnaryMethod<out Message, out Message>? =
        byProcedure[normalizeProcedure(procedure)]

    fun procedures(): Set<String> = byProcedure.keys

    private fun put(
        map: MutableMap<String, ConnectUnaryMethod<out Message, out Message>>,
        method: ConnectUnaryMethod<*, *>,
    ) {
        val procedure = normalizeProcedure(method.procedure())
        require(procedure !in map) {
            "Duplicate Connect procedure registration: /$procedure"
        }
        @Suppress("UNCHECKED_CAST")
        map[procedure] = method as ConnectUnaryMethod<out Message, out Message>
    }

    companion object {
        @JvmStatic
        fun normalizeProcedure(procedure: String): String =
            procedure.trim().removePrefix("/")
    }
}

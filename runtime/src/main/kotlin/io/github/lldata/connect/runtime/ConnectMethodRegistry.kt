package io.github.lldata.connect.runtime

import com.google.protobuf.Message
import io.github.lldata.connect.spi.ConnectUnaryMethod
import jakarta.annotation.PostConstruct
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Instance
import jakarta.inject.Inject

/**
 * Collects CDI beans that implement [ConnectUnaryMethod] and indexes them by procedure path.
 *
 * Uses field injection + [PostConstruct] (instead of a Kotlin primary constructor with
 * [Instance]) so Arc can resolve the bean reliably under both Maven and Gradle builds.
 */
@ApplicationScoped
class ConnectMethodRegistry {

    @Inject
    private lateinit var methods: Instance<ConnectUnaryMethod<*, *>>

    private lateinit var byProcedure: Map<String, ConnectUnaryMethod<out Message, out Message>>

    @PostConstruct
    fun init() {
        val map = linkedMapOf<String, ConnectUnaryMethod<out Message, out Message>>()
        for (method in methods) {
            val procedure = normalizeProcedure(method.procedure())
            require(procedure !in map) {
                "Duplicate Connect procedure registration: /$procedure"
            }
            @Suppress("UNCHECKED_CAST")
            map[procedure] = method as ConnectUnaryMethod<out Message, out Message>
        }
        byProcedure = map.toMap()
    }

    fun find(procedure: String): ConnectUnaryMethod<out Message, out Message>? =
        byProcedure[normalizeProcedure(procedure)]

    fun procedures(): Set<String> = byProcedure.keys

    companion object {
        @JvmStatic
        fun normalizeProcedure(procedure: String): String =
            procedure.trim().removePrefix("/")
    }
}

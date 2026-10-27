package io.github.lldata.connect.conformance

import connectrpc.conformance.v1.ServerCompatRequest
import connectrpc.conformance.v1.ServerCompatResponse
import io.quarkus.runtime.Quarkus
import io.quarkus.runtime.StartupEvent
import io.quarkus.runtime.annotations.QuarkusMain
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Observes
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * connectconformance server-under-test entry: read [ServerCompatRequest] from stdin,
 * start Quarkus on an ephemeral port, write [ServerCompatResponse] to stdout, wait for SIGTERM.
 *
 * Thin host: TLS is rejected (supports_tls=false). Message receive limits map to
 * `quarkus.connect.max-inbound-message-size` when the suite requests one.
 */
@QuarkusMain
object ConformanceMain {
    @JvmStatic
    fun main(args: Array<String>) {
        val request = LengthPrefixedIo.read(System.`in`, ServerCompatRequest.parser())
        if (request.useTls) {
            System.err.println("conformance host: TLS requested but supports_tls=false in features.yaml")
            kotlin.system.exitProcess(2)
        }

        val port = ServerSocket(0).use { it.localPort }
        System.setProperty("quarkus.http.port", port.toString())
        System.setProperty("quarkus.http.host", "127.0.0.1")
        System.setProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager")
        if (request.messageReceiveLimit != 0) {
            val limit = request.messageReceiveLimit.toLong()
            System.setProperty("quarkus.connect.max-inbound-message-size", limit.toString())
            // Native gRPC (PROTOCOL_GRPC) uses quarkus-grpc's inbound limit, not Connect's.
            System.setProperty("quarkus.grpc.server.max-inbound-message-size", limit.toString())
            // Unary bodies go through Vert.x BodyHandler; keep HTTP limit at least as large.
            System.setProperty("quarkus.http.limits.max-body-size", "${limit + 1024}B")
            System.err.println(
                "conformance host: message_receive_limit=$limit → " +
                    "quarkus.connect.max-inbound-message-size + quarkus.grpc.server.max-inbound-message-size",
            )
        }
        CompatState.port = port
        CompatState.request = request

        Quarkus.run(*args)
    }
}

@ApplicationScoped
class CompatResponseOnStartup {
    private val written = AtomicBoolean(false)
    private val writer =
        Executors.newSingleThreadExecutor { r ->
            Thread(r, "conformance-compat-response").apply { isDaemon = false }
        }

    /**
     * Schedule the ServerCompatResponse **after** StartupEvent returns so Quarkus can finish
     * bootstrapping. Writing to stdout mid-startup (piped by connectconformance) races the
     * remaining init and tears the process down before Timing.printStartupTime.
     */
    fun onStart(@Observes event: StartupEvent) {
        val port = CompatState.port
        writer.execute {
            try {
                awaitListen(port, timeoutMs = 15_000)
                if (!written.compareAndSet(false, true)) return@execute
                val response =
                    ServerCompatResponse.newBuilder()
                        .setHost("127.0.0.1")
                        .setPort(port)
                        .build()
                LengthPrefixedIo.write(System.out, response)
                CompatState.responseWritten = true
                System.err.println("conformance host listening on 127.0.0.1:$port")
            } catch (t: Throwable) {
                System.err.println("conformance host failed to publish ServerCompatResponse: $t")
                Quarkus.asyncExit(1)
            }
        }
    }

    private fun awaitListen(port: Int, timeoutMs: Long) {
        val deadline = System.currentTimeMillis() + timeoutMs
        var last: Exception? = null
        while (System.currentTimeMillis() < deadline) {
            try {
                Socket("127.0.0.1", port).use { /* accepted → HTTP port is up */ }
                return
            } catch (e: Exception) {
                last = e
                Thread.sleep(25)
            }
        }
        throw IllegalStateException("HTTP port $port not accepting connections within ${timeoutMs}ms", last)
    }
}

package io.github.lldata.connect.runtime.grpc

import io.github.lldata.connect.spi.ConnectCallContext
import io.github.lldata.connect.spi.ConnectCodec
import io.github.lldata.connect.spi.ConnectException
import io.grpc.ForwardingServerCallListener
import io.grpc.Metadata
import io.grpc.MethodDescriptor
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor
import io.grpc.ServerMethodDefinition
import io.grpc.Status
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets.UTF_8
import java.util.concurrent.atomic.AtomicInteger

/**
 * Quarkus / Micrometer record `grpc.server.processing.duration` in the interceptor
 * listener's [ServerCall.Listener.onComplete] / [ServerCall.Listener.onCancel].
 * Regression for #50: Connect unary must fire those after [ServerCall.close].
 */
class GrpcUnaryInvokerOnCompleteTest {

    @Test
    fun `calls listener onComplete after successful unary close`() {
        val completed = AtomicInteger(0)
        val cancelled = AtomicInteger(0)
        val tracking = trackingInterceptor(completed, cancelled)

        val response = GrpcUnaryInvoker.invoke(
            unaryEchoMethod(),
            listOf(tracking),
            "ping",
            testContext(),
        )

        assertEquals("ping", response)
        assertEquals(1, completed.get(), "onComplete must run so processing.duration is recorded")
        assertEquals(0, cancelled.get())
    }

    @Test
    fun `calls listener onComplete after error status close`() {
        val completed = AtomicInteger(0)
        val cancelled = AtomicInteger(0)
        val tracking = trackingInterceptor(completed, cancelled)

        val ex = assertThrows<ConnectException> {
            GrpcUnaryInvoker.invoke(
                unaryFailMethod(Status.NOT_FOUND.withDescription("missing")),
                listOf(tracking),
                "ping",
                testContext(),
            )
        }

        assertEquals(io.github.lldata.connect.spi.ConnectCode.NOT_FOUND, ex.code)
        assertEquals(1, completed.get(), "error close still completes the call for metrics")
        assertEquals(0, cancelled.get())
    }

    @Test
    fun `calls listener onCancel when unary times out`() {
        val completed = AtomicInteger(0)
        val cancelled = AtomicInteger(0)
        val tracking = trackingInterceptor(completed, cancelled)

        val ex = assertThrows<ConnectException> {
            GrpcUnaryInvoker.invoke(
                unaryHangMethod(),
                listOf(tracking),
                "ping",
                testContext(timeoutMs = 50L),
            )
        }

        assertEquals(io.github.lldata.connect.spi.ConnectCode.DEADLINE_EXCEEDED, ex.code)
        assertEquals(1, cancelled.get(), "timeout must onCancel so processing.duration is recorded")
        assertEquals(0, completed.get())
    }

    private fun trackingInterceptor(
        completed: AtomicInteger,
        cancelled: AtomicInteger,
    ): ServerInterceptor =
        object : ServerInterceptor {
            override fun <ReqT, RespT> interceptCall(
                call: ServerCall<ReqT, RespT>,
                headers: Metadata,
                next: ServerCallHandler<ReqT, RespT>,
            ): ServerCall.Listener<ReqT> {
                val delegate = next.startCall(call, headers)
                return object : ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT>(delegate) {
                    override fun onComplete() {
                        completed.incrementAndGet()
                        super.onComplete()
                    }

                    override fun onCancel() {
                        cancelled.incrementAndGet()
                        super.onCancel()
                    }
                }
            }
        }

    private fun unaryEchoMethod(): ServerMethodDefinition<String, String> {
        val descriptor = stringMethod("test.Echo/Unary")
        val handler = ServerCallHandler<String, String> { call, _ ->
            object : ServerCall.Listener<String>() {
                private var request: String? = null

                override fun onMessage(message: String) {
                    request = message
                }

                override fun onHalfClose() {
                    call.sendHeaders(Metadata())
                    call.sendMessage(request ?: "")
                    call.close(Status.OK, Metadata())
                }
            }
        }
        return ServerMethodDefinition.create(descriptor, handler)
    }

    private fun unaryFailMethod(status: Status): ServerMethodDefinition<String, String> {
        val descriptor = stringMethod("test.Echo/Fail")
        val handler = ServerCallHandler<String, String> { call, _ ->
            object : ServerCall.Listener<String>() {
                override fun onHalfClose() {
                    call.close(status, Metadata())
                }
            }
        }
        return ServerMethodDefinition.create(descriptor, handler)
    }

    private fun unaryHangMethod(): ServerMethodDefinition<String, String> {
        val descriptor = stringMethod("test.Echo/Hang")
        // Never closes — invoker deadline should fire.
        val handler = ServerCallHandler<String, String> { _, _ ->
            object : ServerCall.Listener<String>() {}
        }
        return ServerMethodDefinition.create(descriptor, handler)
    }

    private fun stringMethod(fullMethodName: String): MethodDescriptor<String, String> =
        MethodDescriptor.newBuilder<String, String>()
            .setType(MethodDescriptor.MethodType.UNARY)
            .setFullMethodName(fullMethodName)
            .setRequestMarshaller(STRING_MARSHALLER)
            .setResponseMarshaller(STRING_MARSHALLER)
            .build()

    private fun testContext(timeoutMs: Long? = 5_000L): ConnectCallContext =
        object : ConnectCallContext {
            override val procedure: String = "test.Echo/Unary"
            override val codec: ConnectCodec = ConnectCodec.PROTO
            override val requestHeaders: Map<String, List<String>> = emptyMap()
            override val timeoutMs: Long? = timeoutMs
            override fun responseHeader(name: String, value: String) = Unit
            override fun responseTrailer(name: String, value: String) = Unit
        }

    companion object {
        private val STRING_MARSHALLER = object : MethodDescriptor.Marshaller<String> {
            override fun stream(value: String): InputStream =
                ByteArrayInputStream(value.toByteArray(UTF_8))

            override fun parse(stream: InputStream): String =
                stream.readBytes().toString(UTF_8)
        }
    }
}

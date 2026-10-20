package io.github.lldata.connect.examples.otel

import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor
import io.grpc.Status
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.StatusCode
import io.quarkus.grpc.GlobalInterceptor
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject

/**
 * One OpenTelemetry span per RPC: procedure name + status. No payloads.
 * Applies to gRPC, Connect, and gRPC-Web via the quarkus-connect bridge.
 */
@ApplicationScoped
@GlobalInterceptor
class RpcTracingInterceptor : ServerInterceptor {

    @Inject
    lateinit var openTelemetry: OpenTelemetry

    override fun <ReqT, RespT> interceptCall(
        call: ServerCall<ReqT, RespT>,
        headers: Metadata,
        next: ServerCallHandler<ReqT, RespT>,
    ): ServerCall.Listener<ReqT> {
        val procedure = call.methodDescriptor.fullMethodName
        val tracer = openTelemetry.getTracer("io.github.lldata.connect")
        val span = tracer.spanBuilder(procedure)
            .setSpanKind(SpanKind.SERVER)
            .setAttribute("rpc.system", "connect")
            .setAttribute("rpc.service", call.methodDescriptor.serviceName ?: "")
            .setAttribute("rpc.method", call.methodDescriptor.bareMethodName ?: "")
            .startSpan()

        val scoped = span.makeCurrent()
        val forwarding = object : io.grpc.ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT>(call) {
            override fun close(status: Status, trailers: Metadata) {
                span.setAttribute("rpc.grpc.status_code", status.code.value().toLong())
                span.setAttribute("rpc.connect.status", status.code.name.lowercase())
                if (status.isOk) {
                    span.setStatus(StatusCode.OK)
                } else {
                    span.setStatus(StatusCode.ERROR, status.description ?: status.code.name)
                }
                try {
                    super.close(status, trailers)
                } finally {
                    span.end()
                    scoped.close()
                }
            }
        }

        return try {
            next.startCall(forwarding, headers)
        } catch (e: Exception) {
            span.setStatus(StatusCode.ERROR, e.message ?: "error")
            span.recordException(e)
            span.end()
            scoped.close()
            throw e
        }
    }
}

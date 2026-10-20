package io.github.lldata.connect.it

import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor
import io.grpc.Status
import io.quarkus.grpc.GlobalInterceptor
import jakarta.enterprise.context.ApplicationScoped

/**
 * Global gRPC interceptor — must also run for Connect / gRPC-Web via the bridge.
 * Missing/invalid Bearer token → UNAUTHENTICATED (except health-style Fail mapping tests use the Fail RPC
 * which still requires auth; greets without token fail).
 */
@ApplicationScoped
@GlobalInterceptor
class BearerAuthInterceptor : ServerInterceptor {

    override fun <ReqT, RespT> interceptCall(
        call: ServerCall<ReqT, RespT>,
        headers: Metadata,
        next: ServerCallHandler<ReqT, RespT>,
    ): ServerCall.Listener<ReqT> {
        val auth = headers.get(AUTHORIZATION) ?: ""
        if (!auth.startsWith("Bearer ") || auth.removePrefix("Bearer ").isBlank()) {
            call.close(Status.UNAUTHENTICATED.withDescription("Bearer token required"), Metadata())
            return object : ServerCall.Listener<ReqT>() {}
        }
        val token = auth.removePrefix("Bearer ").trim()
        if (token != VALID_TOKEN) {
            call.close(Status.UNAUTHENTICATED.withDescription("invalid token"), Metadata())
            return object : ServerCall.Listener<ReqT>() {}
        }
        return next.startCall(call, headers)
    }

    companion object {
        const val VALID_TOKEN = "test-token"
        val AUTHORIZATION: Metadata.Key<String> =
            Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER)
    }
}

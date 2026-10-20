package io.github.lldata.connect.examples

import io.grpc.Context
import io.grpc.Contexts
import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor
import io.grpc.Status
import io.quarkus.grpc.GlobalInterceptor
import jakarta.enterprise.context.ApplicationScoped

/**
 * Bearer auth as a normal gRPC [ServerInterceptor].
 * quarkus-connect applies the same interceptors to Connect and gRPC-Web.
 *
 * Tokens:
 * - `demo-user` → subject `user`
 * - `demo-guest` → subject `guest` (Echo OK, WhoAmI → PERMISSION_DENIED)
 */
@ApplicationScoped
@GlobalInterceptor
class DemoAuthInterceptor : ServerInterceptor {

    override fun <ReqT, RespT> interceptCall(
        call: ServerCall<ReqT, RespT>,
        headers: Metadata,
        next: ServerCallHandler<ReqT, RespT>,
    ): ServerCall.Listener<ReqT> {
        val auth = headers.get(AUTHORIZATION).orEmpty()
        if (!auth.startsWith("Bearer ")) {
            call.close(Status.UNAUTHENTICATED.withDescription("Bearer token required"), Metadata())
            return object : ServerCall.Listener<ReqT>() {}
        }
        val subject = when (auth.removePrefix("Bearer ").trim()) {
            "demo-user" -> "user"
            "demo-guest" -> "guest"
            else -> {
                call.close(Status.UNAUTHENTICATED.withDescription("invalid token"), Metadata())
                return object : ServerCall.Listener<ReqT>() {}
            }
        }
        val ctx = Context.current().withValue(SUBJECT_KEY, subject)
        return Contexts.interceptCall(ctx, call, headers, next)
    }

    companion object {
        val AUTHORIZATION: Metadata.Key<String> =
            Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER)

        private val SUBJECT_KEY: Context.Key<String> = Context.key("demo-subject")

        fun currentSubject(): String? = SUBJECT_KEY.get()
    }
}

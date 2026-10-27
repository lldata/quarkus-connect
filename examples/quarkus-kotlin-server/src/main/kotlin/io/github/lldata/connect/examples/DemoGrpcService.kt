package io.github.lldata.connect.examples

import io.github.lldata.connect.examples.demo.v1.EchoRequest
import io.github.lldata.connect.examples.demo.v1.EchoResponse
import io.github.lldata.connect.examples.demo.v1.MutinyDemoServiceGrpc
import io.github.lldata.connect.examples.demo.v1.WhoAmIRequest
import io.github.lldata.connect.examples.demo.v1.WhoAmIResponse
import io.grpc.Metadata
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.quarkus.grpc.GrpcService
import io.smallrye.mutiny.Uni

/**
 * Ordinary `@GrpcService` — no Connect-specific handlers.
 * quarkus-connect serves this as gRPC + Connect (JSON/binary) + gRPC-Web on port 8080.
 */
@GrpcService
class DemoGrpcService : MutinyDemoServiceGrpc.DemoServiceImplBase() {

    override fun echo(request: EchoRequest): Uni<EchoResponse> {
        val message = request.message.trim()
        if (message.isEmpty()) {
            return Uni.createFrom().failure(
                StatusRuntimeException(Status.INVALID_ARGUMENT.withDescription("message must not be empty")),
            )
        }
        if (message.equals("boom", ignoreCase = true)) {
            return Uni.createFrom().failure(
                StatusRuntimeException(Status.FAILED_PRECONDITION.withDescription("refusing message 'boom'")),
            )
        }
        return Uni.createFrom().item(
            EchoResponse.newBuilder()
                .setMessage(message)
                .setClientSeq(request.clientSeq)
                .setServerTimeUnixMs(System.currentTimeMillis())
                .build(),
        )
    }

    override fun whoAmI(request: WhoAmIRequest): Uni<WhoAmIResponse> {
        val subject = DemoAuthInterceptor.currentSubject()
            ?: return Uni.createFrom().failure(
                StatusRuntimeException(Status.UNAUTHENTICATED.withDescription("no subject")),
            )
        if (subject == "guest") {
            // Demo trailer for Flutter connectrpc clients: Connect unary surfaces this as
            // HTTP header trailer-retry-after-seconds; clients read it from ConnectException.metadata.
            val trailers = Metadata()
            trailers.put(RETRY_AFTER_SECONDS, "30")
            return Uni.createFrom().failure(
                StatusRuntimeException(
                    Status.PERMISSION_DENIED.withDescription("guest cannot call WhoAmI"),
                    trailers,
                ),
            )
        }
        return Uni.createFrom().item(WhoAmIResponse.newBuilder().setSubject(subject).build())
    }

    companion object {
        val RETRY_AFTER_SECONDS: Metadata.Key<String> =
            Metadata.Key.of("retry-after-seconds", Metadata.ASCII_STRING_MARSHALLER)
    }
}

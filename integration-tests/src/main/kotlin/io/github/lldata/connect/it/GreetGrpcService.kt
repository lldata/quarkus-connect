package io.github.lldata.connect.it

import io.github.lldata.connect.it.greet.v1.FailRequest
import io.github.lldata.connect.it.greet.v1.FailResponse
import io.github.lldata.connect.it.greet.v1.GreetRequest
import io.github.lldata.connect.it.greet.v1.GreetResponse
import io.github.lldata.connect.it.greet.v1.MutinyGreetServiceGrpc
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.quarkus.grpc.GrpcService
import io.smallrye.mutiny.Uni

/**
 * Ordinary Quarkus `@GrpcService` — served as gRPC + Connect + gRPC-Web on one HTTP port.
 * No hand-written Connect handlers.
 */
@GrpcService
class GreetGrpcService : MutinyGreetServiceGrpc.GreetServiceImplBase() {

    override fun greet(request: GreetRequest): Uni<GreetResponse> {
        val name = request.name.trim()
        if (name.isEmpty()) {
            return Uni.createFrom().failure(
                StatusRuntimeException(Status.INVALID_ARGUMENT.withDescription("name must not be empty")),
            )
        }
        if (name.equals("forbidden", ignoreCase = true)) {
            return Uni.createFrom().failure(
                StatusRuntimeException(Status.PERMISSION_DENIED.withDescription("name is not allowed")),
            )
        }
        return Uni.createFrom().item(
            GreetResponse.newBuilder()
                .setGreeting("Hello, $name!")
                .setRequestId(request.requestId)
                .build(),
        )
    }

    override fun fail(request: FailRequest): Uni<FailResponse> {
        val status = statusFor(request.code).withDescription(request.message.ifBlank { request.code })
        return Uni.createFrom().failure(StatusRuntimeException(status))
    }

    private fun statusFor(code: String): Status = when (code.trim().lowercase()) {
        "invalid_argument" -> Status.INVALID_ARGUMENT
        "not_found" -> Status.NOT_FOUND
        "already_exists" -> Status.ALREADY_EXISTS
        "failed_precondition" -> Status.FAILED_PRECONDITION
        "resource_exhausted" -> Status.RESOURCE_EXHAUSTED
        "unimplemented" -> Status.UNIMPLEMENTED
        "permission_denied" -> Status.PERMISSION_DENIED
        "unauthenticated" -> Status.UNAUTHENTICATED
        else -> Status.UNKNOWN
    }
}

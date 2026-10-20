package io.github.lldata.connect.it

import com.google.protobuf.Any
import com.google.rpc.ErrorInfo
import com.google.rpc.Status as RpcStatus
import io.github.lldata.connect.it.greet.v1.FailRequest
import io.github.lldata.connect.it.greet.v1.FailResponse
import io.github.lldata.connect.it.greet.v1.GreetRequest
import io.github.lldata.connect.it.greet.v1.GreetResponse
import io.github.lldata.connect.it.greet.v1.MutinyGreetServiceGrpc
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.grpc.protobuf.StatusProto
import io.quarkus.grpc.GrpcService
import io.smallrye.mutiny.Multi
import io.smallrye.mutiny.Uni
import java.time.Duration

/**
 * Ordinary Quarkus `@GrpcService` — served as gRPC + Connect + gRPC-Web on one HTTP port.
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

    override fun failWithDetails(request: FailRequest): Uni<FailResponse> {
        val info = ErrorInfo.newBuilder()
            .setReason("DEMO_FAIL")
            .setDomain("quarkus-connect.it")
            .putMetadata("code", request.code)
            .build()
        val rpcStatus = RpcStatus.newBuilder()
            .setCode(Status.Code.INVALID_ARGUMENT.value())
            .setMessage(request.message.ifBlank { "fail with details" })
            .addDetails(Any.pack(info))
            .build()
        return Uni.createFrom().failure(StatusProto.toStatusRuntimeException(rpcStatus))
    }

    override fun slow(request: GreetRequest): Uni<GreetResponse> =
        // Delay on a worker so Connect deadline/latch can fire (do not block the call thread).
        Uni.createFrom().item(
            GreetResponse.newBuilder()
                .setGreeting("Hello, ${request.name}!")
                .setRequestId(request.requestId)
                .build(),
        ).onItem().delayIt().by(Duration.ofSeconds(5))

    override fun streamGreetings(request: GreetRequest): Multi<GreetResponse> {
        val name = request.name.trim().ifEmpty { "friend" }
        return Multi.createFrom().range(1, 4)
            .onItem().transform { i ->
                GreetResponse.newBuilder()
                    .setGreeting("Hello #$i, $name!")
                    .setRequestId(request.requestId)
                    .build()
            }
            .onItem().call { _ -> Uni.createFrom().voidItem().onItem().delayIt().by(Duration.ofMillis(20)) }
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

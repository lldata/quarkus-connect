package io.github.lldata.connect.it.secure

import io.github.lldata.connect.it.secure.v1.MutinySecureServiceGrpc
import io.github.lldata.connect.it.secure.v1.WhoAmIRequest
import io.github.lldata.connect.it.secure.v1.WhoAmIResponse
import io.quarkus.grpc.GrpcService
import io.quarkus.security.identity.SecurityIdentity
import io.smallrye.mutiny.Uni
import jakarta.annotation.security.RolesAllowed
import jakarta.inject.Inject

/**
 * Exercises Quarkus Security annotations on Connect / gRPC-Web via the Connect bridge.
 */
@GrpcService
class SecureGrpcService : MutinySecureServiceGrpc.SecureServiceImplBase() {

    @Inject
    lateinit var identity: SecurityIdentity

    @RolesAllowed("user", "admin")
    override fun whoAmI(request: WhoAmIRequest): Uni<WhoAmIResponse> =
        Uni.createFrom().item(
            WhoAmIResponse.newBuilder()
                .setSubject(identity.principal.name)
                .build(),
        )

    @RolesAllowed("admin")
    override fun adminOnly(request: WhoAmIRequest): Uni<WhoAmIResponse> =
        Uni.createFrom().item(
            WhoAmIResponse.newBuilder()
                .setSubject(identity.principal.name)
                .build(),
        )
}

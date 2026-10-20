package io.github.lldata.connect.runtime.security

import io.github.lldata.connect.spi.ConnectCode
import io.github.lldata.connect.spi.ConnectException
import io.quarkus.arc.Arc
import io.quarkus.grpc.auth.GrpcSecurityInterceptor
import io.vertx.ext.web.RoutingContext
import org.jboss.logging.Logger

/**
 * Opt-in Quarkus Security hooks for the Connect / gRPC-Web HTTP path.
 *
 * Does **not** add OIDC/JWT dependencies. When the app enables Quarkus Security
 * (`Capability.SECURITY` / `quarkus-security` + mechanisms), quarkus-grpc registers
 * [GrpcSecurityInterceptor] as a `@GlobalInterceptor`; the Connect bridge already
 * runs those interceptors. This helper only:
 *
 * 1. Propagates [io.quarkus.security.identity.SecurityIdentity] from the Vert.x
 *    [RoutingContext] onto the duplicated gRPC/Vert.x context (same as native gRPC).
 * 2. Activates the CDI request context on worker threads (needed for
 *    `CurrentIdentityAssociation` / `@RolesAllowed`).
 * 3. Maps Quarkus security failures to Connect codes (aligned with
 *    `DefaultAuthExceptionHandlerProvider`).
 */
object ConnectSecuritySupport {

    private val log = Logger.getLogger(ConnectSecuritySupport::class.java)

    private val authenticationException: Class<*>? =
        try {
            Class.forName("io.quarkus.security.AuthenticationException")
        } catch (_: ClassNotFoundException) {
            null
        }

    /**
     * Call on the event-loop before bridging to `@GrpcService` so HTTP-established
     * identities (basic, OIDC bearer, mTLS, …) are visible to gRPC security / `@RolesAllowed`.
     * Safe no-op when Security is absent or the request has no user.
     */
    fun propagateIdentity(ctx: RoutingContext) {
        try {
            GrpcSecurityInterceptor.propagateSecurityIdentityWithDuplicatedCtx(ctx)
        } catch (e: Throwable) {
            // Should not happen in normal builds; avoid failing open requests if SPI drifts.
            log.debugf(e, "Connect security identity propagation skipped")
        }
    }

    /**
     * Run [block] with an active CDI request context when one is not already active.
     * Required because Connect bridges via `executeBlocking` while
     * `GrpcSecurityInterceptor` touches request-scoped `CurrentIdentityAssociation`.
     */
    fun <T> withRequestContext(block: () -> T): T {
        val container = Arc.container() ?: return block()
        val requestContext = container.requestContext()
        if (requestContext.isActive) {
            return block()
        }
        requestContext.activate()
        return try {
            block()
        } finally {
            requestContext.terminate()
        }
    }

    /**
     * Maps Quarkus Security throwables to Connect codes.
     * Returns null when [error] is not a security failure we recognize.
     */
    fun toConnectException(error: Throwable): ConnectException? {
        val root = unwrap(error)
        val authType = authenticationException
        if (authType != null && authType.isInstance(root)) {
            return ConnectException(
                ConnectCode.UNAUTHENTICATED,
                root.message ?: "Authentication failed",
                root,
            )
        }
        // Match DefaultAuthExceptionHandlerProvider: SecurityException → PERMISSION_DENIED.
        // Limit to Quarkus security types so unrelated SecurityExceptions stay INTERNAL.
        if (root is SecurityException && root.javaClass.name.startsWith("io.quarkus.security.")) {
            return ConnectException(
                ConnectCode.PERMISSION_DENIED,
                root.message ?: "Permission denied",
                root,
            )
        }
        return null
    }

    private fun unwrap(error: Throwable): Throwable {
        var current = error
        var depth = 0
        while (depth < 8) {
            if (authenticationException?.isInstance(current) == true) return current
            if (current is SecurityException &&
                current.javaClass.name.startsWith("io.quarkus.security.")
            ) {
                return current
            }
            val cause = current.cause ?: break
            if (cause === current) break
            current = cause
            depth++
        }
        return current
    }
}

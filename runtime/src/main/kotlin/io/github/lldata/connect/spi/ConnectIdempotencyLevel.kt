package io.github.lldata.connect.spi

/**
 * Declares how idempotent a unary RPC is. Values match
 * `google.protobuf.MethodOptions.IdempotencyLevel`.
 *
 * Connect unary GET is allowed only for [NO_SIDE_EFFECTS] (same rule as connect-go).
 */
enum class ConnectIdempotencyLevel {
    /** Default: may have side effects; Connect GET must not be served. */
    UNKNOWN,

    /**
     * Safe / no side effects (RFC 9110 §9.2.1). Suitable for HTTP GET and for
     * retry without altering results.
     */
    NO_SIDE_EFFECTS,

    /**
     * Idempotent but may have side effects (RFC 9110 §9.2.2). Retries are safe;
     * Connect GET is still not allowed.
     */
    IDEMPOTENT,
}

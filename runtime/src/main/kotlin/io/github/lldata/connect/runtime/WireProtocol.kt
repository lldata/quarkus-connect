package io.github.lldata.connect.runtime

/**
 * Wire protocol selected from the request Content-Type.
 */
enum class WireProtocol {
    /** Connect unary (`application/proto` or `application/json`). */
    CONNECT,

    /** Connect server streaming (`application/connect+proto` or `+json`). */
    CONNECT_STREAM,

    /** gRPC-Web unary (`application/grpc-web`, `application/grpc-web+proto`, `+json`). */
    GRPC_WEB,

    /** Native gRPC (`application/grpc`) — handled by quarkus-grpc on the shared HTTP port. */
    GRPC_NATIVE,
}

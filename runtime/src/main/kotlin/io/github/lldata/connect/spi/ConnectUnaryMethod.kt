package io.github.lldata.connect.spi

import com.google.protobuf.Message

/**
 * Java-friendly SPI for a unary Connect method backed by protobuf messages.
 *
 * Kotlin services can implement this directly or via [ConnectUnaryHandler].
 * A future Java programming model can bind annotations/codegen onto the same SPI
 * without changing the protocol layer.
 */
interface ConnectUnaryMethod<Req : Message, Res : Message> {
    /** Fully-qualified procedure path without leading slash, e.g. `pkg.v1.Service/Method`. */
    fun procedure(): String

    fun requestType(): Class<Req>

    fun responseType(): Class<Res>

    /**
     * Idempotency declared for this procedure. Default [ConnectIdempotencyLevel.UNKNOWN].
     *
     * Connect serves unary GET only when this is [ConnectIdempotencyLevel.NO_SIDE_EFFECTS]
     * (protocol parity with connect-go). Bridged `@GrpcService` methods read the value from
     * protobuf `option idempotency_level` via gRPC [io.grpc.MethodDescriptor.isSafe].
     */
    fun idempotencyLevel(): ConnectIdempotencyLevel = ConnectIdempotencyLevel.UNKNOWN

    @Throws(ConnectException::class)
    fun handle(request: Req, context: ConnectCallContext): Res
}

/**
 * Kotlin-idiomatic alias for [ConnectUnaryMethod].
 */
typealias ConnectUnaryHandler<Req, Res> = ConnectUnaryMethod<Req, Res>

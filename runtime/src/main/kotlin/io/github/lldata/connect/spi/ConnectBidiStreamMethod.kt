package io.github.lldata.connect.spi

import com.google.protobuf.Message

/**
 * Java-friendly SPI for a Connect **bidirectional-streaming** method.
 *
 * HTTP envelopes are parsed as they arrive. CDI SPI beans still receive the
 * collected request list in [handle]; `@GrpcService` bridges feed `onMessage`
 * incrementally. [response] receives zero or more outbound messages (same
 * observer contract as server streaming).
 */
interface ConnectBidiStreamMethod<Req : Message, Res : Message> {
    fun procedure(): String

    fun requestType(): Class<Req>

    fun responseType(): Class<Res>

    /**
     * Handle a bidi call. Implementations must eventually call
     * [ConnectStreamObserver.onCompleted] or [ConnectStreamObserver.onError].
     */
    fun handle(requests: List<Req>, context: ConnectCallContext, response: ConnectStreamObserver<Res>)
}

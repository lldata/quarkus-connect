package io.github.lldata.connect.spi

import com.google.protobuf.Message

/**
 * Java-friendly SPI for a Connect **client-streaming** method.
 * The handler receives zero or more request messages and returns a single response.
 */
interface ConnectClientStreamMethod<Req : Message, Res : Message> {
    fun procedure(): String

    fun requestType(): Class<Req>

    fun responseType(): Class<Res>

    /**
     * Handle a client-streaming call. [requests] is the full sequence of messages
     * from the Connect request body (already decoded).
     */
    fun handle(requests: List<Req>, context: ConnectCallContext): Res
}

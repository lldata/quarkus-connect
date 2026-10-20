package io.github.lldata.connect.spi

import com.google.protobuf.Message

/**
 * Java-friendly SPI for a Connect **server-streaming** method.
 * The handler pushes zero or more responses then completes (or fails).
 */
interface ConnectServerStreamMethod<Req : Message, Res : Message> {
    fun procedure(): String

    fun requestType(): Class<Req>

    fun responseType(): Class<Res>

    /**
     * Handle a server-streaming call. Implementations must eventually call
     * [ConnectStreamObserver.onCompleted] or [ConnectStreamObserver.onError].
     */
    fun handle(request: Req, context: ConnectCallContext, response: ConnectStreamObserver<Res>)
}

/**
 * Minimal stream observer for Connect server streaming.
 */
interface ConnectStreamObserver<T> {
    fun onNext(value: T)
    fun onError(error: ConnectException)
    fun onCompleted()
}

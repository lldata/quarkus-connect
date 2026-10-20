package io.github.lldata.connect.runtime

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * Coordinates HTTP client disconnect with in-flight gRPC stream calls.
 * The Vert.x handler calls [cancelFromClient]; the gRPC invoker attaches
 * a cancel callback that tears down the gRPC [io.grpc.Context].
 */
class StreamCallControl {
    private val cancelled = AtomicBoolean(false)
    private val grpcCancel = AtomicReference<(() -> Unit)?>(null)

    fun attachGrpcCancel(cancel: () -> Unit) {
        grpcCancel.set(cancel)
        if (cancelled.get()) {
            cancel()
        }
    }

    fun cancelFromClient() {
        if (cancelled.compareAndSet(false, true)) {
            grpcCancel.get()?.invoke()
        }
    }

    fun isCancelled(): Boolean = cancelled.get()
}

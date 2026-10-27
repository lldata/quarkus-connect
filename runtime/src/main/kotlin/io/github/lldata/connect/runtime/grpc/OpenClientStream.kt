package io.github.lldata.connect.runtime.grpc

import io.github.lldata.connect.runtime.security.ConnectSecuritySupport
import io.github.lldata.connect.spi.ConnectCallContext
import io.github.lldata.connect.spi.ConnectCode
import io.github.lldata.connect.spi.ConnectException
import io.grpc.Context
import io.grpc.Metadata
import io.grpc.MethodDescriptor
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.Status
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * Open client-streaming gRPC call. Construct on any thread, then call [runAndAwait]
 * from a Vert.x worker (`executeBlocking`) so interceptors see a Vert.x context.
 * Feed [trySend] from the event loop as envelopes arrive; [halfClose] when the HTTP
 * request ends.
 */
class OpenClientStream<ReqT, RespT> internal constructor(
    private val descriptor: MethodDescriptor<ReqT, RespT>,
    private val handler: ServerCallHandler<ReqT, RespT>,
    private val context: ConnectCallContext,
    queueCapacity: Int = DEFAULT_QUEUE_CAPACITY,
    private val onWritable: (() -> Unit)? = null,
) {
    private val responseRef = AtomicReference<RespT>()
    private val errorRef = AtomicReference<Throwable>()
    private val latch = CountDownLatch(1)
    private val closed = AtomicBoolean(false)
    private val inbound = ArrayBlockingQueue<Any>(queueCapacity)
    private val queued = AtomicInteger(0)
    private val resumeAt = (queueCapacity / 2).coerceAtLeast(1)
    private val timeoutMs = context.timeoutMs ?: 60_000L

    fun trySend(message: ReqT): Boolean {
        if (closed.get()) return true
        queued.incrementAndGet()
        val ok = inbound.offer(message)
        if (!ok) {
            queued.decrementAndGet()
        }
        return ok
    }

    fun send(message: ReqT) {
        if (closed.get()) return
        queued.incrementAndGet()
        inbound.put(message)
    }

    fun halfClose() {
        if (closed.compareAndSet(false, true)) {
            queued.incrementAndGet()
            inbound.put(HalfClose)
        }
    }

    fun cancel() {
        if (closed.compareAndSet(false, true)) {
            queued.incrementAndGet()
            if (!inbound.offer(CancelSignal)) {
                inbound.put(CancelSignal)
            }
        }
    }

    /**
     * Runs the gRPC call on the current thread (must be a Vert.x worker) until the
     * response is ready. Call from `vertx.executeBlocking`.
     */
    fun runAndAwait(): RespT = ConnectSecuritySupport.withRequestContext {
        val deadlineCtx =
            Context.current().withDeadlineAfter(timeoutMs, TimeUnit.MILLISECONDS, DEADLINE_SCHEDULER)
        val previous = deadlineCtx.attach()
        try {
            val headers = toMetadata(context.requestHeaders)
            val call = object : ServerCall<ReqT, RespT>() {
                private var callClosed = false

                override fun request(numMessages: Int) = Unit

                override fun sendHeaders(headers: Metadata) {
                    GrpcMetadataCopy.headersToContext(headers, context)
                }

                override fun sendMessage(message: RespT) {
                    responseRef.set(message)
                }

                override fun close(status: Status, trailers: Metadata) {
                    if (callClosed) return
                    callClosed = true
                    GrpcMetadataCopy.trailersToContext(trailers, context)
                    if (!status.isOk) {
                        errorRef.set(status.asRuntimeException(trailers))
                    }
                    latch.countDown()
                }

                override fun isCancelled(): Boolean = deadlineCtx.isCancelled

                override fun getMethodDescriptor(): MethodDescriptor<ReqT, RespT> = descriptor
            }

            val listener = handler.startCall(call, headers)
            listener.onReady()
            while (true) {
                val item = inbound.take()
                val left = queued.decrementAndGet()
                if (left == resumeAt) {
                    onWritable?.invoke()
                }
                when (item) {
                    HalfClose -> {
                        listener.onHalfClose()
                        break
                    }
                    is CancelSignal -> {
                        listener.onCancel()
                        break
                    }
                    else -> {
                        @Suppress("UNCHECKED_CAST")
                        listener.onMessage(item as ReqT)
                    }
                }
            }

            if (!latch.await(timeoutMs, TimeUnit.MILLISECONDS)) {
                throw ConnectException(ConnectCode.DEADLINE_EXCEEDED, "RPC timed out after ${timeoutMs}ms")
            }
            errorRef.get()?.let { throw GrpcStatusMapping.toConnectException(it) }
            responseRef.get()
                ?: throw ConnectException(
                    ConnectCode.INTERNAL,
                    "gRPC client-stream handler completed without a response message",
                )
        } finally {
            deadlineCtx.detach(previous)
            deadlineCtx.cancel(null)
        }
    }

    private object HalfClose
    private object CancelSignal

    companion object {
        const val DEFAULT_QUEUE_CAPACITY = 16

        private val DEADLINE_SCHEDULER = java.util.concurrent.Executors.newSingleThreadScheduledExecutor { r ->
            Thread(r, "connect-client-stream-deadline").apply { isDaemon = true }
        }

        private fun toMetadata(headers: Map<String, List<String>>): Metadata {
            val metadata = Metadata()
            for ((name, values) in headers) {
                val lower = name.lowercase()
                if (lower in HOP_BY_HOP || lower.startsWith(":")) continue
                if (lower.endsWith(Metadata.BINARY_HEADER_SUFFIX)) continue
                val key = Metadata.Key.of(lower, Metadata.ASCII_STRING_MARSHALLER)
                for (value in values) {
                    metadata.put(key, value)
                }
            }
            return metadata
        }

        private val HOP_BY_HOP = setOf(
            "connection", "keep-alive", "proxy-authenticate", "proxy-authorization",
            "te", "trailer", "transfer-encoding", "upgrade", "host", "content-length",
            "content-type", "accept", "accept-encoding", "content-encoding",
            "connect-protocol-version", "connect-content-encoding",
            "connect-accept-encoding",
        )
    }
}

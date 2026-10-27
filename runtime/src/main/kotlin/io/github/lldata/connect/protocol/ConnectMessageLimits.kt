package io.github.lldata.connect.protocol

import io.github.lldata.connect.spi.ConnectCode
import io.github.lldata.connect.spi.ConnectException

/**
 * Shared inbound message-size checks for Connect and gRPC-Web.
 */
object ConnectMessageLimits {

    const val DEFAULT_MAX_INBOUND_MESSAGE_SIZE: Long = 4L * 1024 * 1024

    fun requireWithinLimit(size: Int, maxInboundMessageSize: Long, what: String = "message") {
        requireWithinLimit(size.toLong(), maxInboundMessageSize, what)
    }

    fun requireWithinLimit(size: Long, maxInboundMessageSize: Long, what: String = "message") {
        if (maxInboundMessageSize <= 0) return
        if (size > maxInboundMessageSize) {
            throw ConnectException(
                ConnectCode.RESOURCE_EXHAUSTED,
                "$what size $size exceeds limit $maxInboundMessageSize bytes",
            )
        }
    }

    /** Envelope length field is signed 32-bit; reject negative / over-limit before allocating. */
    fun requireEnvelopeLength(length: Int, maxInboundMessageSize: Long) {
        if (length < 0) {
            throw ConnectException(ConnectCode.INVALID_ARGUMENT, "Invalid envelope length: $length")
        }
        requireWithinLimit(length.toLong(), maxInboundMessageSize, "envelope")
    }
}

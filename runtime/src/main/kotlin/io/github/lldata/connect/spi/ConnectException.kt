package io.github.lldata.connect.spi

/**
 * Protocol-level Connect error. Mapped to JSON body + non-200 HTTP status for unary RPCs.
 */
class ConnectException @JvmOverloads constructor(
    val code: ConnectCode,
    message: String? = null,
    cause: Throwable? = null,
    val details: List<ConnectErrorDetail> = emptyList(),
) : RuntimeException(message, cause) {

    companion object {
        @JvmStatic
        fun of(code: ConnectCode, message: String? = null): ConnectException =
            ConnectException(code, message)

        @JvmStatic
        fun unimplemented(message: String? = null): ConnectException =
            ConnectException(ConnectCode.UNIMPLEMENTED, message)

        @JvmStatic
        fun invalidArgument(message: String? = null): ConnectException =
            ConnectException(ConnectCode.INVALID_ARGUMENT, message)

        @JvmStatic
        fun internal(message: String? = null, cause: Throwable? = null): ConnectException =
            ConnectException(ConnectCode.INTERNAL, message, cause)
    }
}

/**
 * Strongly-typed error detail as defined by Connect (type + unpadded base64 protobuf value).
 */
data class ConnectErrorDetail(
    val type: String,
    val valueBase64: String,
    val debug: Any? = null,
)

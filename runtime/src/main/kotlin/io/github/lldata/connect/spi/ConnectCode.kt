package io.github.lldata.connect.spi

/**
 * Connect / gRPC status codes and their unary HTTP status mapping
 * (https://connectrpc.com/docs/protocol/#error-codes).
 *
 * Language-agnostic SPI: callable from Java and Kotlin.
 */
enum class ConnectCode(val wireName: String, val httpStatus: Int) {
    CANCELED("canceled", 499),
    UNKNOWN("unknown", 500),
    INVALID_ARGUMENT("invalid_argument", 400),
    DEADLINE_EXCEEDED("deadline_exceeded", 504),
    NOT_FOUND("not_found", 404),
    ALREADY_EXISTS("already_exists", 409),
    PERMISSION_DENIED("permission_denied", 403),
    RESOURCE_EXHAUSTED("resource_exhausted", 429),
    FAILED_PRECONDITION("failed_precondition", 400),
    ABORTED("aborted", 409),
    OUT_OF_RANGE("out_of_range", 400),
    UNIMPLEMENTED("unimplemented", 501),
    INTERNAL("internal", 500),
    UNAVAILABLE("unavailable", 503),
    DATA_LOSS("data_loss", 500),
    UNAUTHENTICATED("unauthenticated", 401);

    companion object {
        @JvmStatic
        fun fromWireName(name: String): ConnectCode? =
            entries.firstOrNull { it.wireName == name }

        /**
         * Infer a Connect code when a unary error response body is missing/malformed
         * (HTTP → Connect mapping from the protocol reference).
         */
        @JvmStatic
        fun fromHttpStatus(status: Int): ConnectCode = when (status) {
            400 -> INTERNAL
            401 -> UNAUTHENTICATED
            403 -> PERMISSION_DENIED
            404 -> UNIMPLEMENTED
            429, 502, 503, 504 -> UNAVAILABLE
            else -> UNKNOWN
        }
    }
}

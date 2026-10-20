package io.github.lldata.connect.runtime.grpc

import io.github.lldata.connect.spi.ConnectCode
import io.github.lldata.connect.spi.ConnectException
import io.grpc.Status
import io.grpc.StatusException
import io.grpc.StatusRuntimeException

/**
 * Bidirectional mapping between gRPC [Status.Code] and Connect [ConnectCode].
 */
object GrpcStatusMapping {

    fun toConnectCode(code: Status.Code): ConnectCode = when (code) {
        Status.Code.OK -> error("OK is not an error code")
        Status.Code.CANCELLED -> ConnectCode.CANCELED
        Status.Code.UNKNOWN -> ConnectCode.UNKNOWN
        Status.Code.INVALID_ARGUMENT -> ConnectCode.INVALID_ARGUMENT
        Status.Code.DEADLINE_EXCEEDED -> ConnectCode.DEADLINE_EXCEEDED
        Status.Code.NOT_FOUND -> ConnectCode.NOT_FOUND
        Status.Code.ALREADY_EXISTS -> ConnectCode.ALREADY_EXISTS
        Status.Code.PERMISSION_DENIED -> ConnectCode.PERMISSION_DENIED
        Status.Code.RESOURCE_EXHAUSTED -> ConnectCode.RESOURCE_EXHAUSTED
        Status.Code.FAILED_PRECONDITION -> ConnectCode.FAILED_PRECONDITION
        Status.Code.ABORTED -> ConnectCode.ABORTED
        Status.Code.OUT_OF_RANGE -> ConnectCode.OUT_OF_RANGE
        Status.Code.UNIMPLEMENTED -> ConnectCode.UNIMPLEMENTED
        Status.Code.INTERNAL -> ConnectCode.INTERNAL
        Status.Code.UNAVAILABLE -> ConnectCode.UNAVAILABLE
        Status.Code.DATA_LOSS -> ConnectCode.DATA_LOSS
        Status.Code.UNAUTHENTICATED -> ConnectCode.UNAUTHENTICATED
    }

    fun toGrpcStatus(code: ConnectCode): Status.Code = when (code) {
        ConnectCode.CANCELED -> Status.Code.CANCELLED
        ConnectCode.UNKNOWN -> Status.Code.UNKNOWN
        ConnectCode.INVALID_ARGUMENT -> Status.Code.INVALID_ARGUMENT
        ConnectCode.DEADLINE_EXCEEDED -> Status.Code.DEADLINE_EXCEEDED
        ConnectCode.NOT_FOUND -> Status.Code.NOT_FOUND
        ConnectCode.ALREADY_EXISTS -> Status.Code.ALREADY_EXISTS
        ConnectCode.PERMISSION_DENIED -> Status.Code.PERMISSION_DENIED
        ConnectCode.RESOURCE_EXHAUSTED -> Status.Code.RESOURCE_EXHAUSTED
        ConnectCode.FAILED_PRECONDITION -> Status.Code.FAILED_PRECONDITION
        ConnectCode.ABORTED -> Status.Code.ABORTED
        ConnectCode.OUT_OF_RANGE -> Status.Code.OUT_OF_RANGE
        ConnectCode.UNIMPLEMENTED -> Status.Code.UNIMPLEMENTED
        ConnectCode.INTERNAL -> Status.Code.INTERNAL
        ConnectCode.UNAVAILABLE -> Status.Code.UNAVAILABLE
        ConnectCode.DATA_LOSS -> Status.Code.DATA_LOSS
        ConnectCode.UNAUTHENTICATED -> Status.Code.UNAUTHENTICATED
    }

    fun toConnectException(error: Throwable): ConnectException {
        val status = when (error) {
            is StatusRuntimeException -> error.status
            is StatusException -> error.status
            is ConnectException -> return error
            else -> Status.fromThrowable(error)
        }
        val message = status.description ?: error.message
        return ConnectException(toConnectCode(status.code), message, error)
    }
}

package io.github.lldata.connect.runtime.grpc

import io.github.lldata.connect.runtime.StreamCallControl
import io.github.lldata.connect.spi.ConnectCallContext
import io.github.lldata.connect.spi.ConnectStreamObserver
import io.grpc.ServerInterceptor
import io.grpc.ServerInterceptors
import io.grpc.ServerMethodDefinition
import io.grpc.ServerServiceDefinition

/**
 * Invokes a gRPC BIDI_STREAMING [ServerMethodDefinition] through optional interceptors.
 *
 * Prefer [open] so request messages can be fed as HTTP envelopes arrive; [invoke] remains
 * for the SPI list path (collect-then-handle).
 */
object GrpcBidiStreamInvoker {

    fun <ReqT, RespT> open(
        method: ServerMethodDefinition<ReqT, RespT>,
        interceptors: List<ServerInterceptor>,
        context: ConnectCallContext,
        observer: ConnectStreamObserver<RespT>,
        callControl: StreamCallControl? = null,
        onWritable: (() -> Unit)? = null,
    ): OpenBidiStream<ReqT, RespT> {
        val typed = resolve(method, interceptors)
        return OpenBidiStream(
            typed.methodDescriptor,
            typed.serverCallHandler,
            context,
            observer,
            callControl,
            onWritable = onWritable,
        )
    }

    fun <ReqT, RespT> invoke(
        method: ServerMethodDefinition<ReqT, RespT>,
        interceptors: List<ServerInterceptor>,
        requests: List<ReqT>,
        context: ConnectCallContext,
        observer: ConnectStreamObserver<RespT>,
        callControl: StreamCallControl? = null,
    ) {
        val session = open(method, interceptors, context, observer, callControl)
        for (request in requests) {
            session.send(request)
        }
        session.halfClose()
        session.run()
    }

    private fun <ReqT, RespT> resolve(
        method: ServerMethodDefinition<ReqT, RespT>,
        interceptors: List<ServerInterceptor>,
    ): ServerMethodDefinition<ReqT, RespT> {
        val definition = if (interceptors.isEmpty()) {
            ServerServiceDefinition.builder("connect-bidi-bridge")
                .addMethod(method)
                .build()
                .getMethod(method.methodDescriptor.fullMethodName)
        } else {
            val base = ServerServiceDefinition.builder(method.methodDescriptor.serviceName)
                .addMethod(method)
                .build()
            val intercepted = ServerInterceptors.intercept(base, interceptors)
            intercepted.getMethod(method.methodDescriptor.fullMethodName)
                ?: error("Intercepted bidi method missing: ${method.methodDescriptor.fullMethodName}")
        }
        @Suppress("UNCHECKED_CAST")
        return definition as ServerMethodDefinition<ReqT, RespT>
    }
}

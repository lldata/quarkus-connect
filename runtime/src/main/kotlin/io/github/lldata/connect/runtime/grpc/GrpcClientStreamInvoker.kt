package io.github.lldata.connect.runtime.grpc

import io.github.lldata.connect.runtime.security.ConnectSecuritySupport
import io.github.lldata.connect.spi.ConnectCallContext
import io.grpc.ServerInterceptor
import io.grpc.ServerInterceptors
import io.grpc.ServerMethodDefinition
import io.grpc.ServerServiceDefinition

/**
 * Invokes a gRPC CLIENT_STREAMING [ServerMethodDefinition] through optional interceptors.
 *
 * Prefer [open] so request messages can be fed as HTTP envelopes arrive; [invoke] remains
 * for the SPI list path (collect-then-handle).
 */
object GrpcClientStreamInvoker {

    fun <ReqT, RespT> open(
        method: ServerMethodDefinition<ReqT, RespT>,
        interceptors: List<ServerInterceptor>,
        context: ConnectCallContext,
        onWritable: (() -> Unit)? = null,
    ): OpenClientStream<ReqT, RespT> = ConnectSecuritySupport.withRequestContext {
        val typed = resolve(method, interceptors)
        OpenClientStream(typed.methodDescriptor, typed.serverCallHandler, context, onWritable = onWritable)
    }

    fun <ReqT, RespT> invoke(
        method: ServerMethodDefinition<ReqT, RespT>,
        interceptors: List<ServerInterceptor>,
        requests: List<ReqT>,
        context: ConnectCallContext,
    ): RespT {
        val session = open(method, interceptors, context)
        for (request in requests) {
            session.send(request)
        }
        session.halfClose()
        return session.awaitResponse()
    }

    private fun <ReqT, RespT> resolve(
        method: ServerMethodDefinition<ReqT, RespT>,
        interceptors: List<ServerInterceptor>,
    ): ServerMethodDefinition<ReqT, RespT> {
        val definition = if (interceptors.isEmpty()) {
            ServerServiceDefinition.builder("connect-client-stream-bridge")
                .addMethod(method)
                .build()
                .getMethod(method.methodDescriptor.fullMethodName)
        } else {
            val base = ServerServiceDefinition.builder(method.methodDescriptor.serviceName)
                .addMethod(method)
                .build()
            val intercepted = ServerInterceptors.intercept(base, interceptors)
            intercepted.getMethod(method.methodDescriptor.fullMethodName)
                ?: error("Intercepted client-stream method missing: ${method.methodDescriptor.fullMethodName}")
        }
        @Suppress("UNCHECKED_CAST")
        return definition as ServerMethodDefinition<ReqT, RespT>
    }
}

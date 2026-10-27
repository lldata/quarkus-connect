package io.github.lldata.connect.runtime.grpc

import com.google.protobuf.Message
import io.github.lldata.connect.runtime.DefaultConnectCallContext
import io.github.lldata.connect.spi.ConnectBidiStreamMethod
import io.github.lldata.connect.spi.ConnectCallContext
import io.github.lldata.connect.spi.ConnectStreamObserver
import io.grpc.ServerInterceptor
import io.grpc.ServerMethodDefinition
import java.io.ByteArrayInputStream

/**
 * [ConnectBidiStreamMethod] backed by a gRPC BIDI_STREAMING [ServerMethodDefinition].
 */
class GrpcBridgedBidiStreamMethod<ReqT : Message, RespT : Message>(
    private val method: ServerMethodDefinition<ReqT, RespT>,
    private val interceptors: List<ServerInterceptor>,
    private val requestClass: Class<ReqT>,
    private val responseClass: Class<RespT>,
) : ConnectBidiStreamMethod<ReqT, RespT> {

    override fun procedure(): String = method.methodDescriptor.fullMethodName

    override fun requestType(): Class<ReqT> = requestClass

    override fun responseType(): Class<RespT> = responseClass

    override fun handle(requests: List<ReqT>, context: ConnectCallContext, response: ConnectStreamObserver<RespT>) {
        val control = (context as? DefaultConnectCallContext)?.streamCallControl
        GrpcBidiStreamInvoker.invoke(method, interceptors, requests, context, response, control)
    }

    /** Open a streaming inbound session (messages fed as HTTP envelopes arrive). */
    fun open(
        context: ConnectCallContext,
        response: ConnectStreamObserver<RespT>,
        onWritable: (() -> Unit)? = null,
    ): OpenBidiStream<ReqT, RespT> {
        val control = (context as? DefaultConnectCallContext)?.streamCallControl
        return GrpcBidiStreamInvoker.open(method, interceptors, context, response, control, onWritable)
    }

    companion object {
        @JvmStatic
        fun <ReqT, RespT> from(
            method: ServerMethodDefinition<ReqT, RespT>,
            interceptors: List<ServerInterceptor>,
        ): GrpcBridgedBidiStreamMethod<*, *> {
            @Suppress("UNCHECKED_CAST")
            val reqPrototype = method.methodDescriptor.parseRequest(ByteArrayInputStream(ByteArray(0))) as Message
            @Suppress("UNCHECKED_CAST")
            val respPrototype = method.methodDescriptor.parseResponse(ByteArrayInputStream(ByteArray(0))) as Message
            @Suppress("UNCHECKED_CAST")
            return GrpcBridgedBidiStreamMethod(
                method as ServerMethodDefinition<Message, Message>,
                interceptors,
                reqPrototype.javaClass as Class<Message>,
                respPrototype.javaClass as Class<Message>,
            )
        }
    }
}

package io.github.lldata.connect.runtime.grpc

import com.google.protobuf.Message
import io.github.lldata.connect.runtime.DefaultConnectCallContext
import io.github.lldata.connect.spi.ConnectCallContext
import io.github.lldata.connect.spi.ConnectServerStreamMethod
import io.github.lldata.connect.spi.ConnectStreamObserver
import io.grpc.ServerInterceptor
import io.grpc.ServerMethodDefinition
import java.io.ByteArrayInputStream

/**
 * [ConnectServerStreamMethod] backed by a gRPC SERVER_STREAMING [ServerMethodDefinition].
 */
class GrpcBridgedServerStreamMethod<ReqT : Message, RespT : Message>(
    private val method: ServerMethodDefinition<ReqT, RespT>,
    private val interceptors: List<ServerInterceptor>,
    private val requestClass: Class<ReqT>,
    private val responseClass: Class<RespT>,
) : ConnectServerStreamMethod<ReqT, RespT> {

    override fun procedure(): String = method.methodDescriptor.fullMethodName

    override fun requestType(): Class<ReqT> = requestClass

    override fun responseType(): Class<RespT> = responseClass

    override fun handle(request: ReqT, context: ConnectCallContext, response: ConnectStreamObserver<RespT>) {
        val control = (context as? DefaultConnectCallContext)?.streamCallControl
        GrpcServerStreamInvoker.invoke(method, interceptors, request, context, response, control)
    }

    companion object {
        @JvmStatic
        fun <ReqT, RespT> from(
            method: ServerMethodDefinition<ReqT, RespT>,
            interceptors: List<ServerInterceptor>,
        ): GrpcBridgedServerStreamMethod<*, *> {
            @Suppress("UNCHECKED_CAST")
            val reqPrototype = method.methodDescriptor.parseRequest(ByteArrayInputStream(ByteArray(0))) as Message
            @Suppress("UNCHECKED_CAST")
            val respPrototype = method.methodDescriptor.parseResponse(ByteArrayInputStream(ByteArray(0))) as Message
            @Suppress("UNCHECKED_CAST")
            return GrpcBridgedServerStreamMethod(
                method as ServerMethodDefinition<Message, Message>,
                interceptors,
                reqPrototype.javaClass as Class<Message>,
                respPrototype.javaClass as Class<Message>,
            )
        }
    }
}

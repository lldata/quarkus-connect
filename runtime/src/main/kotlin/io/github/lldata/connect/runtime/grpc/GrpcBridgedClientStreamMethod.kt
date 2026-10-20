package io.github.lldata.connect.runtime.grpc

import com.google.protobuf.Message
import io.github.lldata.connect.spi.ConnectCallContext
import io.github.lldata.connect.spi.ConnectClientStreamMethod
import io.grpc.ServerInterceptor
import io.grpc.ServerMethodDefinition
import java.io.ByteArrayInputStream

/**
 * [ConnectClientStreamMethod] backed by a gRPC CLIENT_STREAMING [ServerMethodDefinition].
 */
class GrpcBridgedClientStreamMethod<ReqT : Message, RespT : Message>(
    private val method: ServerMethodDefinition<ReqT, RespT>,
    private val interceptors: List<ServerInterceptor>,
    private val requestClass: Class<ReqT>,
    private val responseClass: Class<RespT>,
) : ConnectClientStreamMethod<ReqT, RespT> {

    override fun procedure(): String = method.methodDescriptor.fullMethodName

    override fun requestType(): Class<ReqT> = requestClass

    override fun responseType(): Class<RespT> = responseClass

    override fun handle(requests: List<ReqT>, context: ConnectCallContext): RespT =
        GrpcClientStreamInvoker.invoke(method, interceptors, requests, context)

    companion object {
        @JvmStatic
        fun <ReqT, RespT> from(
            method: ServerMethodDefinition<ReqT, RespT>,
            interceptors: List<ServerInterceptor>,
        ): GrpcBridgedClientStreamMethod<*, *> {
            @Suppress("UNCHECKED_CAST")
            val reqPrototype = method.methodDescriptor.parseRequest(ByteArrayInputStream(ByteArray(0))) as Message
            @Suppress("UNCHECKED_CAST")
            val respPrototype = method.methodDescriptor.parseResponse(ByteArrayInputStream(ByteArray(0))) as Message
            @Suppress("UNCHECKED_CAST")
            return GrpcBridgedClientStreamMethod(
                method as ServerMethodDefinition<Message, Message>,
                interceptors,
                reqPrototype.javaClass as Class<Message>,
                respPrototype.javaClass as Class<Message>,
            )
        }
    }
}

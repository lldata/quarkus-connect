package io.github.lldata.connect.runtime.grpc

import com.google.protobuf.Message
import io.github.lldata.connect.spi.ConnectCallContext
import io.github.lldata.connect.spi.ConnectUnaryMethod
import io.grpc.ServerInterceptor
import io.grpc.ServerMethodDefinition
import java.io.ByteArrayInputStream

/**
 * [ConnectUnaryMethod] backed by a gRPC [ServerMethodDefinition] from a `@GrpcService` bean.
 * Server interceptors (including `@GlobalInterceptor`) run for Connect and gRPC-Web calls.
 */
class GrpcBridgedUnaryMethod<ReqT : Message, RespT : Message>(
    private val method: ServerMethodDefinition<ReqT, RespT>,
    private val interceptors: List<ServerInterceptor>,
    private val requestClass: Class<ReqT>,
    private val responseClass: Class<RespT>,
) : ConnectUnaryMethod<ReqT, RespT> {

    override fun procedure(): String = method.methodDescriptor.fullMethodName

    override fun requestType(): Class<ReqT> = requestClass

    override fun responseType(): Class<RespT> = responseClass

    override fun handle(request: ReqT, context: ConnectCallContext): RespT =
        GrpcUnaryInvoker.invoke(method, interceptors, request, context)

    companion object {
        @JvmStatic
        fun <ReqT, RespT> from(
            method: ServerMethodDefinition<ReqT, RespT>,
            interceptors: List<ServerInterceptor>,
        ): GrpcBridgedUnaryMethod<*, *> {
            @Suppress("UNCHECKED_CAST")
            val reqPrototype = method.methodDescriptor.parseRequest(ByteArrayInputStream(ByteArray(0))) as Message
            @Suppress("UNCHECKED_CAST")
            val respPrototype = method.methodDescriptor.parseResponse(ByteArrayInputStream(ByteArray(0))) as Message
            @Suppress("UNCHECKED_CAST")
            return GrpcBridgedUnaryMethod(
                method as ServerMethodDefinition<Message, Message>,
                interceptors,
                reqPrototype.javaClass as Class<Message>,
                respPrototype.javaClass as Class<Message>,
            )
        }
    }
}

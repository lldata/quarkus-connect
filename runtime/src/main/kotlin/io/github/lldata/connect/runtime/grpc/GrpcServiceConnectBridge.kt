package io.github.lldata.connect.runtime.grpc

import io.github.lldata.connect.spi.ConnectUnaryMethod
import io.grpc.BindableService
import io.grpc.MethodDescriptor
import io.grpc.ServerInterceptor
import io.quarkus.grpc.GlobalInterceptor
import io.quarkus.grpc.GrpcService
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Instance
import jakarta.inject.Inject
import org.jboss.logging.Logger

/**
 * Discovers Quarkus `@GrpcService` [BindableService] beans and `@GlobalInterceptor`
 * [ServerInterceptor]s, then exposes each unary RPC as a [ConnectUnaryMethod].
 */
@ApplicationScoped
class GrpcServiceConnectBridge {

    private val log = Logger.getLogger(GrpcServiceConnectBridge::class.java)

    @Inject
    @GrpcService
    private lateinit var grpcServices: Instance<BindableService>

    @Inject
    @GlobalInterceptor
    private lateinit var globalInterceptors: Instance<ServerInterceptor>

    fun bridgedMethods(): List<ConnectUnaryMethod<*, *>> {
        val interceptors = globalInterceptors.toList()
        val methods = mutableListOf<ConnectUnaryMethod<*, *>>()

        if (grpcServices.isUnsatisfied) {
            log.debug("No @GrpcService beans found for Connect bridge")
            return methods
        }

        for (service in grpcServices) {
            val definition = service.bindService()
            for (method in definition.methods) {
                if (method.methodDescriptor.type != MethodDescriptor.MethodType.UNARY) {
                    log.debugf(
                        "Skipping non-unary gRPC method %s (%s)",
                        method.methodDescriptor.fullMethodName,
                        method.methodDescriptor.type,
                    )
                    continue
                }
                methods += GrpcBridgedUnaryMethod.from(method, interceptors)
                log.infof(
                    "Bridged @GrpcService unary method /%s → Connect + gRPC-Web (%d global interceptor(s))",
                    method.methodDescriptor.fullMethodName,
                    interceptors.size,
                )
            }
        }
        return methods
    }
}

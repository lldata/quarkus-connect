package io.github.lldata.connect.deployment;

import io.github.lldata.connect.runtime.ConnectMethodRegistry;
import io.github.lldata.connect.runtime.ConnectRouteRegistrar;
import io.github.lldata.connect.runtime.grpc.GrpcServiceConnectBridge;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;

/**
 * Quarkus build-time processor for the Connect RPC extension.
 * Registers runtime CDI beans that bridge {@code @GrpcService} to Connect + gRPC-Web.
 */
class ConnectProcessor {

    private static final String FEATURE = "connect";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    AdditionalBeanBuildItem registerBeans() {
        return AdditionalBeanBuildItem.builder()
                .addBeanClasses(
                        ConnectRouteRegistrar.class,
                        ConnectMethodRegistry.class,
                        GrpcServiceConnectBridge.class)
                .setUnremovable()
                .build();
    }
}

package io.github.lldata.connect.runtime;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Runtime configuration for Connect and gRPC-Web on the Quarkus HTTP port.
 */
@ConfigMapping(prefix = "quarkus.connect")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface ConnectRuntimeConfig {

    /**
     * JSON codec ({@code application/json}, {@code application/connect+json},
     * {@code application/grpc-web+json}).
     */
    Json json();

    interface Json {

        /**
         * Print response fields with their proto field names ({@code request_id}) instead of the
         * proto3 JSON lowerCamelCase names ({@code requestId}).
         * <p>
         * Applies to every JSON response message: Connect unary, Connect streaming and
         * gRPC-Web {@code +json}. Requests are parsed in either spelling regardless of this
         * setting. The default keeps the proto3 JSON mapping that Connect clients expect.
         */
        @WithDefault("false")
        boolean preserveProtoFieldNames();
    }
}

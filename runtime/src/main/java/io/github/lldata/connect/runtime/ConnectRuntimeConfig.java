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
     * Maximum decoded message size in bytes for a Connect unary body and for each
     * Connect / gRPC-Web streaming envelope (after decompressing the envelope payload).
     * <p>
     * Default {@code 4194304} (4 MiB) matches the common gRPC default. Align with
     * {@code quarkus.grpc.server.max-inbound-message-size} so the same {@code @GrpcService}
     * sees the same limit on every protocol. Exceeding the limit yields Connect
     * {@code resource_exhausted} (not a bare HTTP 413).
     * <p>
     * Also raise {@code quarkus.http.limits.max-body-size} to at least this value (plus a
     * small framing overhead for streaming), otherwise Vert.x / Quarkus may reject the
     * HTTP body with 413 before Connect runs.
     */
    @WithDefault("4194304")
    long maxInboundMessageSize();

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

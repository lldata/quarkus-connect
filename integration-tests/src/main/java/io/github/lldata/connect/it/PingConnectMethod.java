package io.github.lldata.connect.it;

import io.github.lldata.connect.it.greet.v1.GreetRequest;
import io.github.lldata.connect.it.greet.v1.GreetResponse;
import io.github.lldata.connect.spi.ConnectCallContext;
import io.github.lldata.connect.spi.ConnectCode;
import io.github.lldata.connect.spi.ConnectException;
import io.github.lldata.connect.spi.ConnectUnaryMethod;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Java SPI programming-model proof: a CDI {@link ConnectUnaryMethod} bean (no {@code @GrpcService}).
 * Exercises response headers and Connect unary trailers ({@code trailer-*}).
 */
@ApplicationScoped
public class PingConnectMethod implements ConnectUnaryMethod<GreetRequest, GreetResponse> {

    @Override
    public String procedure() {
        return "greet.v1.GreetService/Ping";
    }

    @Override
    public Class<GreetRequest> requestType() {
        return GreetRequest.class;
    }

    @Override
    public Class<GreetResponse> responseType() {
        return GreetResponse.class;
    }

    @Override
    public GreetResponse handle(GreetRequest request, ConnectCallContext context) throws ConnectException {
        String name = request.getName() == null ? "" : request.getName().trim();
        if (name.isEmpty()) {
            throw new ConnectException(ConnectCode.INVALID_ARGUMENT, "name must not be empty");
        }
        if (name.equalsIgnoreCase("nomsg")) {
            // Protocol: omit the JSON "message" field when there is no description.
            throw new ConnectException(ConnectCode.NOT_FOUND, null);
        }
        context.responseHeader("x-ping", "1");
        context.responseTrailer("x-ping-trail", "ok");
        return GreetResponse.newBuilder()
                .setGreeting("Pong, " + name + "!")
                .setRequestId(request.getRequestId())
                .build();
    }
}

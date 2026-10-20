package io.github.lldata.connect.it

import io.github.lldata.connect.it.greet.v1.GreetRequest
import io.github.lldata.connect.it.greet.v1.GreetResponse
import io.github.lldata.connect.spi.ConnectCallContext
import io.github.lldata.connect.spi.ConnectException
import io.github.lldata.connect.spi.ConnectUnaryMethod
import jakarta.enterprise.context.ApplicationScoped

/**
 * Sample unary Connect service: `greet.v1.GreetService/Greet`.
 */
@ApplicationScoped
class GreetConnectService : ConnectUnaryMethod<GreetRequest, GreetResponse> {

    override fun procedure(): String = "greet.v1.GreetService/Greet"

    override fun requestType(): Class<GreetRequest> = GreetRequest::class.java

    override fun responseType(): Class<GreetResponse> = GreetResponse::class.java

    override fun handle(request: GreetRequest, context: ConnectCallContext): GreetResponse {
        val name = request.name.trim()
        if (name.isEmpty()) {
            throw ConnectException.invalidArgument("name must not be empty")
        }
        context.responseHeader("x-quarkus-connect", "v1")
        return GreetResponse.newBuilder()
            .setGreeting("Hello, $name!")
            .build()
    }
}

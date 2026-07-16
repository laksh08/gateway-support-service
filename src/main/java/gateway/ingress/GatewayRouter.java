package gateway.ingress;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.path;

/**
 * Functional router that maps all {@code /WebServices/**} requests to the {@link GatewayHandler}.
 *
 * <p>Using functional routing (instead of {@code @RequestMapping}) avoids Spring MVC entirely
 * and keeps the gateway fully within the WebFlux functional DSL.
 */
@Configuration
public class GatewayRouter {

    @Bean
    public RouterFunction<ServerResponse> gatewayRouterFunction(GatewayHandler handler) {
        return RouterFunctions.route()
                .nest(
                        path("/WebServices/**"),
                        nested ->
                                nested.POST("/**", handler::handle)
                                      .GET("/**", handler::handle))
                .build();
    }
}

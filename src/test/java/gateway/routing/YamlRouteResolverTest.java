package gateway.routing;

import gateway.config.GatewayProperties;
import gateway.config.GatewayProperties.RouteProperties;
import java.time.Duration;
import java.util.Map;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

class YamlRouteResolverTest {

    private YamlRouteResolver resolver;

    @BeforeEach
    void setUp() {
        GatewayProperties props = new GatewayProperties(
                null,
                Map.of(
                        "createCustomer",
                        new RouteProperties("customer-service", "/soap/customer", Duration.ofSeconds(30)),
                        "createOrder",
                        new RouteProperties("order-service", "/soap/order", Duration.ofSeconds(20))),
                "yaml",
                "round-robin",
                new GatewayProperties.ForwardingProperties(
                        Duration.ofSeconds(5), Duration.ofSeconds(30),
                        Duration.ofSeconds(10), 100,
                        Duration.ofSeconds(30), 4_194_304),
                new GatewayProperties.CacheProperties(
                        Duration.ofMinutes(5), Duration.ofSeconds(30), 1000));

        resolver = new YamlRouteResolver(props);
    }

    @Test
    void resolvesKnownOperation() {
        StepVerifier.create(resolver.resolve("createCustomer"))
                .assertNext(route -> {
                    Assertions.assertThat(route.serviceName()).isEqualTo("customer-service");
                    Assertions.assertThat(route.targetPath()).isEqualTo("/soap/customer");
                    Assertions.assertThat(route.timeout()).isEqualTo(Duration.ofSeconds(30));
                })
                .verifyComplete();
    }

    @Test
    void returnsEmptyForUnknownOperation() {
        StepVerifier.create(resolver.resolve("unknownOperation"))
                .verifyComplete();
    }

    @Test
    void loadAllReturnsAllRoutes() {
        StepVerifier.create(resolver.loadAll())
                .assertNext(routes -> {
                    Assertions.assertThat(routes).containsKeys("createCustomer", "createOrder");
                    Assertions.assertThat(routes).hasSize(2);
                })
                .verifyComplete();
    }

    @Test
    void reloadIsNoOp() {
        StepVerifier.create(resolver.reload()).verifyComplete();
    }
}

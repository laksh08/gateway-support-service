package com.gateway.smartrouter.routing;

import com.gateway.smartrouter.config.ConsulClientProperties;
import com.gateway.smartrouter.config.RoutingProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ConsulRoutingProviderTest {

    private RoutingCache routingCache;
    private ConsulRoutingProvider provider;

    @BeforeEach
    void setUp() {
        routingCache = new RoutingCache();
        RoutingProperties properties = new RoutingProperties(
                "consul",
                new RoutingProperties.ConsulProperties("gateway/routes", 30000)
        );
        ConsulClientProperties clientProps = new ConsulClientProperties(
                "127.0.0.1", 8500, "http", null, 2000, 5000);

        // Stub ConsulKvClient returning predictable routes
        ConsulKvClient stubClient = new ConsulKvClient(clientProps) {
            @Override
            public Mono<Map<String, String>> fetchRoutes(String keyPrefix) {
                return Mono.just(Map.of(
                        "getCustomer", "customer-service/soap/CustomerService",
                        "makePayment",  "payment-service:9091"
                ));
            }
        };

        provider = new ConsulRoutingProvider(properties, routingCache, stubClient);
        provider.initialize();
    }

    @Test
    void loadsRoutesFromConsulKv() {
        RouteTarget target = provider.resolveTarget("getCustomer");
        assertThat(target).isNotNull();
        assertThat(target.host()).isEqualTo("customer-service");
        assertThat(target.upstreamPath()).isEqualTo("/soap/CustomerService");
    }

    @Test
    void loadsRouteWithPortFromConsulKv() {
        RouteTarget target = provider.resolveTarget("makePayment");
        assertThat(target).isNotNull();
        assertThat(target.host()).isEqualTo("payment-service");
        assertThat(target.port()).isEqualTo(9091);
    }

    @Test
    void returnsNullForUnknownMethod() {
        assertThat(provider.resolveTarget("unknown")).isNull();
    }
}

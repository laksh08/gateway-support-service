package com.gateway.smartrouter.routing;

import com.gateway.smartrouter.config.RoutingProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
        provider = new ConsulRoutingProvider(properties, routingCache);
        provider.initialize();
    }

    @Test
    void loadsPlaceholderRoutesFromConsulFormat() {
        assertThat(provider.resolveService("getCustomer")).isEqualTo("customer-service");
        assertThat(provider.resolveService("issuePolicy")).isEqualTo("policy-service");
    }

    @Test
    void parseConsulValueHandlesCommentsAndBlankLines() {
        String value = """
                # comment
                getCustomer=customer-service

                makePayment=payment-service
                """;

        var routes = provider.parseConsulValue(value);

        assertThat(routes).hasSize(2)
                .containsEntry("getCustomer", "customer-service")
                .containsEntry("makePayment", "payment-service");
    }
}

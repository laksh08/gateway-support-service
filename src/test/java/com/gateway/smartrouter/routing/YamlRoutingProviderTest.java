package com.gateway.smartrouter.routing;

import com.gateway.smartrouter.config.RoutesProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class YamlRoutingProviderTest {

    private RoutingCache routingCache;
    private YamlRoutingProvider provider;

    @BeforeEach
    void setUp() {
        routingCache = new RoutingCache();
        RoutesProperties routesProperties = new RoutesProperties();
        routesProperties.put("getCustomer", "customer-service");
        routesProperties.put("makePayment", "payment-service");
        provider = new YamlRoutingProvider(routesProperties, routingCache);
        provider.initialize();
    }

    @Test
    void resolvesConfiguredRoutes() {
        assertThat(provider.resolveService("getCustomer")).isEqualTo("customer-service");
        assertThat(provider.resolveService("makePayment")).isEqualTo("payment-service");
    }

    @Test
    void returnsNullForUnknownRoute() {
        assertThat(provider.resolveService("unknown")).isNull();
    }

    @Test
    void reloadReplacesCacheAtomically() {
        RoutesProperties updated = new RoutesProperties();
        updated.put("newMethod", "new-service");
        YamlRoutingProvider reloaded = new YamlRoutingProvider(updated, routingCache);
        reloaded.reload();

        assertThat(routingCache.resolve("newMethod")).isEqualTo("new-service");
        assertThat(routingCache.resolve("getCustomer")).isNull();
    }
}

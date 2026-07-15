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
        routesProperties.put("getCustomer", "customer-service/soap/CustomerService");
        routesProperties.put("makePayment", "payment-service:9091/soap/PaymentService");
        provider = new YamlRoutingProvider(routesProperties, routingCache);
        provider.initialize();
    }

    @Test
    void resolvesSimpleServiceName() {
        RouteTarget target = provider.resolveTarget("getCustomer");
        assertThat(target).isNotNull();
        assertThat(target.host()).isEqualTo("customer-service");
        assertThat(target.upstreamPath()).isEqualTo("/soap/CustomerService");
        assertThat(target.hasPort()).isFalse();
    }

    @Test
    void resolvesServiceWithPort() {
        RouteTarget target = provider.resolveTarget("makePayment");
        assertThat(target).isNotNull();
        assertThat(target.host()).isEqualTo("payment-service");
        assertThat(target.port()).isEqualTo(9091);
        assertThat(target.upstreamPath()).isEqualTo("/soap/PaymentService");
    }

    @Test
    void returnsNullForUnknownRoute() {
        assertThat(provider.resolveTarget("unknown")).isNull();
    }

    @Test
    void reloadReplacesCacheAtomically() {
        RoutesProperties updated = new RoutesProperties();
        updated.put("newMethod", "new-service/api/new");
        YamlRoutingProvider reloaded = new YamlRoutingProvider(updated, routingCache);
        reloaded.reload();

        assertThat(routingCache.resolve("newMethod")).isNotNull();
        assertThat(routingCache.resolve("newMethod").host()).isEqualTo("new-service");
        assertThat(routingCache.resolve("getCustomer")).isNull();
    }

    @Test
    void getAllRoutesReturnsSnapshot() {
        assertThat(provider.getAllRoutes()).containsKeys("getCustomer", "makePayment");
    }
}

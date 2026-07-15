package com.gateway.smartrouter.controller;

import com.gateway.smartrouter.model.AllowedHostRequest;
import com.gateway.smartrouter.routing.RouteTarget;
import com.gateway.smartrouter.routing.RoutingProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Map;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class PortalApiIntegrationTest {

    @LocalServerPort
    private int port;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        webTestClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();
    }

    @Test
    void dashboardReturnsStats() {
        webTestClient.get()
                .uri("/api/portal/dashboard")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.totalRequests").isNumber()
                .jsonPath("$.successRate").isNumber();
    }

    @Test
    void listsAllowedHosts() {
        webTestClient.get()
                .uri("/api/portal/allowed-hosts")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].hostname").exists();
    }

    @Test
    void requiresAuthForHostMutation() {
        AllowedHostRequest request = new AllowedHostRequest(
                "test.example.com", "10.0.0.9", "test", "dmz-test", "ACTIVE");

        webTestClient.post()
                .uri("/api/portal/allowed-hosts")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void createsHostWithPortalUserHeader() {
        AllowedHostRequest request = new AllowedHostRequest(
                "portal-test.example.com", "10.0.0.99", "portal test", "dmz-01", "ACTIVE");

        webTestClient.post()
                .uri("/api/portal/allowed-hosts")
                .header("X-Portal-User", "test.user")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.hostname").isEqualTo("portal-test.example.com")
                .jsonPath("$.createdBy").isEqualTo("test.user");
    }

    @Test
    void listsRoutes() {
        webTestClient.get()
                .uri("/api/portal/routes")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].serviceMethod").exists()
                .jsonPath("$[0].routeValue").exists();
    }

    @Test
    void upsertAndRetrieveRoute() {
        webTestClient.put()
                .uri("/api/portal/routes/testMethod")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("routeValue", "test-service/soap/TestService"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.serviceMethod").isEqualTo("testMethod")
                .jsonPath("$.resolvedHost").isEqualTo("test-service")
                .jsonPath("$.upstreamPath").isEqualTo("/soap/TestService");
    }

    @TestConfiguration
    static class TestRoutingConfiguration {

        @Bean
        @Primary
        RoutingProvider testRoutingProvider() {
            return new RoutingProvider() {
                private final java.util.concurrent.atomic.AtomicReference<Map<String, RouteTarget>> routes =
                        new java.util.concurrent.atomic.AtomicReference<>(
                                Map.of("getCustomer", RouteTarget.parse("customer-service/soap/CustomerService"))
                        );

                @Override
                public void initialize() {}

                @Override
                public RouteTarget resolveTarget(String serviceMethod) {
                    return routes.get().get(serviceMethod);
                }

                @Override
                public void reload() {}

                @Override
                public Map<String, RouteTarget> getAllRoutes() {
                    return routes.get();
                }
            };
        }
    }
}

package com.gateway.smartrouter.controller;

import com.gateway.smartrouter.model.AllowedHostRequest;
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

    @TestConfiguration
    static class TestRoutingConfiguration {

        @Bean
        @Primary
        RoutingProvider testRoutingProvider() {
            return new RoutingProvider() {
                @Override
                public void initialize() {
                }

                @Override
                public String resolveService(String serviceMethod) {
                    return "customer-service";
                }

                @Override
                public void reload() {
                }
            };
        }
    }
}

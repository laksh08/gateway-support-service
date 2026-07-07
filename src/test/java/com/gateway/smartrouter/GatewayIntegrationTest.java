package com.gateway.smartrouter;

import com.gateway.smartrouter.routing.RoutingProvider;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class GatewayIntegrationTest {

    private static MockWebServer mockWebServer;

    @LocalServerPort
    private int port;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        if (mockWebServer == null) {
            throw new IllegalStateException("MockWebServer must be initialized via @DynamicPropertySource");
        }
        webTestClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .responseTimeout(java.time.Duration.ofSeconds(10))
                .build();
    }

    @AfterEach
    void drainMockServer() throws Exception {
        while (mockWebServer.getRequestCount() > 0) {
            var request = mockWebServer.takeRequest(100, java.util.concurrent.TimeUnit.MILLISECONDS);
            if (request == null) {
                break;
            }
        }
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) throws Exception {
        if (mockWebServer == null) {
            mockWebServer = new MockWebServer();
            mockWebServer.start();
        }
        registry.add("forwarding.service-url-pattern",
                () -> "http://127.0.0.1:" + mockWebServer.getPort());
    }

    @Test
    void returns403ForDisallowedHost() {
        webTestClient.post()
                .uri("/WebServices/Gateway/CBISvc")
                .header(HttpHeaders.HOST, "evil.example.com")
                .contentType(MediaType.TEXT_XML)
                .bodyValue("<Request><serviceMethod>getCustomer</serviceMethod></Request>")
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void routesAndForwardsSoapRequest() {
        String downstreamResponse = "<Response><customerId>123</customerId></Response>";
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_XML_VALUE)
                .setBody(downstreamResponse));

        webTestClient.post()
                .uri("/WebServices/Gateway/CBISvc")
                .header(HttpHeaders.HOST, "gateway.example.com")
                .contentType(MediaType.TEXT_XML)
                .bodyValue("<Request><serviceMethod>getCustomer</serviceMethod></Request>")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_XML)
                .expectBody(String.class).isEqualTo(downstreamResponse);
    }

    @Test
    void reloadsAllowedHostCacheViaAdminEndpoint() {
        webTestClient.post()
                .uri("/admin/cache/allowed-hosts/reload")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("reloaded")
                .jsonPath("$.hostCount").isNumber();
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

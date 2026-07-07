package com.gateway.smartrouter.service;

import com.gateway.smartrouter.config.ForwardingProperties;
import com.gateway.smartrouter.routing.RoutingProvider;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

class RequestForwardingServiceTest {

    private MockWebServer mockWebServer;
    private RequestForwardingService forwardingService;

    @BeforeEach
    void setUp() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        ForwardingProperties properties = new ForwardingProperties(
                "http://localhost:" + mockWebServer.getPort(),
                5000,
                30000,
                10,
                30
        );

        RoutingProvider routingProvider = new RoutingProvider() {
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
        forwardingService = new RequestForwardingService(WebClient.create(), properties, routingProvider);
    }

    @AfterEach
    void tearDown() throws Exception {
        mockWebServer.shutdown();
    }

    @Test
    void forwardsRequestBodyAndReturnsDownstreamResponseUnchanged() throws Exception {
        String downstreamBody = "<Response><status>OK</status></Response>";
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_XML_VALUE)
                .setBody(downstreamBody));

        byte[] requestBody = "<Request><serviceMethod>getCustomer</serviceMethod></Request>".getBytes();
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/WebServices/Gateway/CBISvc")
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_XML_VALUE)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                        .header("traceparent", "00-trace-id-span-id-01")
                        .build());

        StepVerifier.create(forwardingService.forward(exchange, "getCustomer", requestBody))
                .assertNext(response -> {
                    assertThat(response.getStatusCode().value()).isEqualTo(200);
                    assertThat(new String(response.getBody())).isEqualTo(downstreamBody);
                    assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.TEXT_XML);
                })
                .verifyComplete();

        var recorded = mockWebServer.takeRequest();
        assertThat(recorded.getMethod()).isEqualTo(HttpMethod.POST.name());
        assertThat(recorded.getBody().readUtf8()).isEqualTo(new String(requestBody));
        assertThat(recorded.getHeader(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer token");
        assertThat(recorded.getHeader("traceparent")).isEqualTo("00-trace-id-span-id-01");
    }

    @Test
    void buildsDownstreamUrlFromServiceNameAndOriginalPath() {
        ForwardingProperties properties = new ForwardingProperties(
                "http://{serviceName}", 5000, 30000, 10, 30);
        RoutingProvider routingProvider = new RoutingProvider() {
            @Override
            public void initialize() {
            }

            @Override
            public String resolveService(String serviceMethod) {
                return "payment-service";
            }

            @Override
            public void reload() {
            }
        };
        RequestForwardingService service = new RequestForwardingService(
                WebClient.create(), properties, routingProvider);

        assertThat(service.buildDownstreamUrl("payment-service", "/WebServices/Gateway/CBISvc"))
                .isEqualTo("http://payment-service/WebServices/Gateway/CBISvc");
    }
}

package com.gateway.smartrouter.service;

import com.gateway.smartrouter.config.EnvoyProperties;
import com.gateway.smartrouter.routing.RouteTarget;
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

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RequestForwardingServiceTest {

    private MockWebServer mockWebServer;
    private RequestForwardingService forwardingService;
    private EnvoyProperties envoyProperties;

    @BeforeEach
    void setUp() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        envoyProperties = new EnvoyProperties(
                "passthrough",
                "service.consul",
                mockWebServer.getPort(),
                "127.0.0.1",
                9090
        );

        RoutingProvider routingProvider = new RoutingProvider() {
            @Override
            public void initialize() {}

            @Override
            public RouteTarget resolveTarget(String serviceMethod) {
                return RouteTarget.parse("localhost/WebServices/Gateway/CBISvc");
            }

            @Override
            public void reload() {}

            @Override
            public Map<String, RouteTarget> getAllRoutes() {
                return Map.of();
            }
        };
        forwardingService = new RequestForwardingService(WebClient.create(), envoyProperties, routingProvider);
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
    void buildsConsulDnsUrl() {
        EnvoyProperties consulDnsProps = new EnvoyProperties(
                "consul-dns", "service.consul", 8080, "127.0.0.1", 9090);
        RequestForwardingService svc = new RequestForwardingService(
                WebClient.create(), consulDnsProps, routingProviderFor("customer-service/soap/CustSvc"));

        RouteTarget target = RouteTarget.parse("customer-service/soap/CustSvc");
        String url = svc.buildDownstreamUrl(target, "/WebServices/Gateway/CBISvc");
        assertThat(url).isEqualTo("http://customer-service.service.consul:8080/soap/CustSvc");
    }

    @Test
    void buildsUpstreamPortUrl() {
        EnvoyProperties upstreamProps = new EnvoyProperties(
                "upstream-port", "service.consul", 8080, "127.0.0.1", 9090);
        RequestForwardingService svc = new RequestForwardingService(
                WebClient.create(), upstreamProps, routingProviderFor("customer-service:9091/soap/CustSvc"));

        RouteTarget target = RouteTarget.parse("customer-service:9091/soap/CustSvc");
        String url = svc.buildDownstreamUrl(target, "/WebServices/Gateway/CBISvc");
        assertThat(url).isEqualTo("http://127.0.0.1:9091/soap/CustSvc");
    }

    @Test
    void usesOriginalPathWhenNoUpstreamPathSet() {
        EnvoyProperties passthroughProps = new EnvoyProperties(
                "passthrough", "service.consul", 8080, "127.0.0.1", 9090);
        RequestForwardingService svc = new RequestForwardingService(
                WebClient.create(), passthroughProps, routingProviderFor("customer-service"));

        RouteTarget target = RouteTarget.parse("customer-service");
        String url = svc.buildDownstreamUrl(target, "/WebServices/Gateway/CBISvc");
        assertThat(url).isEqualTo("http://customer-service:8080/WebServices/Gateway/CBISvc");
    }

    private RoutingProvider routingProviderFor(String rawTarget) {
        RouteTarget t = RouteTarget.parse(rawTarget);
        return new RoutingProvider() {
            @Override public void initialize() {}
            @Override public RouteTarget resolveTarget(String m) { return t; }
            @Override public void reload() {}
            @Override public Map<String, RouteTarget> getAllRoutes() { return Map.of(); }
        };
    }
}

package gateway.ingress;

import gateway.config.GatewayProperties;
import gateway.discovery.ConsulDiscoveryService;
import gateway.discovery.ServiceInstance;
import gateway.discovery.ServiceMetadata;
import gateway.loadbalancer.RoundRobinLoadBalancer;
import gateway.metrics.GatewayMetrics;
import gateway.parser.StaxSoapOperationExtractor;
import gateway.proxy.DefaultTargetUrlBuilder;
import gateway.routing.YamlRouteResolver;
import gateway.validation.AllowedHostValidator;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

/**
 * Tests the routing logic of GatewayHandler using a functional WebTestClient.
 * Error mapping (RFC 7807) is tested at the integration level; here we verify
 * that errors are propagated (5xx on infrastructure errors, route-not-found on missing ops).
 */
@ExtendWith(MockitoExtension.class)
class GatewayHandlerTest {

    @Mock
    private ConsulDiscoveryService discoveryService;

    @Mock
    private gateway.proxy.ProxyService proxyService;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        GatewayProperties props = defaultProps();
        GatewayMetrics metrics = new GatewayMetrics(new SimpleMeterRegistry());
        gateway.discovery.MetadataResolver metadataResolver = new gateway.discovery.MetadataResolver();

        GatewayHandler handler = new GatewayHandler(
                new AllowedHostValidator(props),
                new StaxSoapOperationExtractor(),
                new YamlRouteResolver(props),
                discoveryService,
                new RoundRobinLoadBalancer(),
                new DefaultTargetUrlBuilder(metadataResolver),
                proxyService,
                metadataResolver,
                metrics,
                props);

        webTestClient = WebTestClient
                .bindToRouterFunction(new GatewayRouter().gatewayRouterFunction(handler))
                .configureClient()
                .build();
    }

    @Test
    void propagatesErrorForUnknownSoapOperation() {
        // Route not found — discovery never called
        webTestClient.post()
                .uri("/WebServices/Gateway/test")
                .header("Host", "localhost")
                .contentType(MediaType.TEXT_XML)
                .bodyValue(soapBody("unknownOperation"))
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    void propagatesErrorWhenNoHealthyInstances() {
        Mockito.when(discoveryService.discoverAll("test-service"))
                .thenReturn(Mono.just(List.of()));

        webTestClient.post()
                .uri("/WebServices/Gateway/test")
                .header("Host", "localhost")
                .contentType(MediaType.TEXT_XML)
                .bodyValue(soapBody("testOperation"))
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    void propagatesErrorForInvalidXml() {
        webTestClient.post()
                .uri("/WebServices/Gateway/test")
                .header("Host", "localhost")
                .contentType(MediaType.TEXT_XML)
                .bodyValue("NOT XML")
                .exchange()
                .expectStatus().is5xxServerError();
    }

    private GatewayProperties defaultProps() {
        return new GatewayProperties(
                List.of("*"),
                Map.of("testOperation", new GatewayProperties.RouteProperties(
                        "test-service", "/soap/test", Duration.ofSeconds(10))),
                "yaml",
                "round-robin",
                new GatewayProperties.ForwardingProperties(
                        Duration.ofSeconds(5), Duration.ofSeconds(30), Duration.ofSeconds(10),
                        100, Duration.ofSeconds(30), 4_194_304),
                new GatewayProperties.CacheProperties(
                        Duration.ofMinutes(5), Duration.ofSeconds(30), 1000));
    }

    private String soapBody(String operation) {
        return """
                <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/"
                            xmlns:t="http://example.com/test">
                  <s:Body>
                    <t:%s><id>1</id></t:%s>
                  </s:Body>
                </s:Envelope>
                """.formatted(operation, operation);
    }

    private ServiceInstance testInstance() {
        return new ServiceInstance(
                "test-svc-001", "test-service", "10.0.0.1", 9090,
                List.of(), ServiceMetadata.empty(), 1, "node-1", "dc1", "default");
    }
}

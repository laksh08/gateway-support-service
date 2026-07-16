package gateway.integration;

import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.consul.ConsulContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * End-to-end integration test using a real Consul container (via Testcontainers).
 *
 * <p>Flow:
 * <ol>
 *   <li>Testcontainers starts a Consul agent in dev mode.
 *   <li>A service instance is registered in Consul with a dynamic address and port
 *       (pointing to the {@link MockWebServer} that stands in for the real downstream service).
 *   <li>The gateway is started with {@code routing.provider=yaml}
 *       (routes from application-test.yml) but uses the real Consul discovery.
 *   <li>A SOAP request is sent to the gateway; it discovers the instance via Consul,
 *       builds the URL dynamically, and proxies to MockWebServer.
 * </ol>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class GatewayIntegrationTest {

    @Container
    static final ConsulContainer consulContainer =
            new ConsulContainer("hashicorp/consul:1.19");

    private static MockWebServer downstreamServer;
    private static int downstreamPort;

    @LocalServerPort
    private int gatewayPort;

    private WebTestClient webTestClient;

    @DynamicPropertySource
    static void registerConsulProperties(DynamicPropertyRegistry registry) throws IOException {
        downstreamServer = new MockWebServer();
        downstreamServer.start();
        downstreamPort = downstreamServer.getPort();

        registry.add("consul.host", consulContainer::getHost);
        registry.add("consul.port", () -> consulContainer.getMappedPort(8500));
    }

    @BeforeEach
    void setUp() throws Exception {
        // Register test-service in Consul pointing to MockWebServer
        registerServiceInConsul("test-service", "127.0.0.1", downstreamPort);

        webTestClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + gatewayPort)
                .responseTimeout(Duration.ofSeconds(15))
                .build();
    }

    @AfterEach
    void tearDown() throws Exception {
        // Drain any unconsumed mock requests
        while (downstreamServer.getRequestCount() > 0) {
            var req = downstreamServer.takeRequest(50, java.util.concurrent.TimeUnit.MILLISECONDS);
            if (req == null) break;
        }
    }

    @Test
    void discoversAndRoutesToConsulInstance() {
        String downstreamResponse = "<Response><status>SUCCESS</status></Response>";
        downstreamServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "text/xml")
                .setBody(downstreamResponse));

        webTestClient.post()
                .uri("/WebServices/Gateway/test")
                .header("Host", "localhost")
                .contentType(MediaType.TEXT_XML)
                .bodyValue(soapEnvelope("testOperation"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo(downstreamResponse);
    }

    @Test
    void returns404ForUnknownSoapOperation() {
        webTestClient.post()
                .uri("/WebServices/Gateway/test")
                .header("Host", "localhost")
                .contentType(MediaType.TEXT_XML)
                .bodyValue(soapEnvelope("nonExistentOperation"))
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void returns400ForInvalidXml() {
        webTestClient.post()
                .uri("/WebServices/Gateway/test")
                .header("Host", "localhost")
                .contentType(MediaType.TEXT_XML)
                .bodyValue("this is not xml")
                .exchange()
                .expectStatus().isBadRequest();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void registerServiceInConsul(String name, String address, int port) throws Exception {
        String consulAddress = consulContainer.getHost();
        int consulPort = consulContainer.getMappedPort(8500);

        String payload = """
                {
                  "ID": "%s-001",
                  "Name": "%s",
                  "Address": "%s",
                  "Port": %d,
                  "Tags": ["soap"],
                  "Meta": {
                    "api-version": "v1",
                    "soap-context": "/soap/test"
                  },
                  "Check": {
                    "TTL": "10s",
                    "DeregisterCriticalServiceAfter": "1m"
                  }
                }
                """.formatted(name, name, address, port);

        HttpClient http = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://" + consulAddress + ":" + consulPort + "/v1/agent/service/register"))
                .PUT(HttpRequest.BodyPublishers.ofString(payload))
                .header("Content-Type", "application/json")
                .build();
        http.send(request, HttpResponse.BodyHandlers.ofString());

        // Mark health check as passing
        String checkId = "service:" + name + "-001";
        HttpRequest passRequest = HttpRequest.newBuilder()
                .uri(URI.create("http://" + consulAddress + ":" + consulPort
                        + "/v1/agent/check/pass/" + checkId))
                .PUT(HttpRequest.BodyPublishers.noBody())
                .build();
        http.send(passRequest, HttpResponse.BodyHandlers.ofString());
    }

    private String soapEnvelope(String operation) {
        return """
                <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/"
                            xmlns:t="http://example.com/test">
                  <s:Body>
                    <t:%s>
                      <id>1</id>
                    </t:%s>
                  </s:Body>
                </s:Envelope>
                """.formatted(operation, operation);
    }
}

package gateway.consul;

import java.time.Duration;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

class ConsulClientTest {

    private MockWebServer mockServer;
    private ConsulClient consulClient;

    @BeforeEach
    void setUp() throws Exception {
        mockServer = new MockWebServer();
        mockServer.start();
        ConsulProperties props = new ConsulProperties(
                mockServer.getHostName(),
                mockServer.getPort(),
                "http",
                "",
                "dc1",
                "gateway/routes",
                Duration.ofSeconds(2),
                Duration.ofSeconds(5));
        consulClient = new ConsulClient(props);
    }

    @AfterEach
    void tearDown() throws Exception {
        mockServer.shutdown();
    }

    @Test
    void parsesHealthyInstancesFromConsulResponse() {
        mockServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        [
                          {
                            "Node": {
                              "ID": "node-uuid",
                              "Node": "nomad-client-1",
                              "Address": "10.20.15.7",
                              "Datacenter": "dc1"
                            },
                            "Service": {
                              "ID": "customer-svc-abc123",
                              "Service": "customer-service",
                              "Address": "10.20.15.7",
                              "Port": 28934,
                              "Tags": ["soap", "v2"],
                              "Meta": {
                                "api-version": "v2",
                                "soap-context": "/soap/customer",
                                "owner": "team-alpha"
                              },
                              "Weights": { "Passing": 10, "Warning": 1 },
                              "Namespace": "default"
                            },
                            "Checks": [{ "Status": "passing", "Name": "health", "CheckID": "c1" }]
                          }
                        ]
                        """));

        StepVerifier.create(consulClient.getHealthyInstances("customer-service"))
                .assertNext(entry -> {
                    Assertions.assertThat(entry.service().id()).isEqualTo("customer-svc-abc123");
                    Assertions.assertThat(entry.service().port()).isEqualTo(28934);
                    Assertions.assertThat(entry.service().address()).isEqualTo("10.20.15.7");
                    Assertions.assertThat(entry.service().meta())
                            .containsEntry("api-version", "v2")
                            .containsEntry("soap-context", "/soap/customer");
                    Assertions.assertThat(entry.service().weights().passing()).isEqualTo(10);
                })
                .verifyComplete();
    }

    @Test
    void returnsEmptyKvListOn404() {
        mockServer.enqueue(new MockResponse().setResponseCode(404));

        StepVerifier.create(consulClient.getKvEntries("gateway/routes"))
                .assertNext(list -> Assertions.assertThat(list).isEmpty())
                .verifyComplete();
    }

    @Test
    void includesDcQueryParam() throws Exception {
        mockServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("[]"));

        consulClient.getHealthyInstances("test-service").blockFirst();

        var recorded = mockServer.takeRequest();
        Assertions.assertThat(recorded.getPath()).contains("passing=true");
        Assertions.assertThat(recorded.getPath()).contains("dc=dc1");
    }
}

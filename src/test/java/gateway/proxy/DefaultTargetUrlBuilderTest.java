package gateway.proxy;

import gateway.discovery.MetadataResolver;
import gateway.discovery.ServiceInstance;
import gateway.discovery.ServiceMetadata;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DefaultTargetUrlBuilderTest {

    private DefaultTargetUrlBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new DefaultTargetUrlBuilder(new MetadataResolver());
    }

    @Test
    void buildsUrlFromDynamicAddressAndPort() {
        ServiceInstance instance = instance("10.20.15.7", 28934);
        ServiceMetadata meta = ServiceMetadata.empty();

        String url = builder.build(instance, "/soap/customer", meta);

        Assertions.assertThat(url).isEqualTo("http://10.20.15.7:28934/soap/customer");
    }

    @Test
    void metadataSoapContextOverridesConfiguredPath() {
        ServiceInstance instance = instance("10.20.15.7", 9876);
        ServiceMetadata meta = new ServiceMetadata(Map.of("soap-context", "/meta/soap/endpoint"));

        String url = builder.build(instance, "/configured/path", meta);

        Assertions.assertThat(url).isEqualTo("http://10.20.15.7:9876/meta/soap/endpoint");
    }

    @Test
    void addsLeadingSlashWhenMissing() {
        ServiceInstance instance = instance("192.168.1.5", 8080);
        ServiceMetadata meta = ServiceMetadata.empty();

        String url = builder.build(instance, "soap/order", meta);

        Assertions.assertThat(url).isEqualTo("http://192.168.1.5:8080/soap/order");
    }

    @Test
    void differentNodesGetDifferentUrls() {
        ServiceInstance a = instance("10.0.0.1", 30001);
        ServiceInstance b = instance("10.0.0.2", 30002);
        ServiceMetadata meta = ServiceMetadata.empty();

        String urlA = builder.build(a, "/soap/svc", meta);
        String urlB = builder.build(b, "/soap/svc", meta);

        Assertions.assertThat(urlA).isEqualTo("http://10.0.0.1:30001/soap/svc");
        Assertions.assertThat(urlB).isEqualTo("http://10.0.0.2:30002/soap/svc");
        Assertions.assertThat(urlA).isNotEqualTo(urlB);
    }

    private ServiceInstance instance(String address, int port) {
        return new ServiceInstance(
                "svc-" + port,
                "test-service",
                address,
                port,
                List.of(),
                ServiceMetadata.empty(),
                1,
                "node-1",
                "dc1",
                "default");
    }
}

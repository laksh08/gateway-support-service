package gateway.discovery;

import java.time.Duration;
import java.util.Map;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MetadataResolverTest {

    private MetadataResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new MetadataResolver();
    }

    @Test
    void soapContextOverridesConfiguredPath() {
        ServiceMetadata meta = new ServiceMetadata(Map.of("soap-context", "/meta/path"));
        String result = resolver.resolveTargetPath(meta, "/configured/path");
        Assertions.assertThat(result).isEqualTo("/meta/path");
    }

    @Test
    void fallsBackToConfiguredPathWhenNoSoapContext() {
        ServiceMetadata meta = ServiceMetadata.empty();
        String result = resolver.resolveTargetPath(meta, "/configured/path");
        Assertions.assertThat(result).isEqualTo("/configured/path");
    }

    @Test
    void metadataTimeoutOverridesConfiguredTimeout() {
        ServiceMetadata meta = new ServiceMetadata(Map.of("timeout", "PT45S"));
        Duration result = resolver.resolveTimeout(meta, Duration.ofSeconds(30));
        Assertions.assertThat(result).isEqualTo(Duration.ofSeconds(45));
    }

    @Test
    void fallsBackToConfiguredTimeoutWhenMissing() {
        ServiceMetadata meta = ServiceMetadata.empty();
        Duration result = resolver.resolveTimeout(meta, Duration.ofSeconds(30));
        Assertions.assertThat(result).isEqualTo(Duration.ofSeconds(30));
    }

    @Test
    void readsRawMetadataByKey() {
        ServiceMetadata meta = new ServiceMetadata(Map.of("owner", "customer-team", "region", "ap-south"));
        Assertions.assertThat(resolver.resolve(meta, "owner")).contains("customer-team");
        Assertions.assertThat(resolver.resolve(meta, "region")).contains("ap-south");
        Assertions.assertThat(resolver.resolve(meta, "missing")).isEmpty();
    }
}

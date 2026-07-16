package gateway.consul;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Consul HTTP API client configuration bound from {@code application.yml → consul:}.
 *
 * <p>The token field is optional; leave blank when ACL is disabled.
 */
@Validated
@ConfigurationProperties(prefix = "consul")
public record ConsulProperties(
        @NotBlank @DefaultValue("127.0.0.1") String host,
        @Min(1) @Max(65535) @DefaultValue("8500") int port,
        @NotBlank @DefaultValue("http") String scheme,
        @DefaultValue("") String token,
        @NotBlank @DefaultValue("dc1") String datacenter,
        @NotBlank @DefaultValue("gateway/routes") String kvRoutePrefix,
        @DefaultValue("PT2S") Duration connectTimeout,
        @DefaultValue("PT5S") Duration readTimeout) {

    /** Builds the base URL for the Consul HTTP API (e.g. {@code http://127.0.0.1:8500}). */
    public String baseUrl() {
        return scheme + "://" + host + ":" + port;
    }
}

package gateway.validation;

import gateway.config.GatewayProperties;
import gateway.exception.HostNotAllowedException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.reactive.function.server.ServerRequest;
import reactor.test.StepVerifier;

class AllowedHostValidatorTest {

    @Test
    void allowsWildcardHost() {
        AllowedHostValidator validator = validatorFor("*");
        StepVerifier.create(validator.validate(requestWithHost("any.host.com")))
                .verifyComplete();
    }

    @Test
    void allowsConfiguredHost() {
        AllowedHostValidator validator = validatorFor("api.example.com");
        StepVerifier.create(validator.validate(requestWithHost("api.example.com")))
                .verifyComplete();
    }

    @Test
    void rejectsUnknownHost() {
        AllowedHostValidator validator = validatorFor("api.example.com");
        StepVerifier.create(validator.validate(requestWithHost("evil.example.com")))
                .expectError(HostNotAllowedException.class)
                .verify();
    }

    @Test
    void hostMatchingIsCaseInsensitive() {
        AllowedHostValidator validator = validatorFor("API.EXAMPLE.COM");
        StepVerifier.create(validator.validate(requestWithHost("api.example.com")))
                .verifyComplete();
    }

    @Test
    void ignoresPortInHostHeader() {
        AllowedHostValidator validator = validatorFor("api.example.com");
        StepVerifier.create(validator.validate(requestWithHost("api.example.com:443")))
                .verifyComplete();
    }

    private AllowedHostValidator validatorFor(String... hosts) {
        GatewayProperties props = new GatewayProperties(
                List.of(hosts),
                Map.of(),
                "yaml",
                "round-robin",
                new GatewayProperties.ForwardingProperties(
                        Duration.ofSeconds(5), Duration.ofSeconds(30),
                        Duration.ofSeconds(10), 100,
                        Duration.ofSeconds(30), 4_194_304),
                new GatewayProperties.CacheProperties(
                        Duration.ofMinutes(5), Duration.ofSeconds(30), 1000));
        return new AllowedHostValidator(props);
    }

    private ServerRequest requestWithHost(String host) {
        MockServerHttpRequest httpRequest = MockServerHttpRequest
                .post("/WebServices/Gateway/test")
                .header("Host", host)
                .build();
        return ServerRequest.create(MockServerWebExchange.from(httpRequest),
                List.of());
    }
}

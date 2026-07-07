package com.gateway.smartrouter.filter;

import com.gateway.smartrouter.service.AllowedHostService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

class AllowedHostWebFilterTest {

    private AllowedHostWebFilter filter;
    private boolean chainInvoked;

    @BeforeEach
    void setUp() {
        AllowedHostService allowedHostService = new AllowedHostService(
                () -> reactor.core.publisher.Mono.just(java.util.Set.of("gateway.example.com")));
        allowedHostService.initialize();

        filter = new AllowedHostWebFilter(allowedHostService, new ClientIpResolver());
        chainInvoked = false;
    }

    @Test
    void rejectsDisallowedHostForWebServicesPath() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/WebServices/Gateway/CBISvc")
                        .header("Host", "unauthorized.example.com")
                        .build());

        StepVerifier.create(filter.filter(exchange, chain()))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(chainInvoked).isFalse();
    }

    @Test
    void allowsValidHostForWebServicesPath() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/WebServices/Gateway/CBISvc")
                        .header("Host", "gateway.example.com")
                        .build());

        StepVerifier.create(filter.filter(exchange, chain()))
                .verifyComplete();

        assertThat(chainInvoked).isTrue();
    }

    @Test
    void skipsValidationForNonWebServicesPath() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/health")
                        .header("Host", "unauthorized.example.com")
                        .build());

        StepVerifier.create(filter.filter(exchange, chain()))
                .verifyComplete();

        assertThat(chainInvoked).isTrue();
    }

    private WebFilterChain chain() {
        return ex -> {
            chainInvoked = true;
            return Mono.empty();
        };
    }
}

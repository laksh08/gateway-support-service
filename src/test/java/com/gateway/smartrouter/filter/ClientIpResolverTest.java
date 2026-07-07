package com.gateway.smartrouter.filter;

import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;

import static org.assertj.core.api.Assertions.assertThat;

class ClientIpResolverTest {

    private final ClientIpResolver resolver = new ClientIpResolver();

    @Test
    void prefersXForwardedFor() {
        var request = MockServerHttpRequest.get("/")
                .header("X-Forwarded-For", "203.0.113.1, 198.51.100.2")
                .header("X-Real-IP", "198.51.100.2")
                .build();

        assertThat(resolver.resolve(request)).isEqualTo("203.0.113.1");
    }

    @Test
    void fallsBackToXRealIp() {
        var request = MockServerHttpRequest.get("/")
                .header("X-Real-IP", "198.51.100.2")
                .build();

        assertThat(resolver.resolve(request)).isEqualTo("198.51.100.2");
    }

    @Test
    void fallsBackToRemoteAddress() {
        var request = MockServerHttpRequest.get("/")
                .remoteAddress(new java.net.InetSocketAddress("10.0.0.5", 12345))
                .build();

        assertThat(resolver.resolve(request)).isEqualTo("10.0.0.5");
    }
}

package com.gateway.smartrouter.filter;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.util.Optional;

/**
 * Resolves the client IP from proxy headers or the remote socket address.
 * Priority: X-Forwarded-For → X-Real-IP → Remote Address.
 */
@Component
public class ClientIpResolver {

    private static final String X_FORWARDED_FOR = "X-Forwarded-For";
    private static final String X_REAL_IP = "X-Real-IP";

    public String resolve(ServerHttpRequest request) {
        return fromForwardedFor(request)
                .or(() -> fromRealIp(request))
                .or(() -> fromRemoteAddress(request))
                .orElse("unknown");
    }

    private Optional<String> fromForwardedFor(ServerHttpRequest request) {
        String forwardedFor = request.getHeaders().getFirst(X_FORWARDED_FOR);
        if (forwardedFor == null || forwardedFor.isBlank()) {
            return Optional.empty();
        }
        String firstIp = forwardedFor.split(",")[0].trim();
        return firstIp.isEmpty() ? Optional.empty() : Optional.of(firstIp);
    }

    private Optional<String> fromRealIp(ServerHttpRequest request) {
        String realIp = request.getHeaders().getFirst(X_REAL_IP);
        if (realIp == null || realIp.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(realIp.trim());
    }

    private Optional<String> fromRemoteAddress(ServerHttpRequest request) {
        InetSocketAddress remoteAddress = request.getRemoteAddress();
        if (remoteAddress == null || remoteAddress.getAddress() == null) {
            return Optional.empty();
        }
        return Optional.of(remoteAddress.getAddress().getHostAddress());
    }
}

package com.gateway.smartrouter.filter;

import com.gateway.smartrouter.service.AllowedHostService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Reactive filter that validates the Host header for all {@code /WebServices/*} requests.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AllowedHostWebFilter implements WebFilter {

    private static final Logger log = LoggerFactory.getLogger(AllowedHostWebFilter.class);
    private static final String WEB_SERVICES_PREFIX = "/WebServices";

    private final AllowedHostService allowedHostService;
    private final ClientIpResolver clientIpResolver;

    public AllowedHostWebFilter(AllowedHostService allowedHostService, ClientIpResolver clientIpResolver) {
        this.allowedHostService = allowedHostService;
        this.clientIpResolver = clientIpResolver;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (!path.startsWith(WEB_SERVICES_PREFIX)) {
            return chain.filter(exchange);
        }

        ServerHttpRequest request = exchange.getRequest();
        String host = resolveHost(request);
        String clientIp = clientIpResolver.resolve(request);

        if (!allowedHostService.isHostAllowed(host)) {
            log.warn("Rejected request from host '{}' (client IP: {}) for path '{}'", host, clientIp, path);
            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
            return exchange.getResponse().setComplete();
        }

        log.debug("Allowed host '{}' (client IP: {}) for path '{}'", host, clientIp, path);
        return chain.filter(exchange);
    }

    private String resolveHost(ServerHttpRequest request) {
        String host = request.getHeaders().getFirst("Host");
        if (host != null && !host.isBlank()) {
            return host;
        }
        return request.getURI().getHost();
    }
}

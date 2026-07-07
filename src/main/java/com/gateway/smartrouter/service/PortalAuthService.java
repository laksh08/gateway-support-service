package com.gateway.smartrouter.service;

import com.gateway.smartrouter.model.AuthRequest;
import com.gateway.smartrouter.model.AuthResponse;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Mock Active Directory authentication for portal auditing.
 * Production deployments integrate with LDAP/AD.
 */
@Service
public class PortalAuthService {

    public Mono<AuthResponse> authenticate(AuthRequest request) {
        return Mono.fromCallable(() -> {
            String username = request.username().trim();
            String password = request.password();

            if (username.isEmpty() || password == null || password.isBlank()) {
                return new AuthResponse(false, null, null, "Username and password are required");
            }

            // Mock AD validation — accepts any non-empty credentials in dev
            return new AuthResponse(true, username, username, "Authenticated via mock AD");
        });
    }
}

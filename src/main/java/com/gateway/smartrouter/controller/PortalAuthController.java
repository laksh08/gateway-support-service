package com.gateway.smartrouter.controller;

import com.gateway.smartrouter.model.AuthRequest;
import com.gateway.smartrouter.model.AuthResponse;
import com.gateway.smartrouter.service.PortalAuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/portal/auth")
public class PortalAuthController {

    private final PortalAuthService portalAuthService;

    public PortalAuthController(PortalAuthService portalAuthService) {
        this.portalAuthService = portalAuthService;
    }

    @PostMapping("/login")
    public Mono<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        return portalAuthService.authenticate(request);
    }
}

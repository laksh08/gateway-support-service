package com.gateway.smartrouter.controller;

import com.gateway.smartrouter.model.AllowedHost;
import com.gateway.smartrouter.model.AllowedHostRequest;
import com.gateway.smartrouter.service.AllowedHostService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/portal/allowed-hosts")
public class PortalAllowedHostController {

    private final AllowedHostService allowedHostService;

    public PortalAllowedHostController(AllowedHostService allowedHostService) {
        this.allowedHostService = allowedHostService;
    }

    @GetMapping
    public Flux<AllowedHost> listAllowedHosts() {
        return allowedHostService.findAll();
    }

    @GetMapping("/{id}")
    public Mono<AllowedHost> getAllowedHost(@PathVariable Long id) {
        return allowedHostService.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Host not found")));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<AllowedHost> createAllowedHost(
            @Valid @RequestBody AllowedHostRequest request,
            @RequestHeader(value = "X-Portal-User", required = false) String portalUser) {
        return allowedHostService.create(request, requireUser(portalUser));
    }

    @PutMapping("/{id}")
    public Mono<AllowedHost> updateAllowedHost(
            @PathVariable Long id,
            @Valid @RequestBody AllowedHostRequest request,
            @RequestHeader(value = "X-Portal-User", required = false) String portalUser) {
        return allowedHostService.update(id, request, requireUser(portalUser))
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Host not found")));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteAllowedHost(
            @PathVariable Long id,
            @RequestHeader(value = "X-Portal-User", required = false) String portalUser) {
        requireUser(portalUser);
        return allowedHostService.delete(id);
    }

    @PostMapping("/refresh")
    public Mono<Map<String, Object>> refreshCache() {
        return allowedHostService.reload()
                .then(Mono.fromSupplier(() -> Map.<String, Object>of(
                        "status", "refreshed",
                        "activeHostCount", allowedHostService.getAllowedHosts().size()
                )));
    }

    private String requireUser(String portalUser) {
        if (portalUser == null || portalUser.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "AD login required. Provide X-Portal-User header.");
        }
        return portalUser.trim();
    }
}

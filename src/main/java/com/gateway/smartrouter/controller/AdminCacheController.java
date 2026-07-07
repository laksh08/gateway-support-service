package com.gateway.smartrouter.controller;

import com.gateway.smartrouter.service.AllowedHostService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/admin/cache")
public class AdminCacheController {

    private final AllowedHostService allowedHostService;

    public AdminCacheController(AllowedHostService allowedHostService) {
        this.allowedHostService = allowedHostService;
    }

    @PostMapping("/allowed-hosts/reload")
    public Mono<ResponseEntity<Map<String, Object>>> reloadAllowedHosts() {
        return allowedHostService.reload()
                .then(Mono.fromSupplier(() -> ResponseEntity.ok(Map.of(
                        "status", "reloaded",
                        "hostCount", allowedHostService.getAllowedHosts().size()
                ))));
    }
}

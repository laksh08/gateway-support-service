package com.gateway.smartrouter.controller;

import com.gateway.smartrouter.model.DashboardStats;
import com.gateway.smartrouter.service.GatewayMetricsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/portal/dashboard")
public class PortalDashboardController {

    private final GatewayMetricsService gatewayMetricsService;

    public PortalDashboardController(GatewayMetricsService gatewayMetricsService) {
        this.gatewayMetricsService = gatewayMetricsService;
    }

    @GetMapping
    public Mono<DashboardStats> getDashboardStats() {
        return Mono.fromSupplier(gatewayMetricsService::getDashboardStats);
    }
}

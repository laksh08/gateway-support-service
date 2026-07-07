package com.gateway.smartrouter.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayMetricsServiceTest {

    private GatewayMetricsService metricsService;

    @BeforeEach
    void setUp() {
        metricsService = new GatewayMetricsService();
    }

    @Test
    void recordsSuccessAndFailureMetrics() {
        metricsService.recordSuccess("getCustomer", 45);
        metricsService.recordSuccess("getCustomer", 55);
        metricsService.recordFailure("makePayment", 120, 500);

        var stats = metricsService.getDashboardStats();

        assertThat(stats.totalRequests()).isEqualTo(3);
        assertThat(stats.successCount()).isEqualTo(2);
        assertThat(stats.failureCount()).isEqualTo(1);
        assertThat(stats.topServices()).isNotEmpty();
        assertThat(stats.topServices().getFirst().serviceName()).isEqualTo("getCustomer");
    }
}

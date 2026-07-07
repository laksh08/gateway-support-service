package com.gateway.smartrouter.model;

import java.util.List;

public record DashboardStats(
        long totalRequests,
        long successCount,
        long failureCount,
        double successRate,
        double failureRate,
        double averageLatencyMs,
        double p95LatencyMs,
        List<ServiceCallStat> topServices,
        List<LatencyBucket> latencyDistribution
) {
    public record ServiceCallStat(String serviceName, long callCount, double averageLatencyMs) {
    }

    public record LatencyBucket(String range, long count) {
    }
}

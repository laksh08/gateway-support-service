package com.gateway.smartrouter.service;

import com.gateway.smartrouter.model.DashboardStats;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Lightweight in-memory gateway metrics collector (no Micrometer/OTel).
 */
@Service
public class GatewayMetricsService {

    private final AtomicLong totalRequests = new AtomicLong();
    private final AtomicLong successCount = new AtomicLong();
    private final AtomicLong failureCount = new AtomicLong();
    private final Map<String, ServiceMetrics> serviceMetrics = new ConcurrentHashMap<>();
    private final List<Long> recentLatencies = new ArrayList<>();
    private final ReentrantReadWriteLock latencyLock = new ReentrantReadWriteLock();
    private static final int MAX_LATENCY_SAMPLES = 10_000;

    public void recordSuccess(String serviceName, long latencyMs) {
        totalRequests.incrementAndGet();
        successCount.incrementAndGet();
        recordServiceCall(serviceName, latencyMs);
        recordLatency(latencyMs);
    }

    public void recordFailure(String serviceName, long latencyMs, int statusCode) {
        totalRequests.incrementAndGet();
        failureCount.incrementAndGet();
        String key = serviceName != null ? serviceName : "unknown";
        if (statusCode == 403) {
            key = "host-rejected";
        }
        recordServiceCall(key, latencyMs);
        recordLatency(latencyMs);
    }

    public void recordHostRejection(long latencyMs) {
        recordFailure("host-rejected", latencyMs, 403);
    }

    public DashboardStats getDashboardStats() {
        long total = totalRequests.get();
        long success = successCount.get();
        long failure = failureCount.get();

        double successRate = total == 0 ? 0.0 : (success * 100.0) / total;
        double failureRate = total == 0 ? 0.0 : (failure * 100.0) / total;

        List<Long> latencySnapshot = snapshotLatencies();
        double averageLatency = latencySnapshot.stream().mapToLong(Long::longValue).average().orElse(0.0);
        double p95 = percentile(latencySnapshot, 95);

        List<DashboardStats.ServiceCallStat> topServices = serviceMetrics.entrySet().stream()
                .map(entry -> new DashboardStats.ServiceCallStat(
                        entry.getKey(),
                        entry.getValue().callCount.get(),
                        entry.getValue().averageLatency()))
                .sorted(Comparator.comparingLong(DashboardStats.ServiceCallStat::callCount).reversed())
                .limit(10)
                .toList();

        return new DashboardStats(
                total,
                success,
                failure,
                round(successRate),
                round(failureRate),
                round(averageLatency),
                round(p95),
                topServices,
                buildLatencyDistribution(latencySnapshot)
        );
    }

    private void recordServiceCall(String serviceName, long latencyMs) {
        serviceMetrics.computeIfAbsent(serviceName, key -> new ServiceMetrics())
                .record(latencyMs);
    }

    private void recordLatency(long latencyMs) {
        latencyLock.writeLock().lock();
        try {
            recentLatencies.add(latencyMs);
            if (recentLatencies.size() > MAX_LATENCY_SAMPLES) {
                recentLatencies.removeFirst();
            }
        } finally {
            latencyLock.writeLock().unlock();
        }
    }

    private List<Long> snapshotLatencies() {
        latencyLock.readLock().lock();
        try {
            return List.copyOf(recentLatencies);
        } finally {
            latencyLock.readLock().unlock();
        }
    }

    private List<DashboardStats.LatencyBucket> buildLatencyDistribution(List<Long> latencies) {
        long b1 = 0, b2 = 0, b3 = 0, b4 = 0, b5 = 0;
        for (long latency : latencies) {
            if (latency < 50) {
                b1++;
            } else if (latency < 100) {
                b2++;
            } else if (latency < 250) {
                b3++;
            } else if (latency < 500) {
                b4++;
            } else {
                b5++;
            }
        }
        return List.of(
                new DashboardStats.LatencyBucket("0-49ms", b1),
                new DashboardStats.LatencyBucket("50-99ms", b2),
                new DashboardStats.LatencyBucket("100-249ms", b3),
                new DashboardStats.LatencyBucket("250-499ms", b4),
                new DashboardStats.LatencyBucket("500ms+", b5)
        );
    }

    private double percentile(List<Long> values, int percentile) {
        if (values.isEmpty()) {
            return 0.0;
        }
        List<Long> sorted = values.stream().sorted().toList();
        int index = (int) Math.ceil((percentile / 100.0) * sorted.size()) - 1;
        index = Math.max(0, Math.min(index, sorted.size() - 1));
        return sorted.get(index);
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private static final class ServiceMetrics {
        private final AtomicLong callCount = new AtomicLong();
        private final AtomicLong totalLatency = new AtomicLong();

        void record(long latencyMs) {
            callCount.incrementAndGet();
            totalLatency.addAndGet(latencyMs);
        }

        double averageLatency() {
            long count = callCount.get();
            return count == 0 ? 0.0 : (double) totalLatency.get() / count;
        }
    }
}

package gateway.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Component;

/**
 * Micrometer-based metrics for the gateway request pipeline.
 *
 * <h3>Metrics emitted</h3>
 * <ul>
 *   <li>{@code gateway.requests.total} – total requests received
 *   <li>{@code gateway.requests.success} – successfully proxied requests
 *   <li>{@code gateway.requests.errors} – failed requests (by error type)
 *   <li>{@code gateway.request.duration} – end-to-end latency
 *   <li>{@code gateway.discovery.instances} – number of healthy instances found
 * </ul>
 */
@Component
public class GatewayMetrics {

    private final MeterRegistry meterRegistry;
    private final Counter totalRequests;
    private final Counter successRequests;
    private final ConcurrentMap<String, Counter> errorCounters = new ConcurrentHashMap<>();
    private final Timer requestTimer;

    public GatewayMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        this.totalRequests =
                Counter.builder("gateway.requests.total")
                        .description("Total number of requests received by the gateway")
                        .register(meterRegistry);

        this.successRequests =
                Counter.builder("gateway.requests.success")
                        .description("Number of successfully proxied requests")
                        .register(meterRegistry);

        this.requestTimer =
                Timer.builder("gateway.request.duration")
                        .description("End-to-end request duration")
                        .publishPercentiles(0.5, 0.95, 0.99)
                        .register(meterRegistry);
    }

    public void recordRequest(String operation) {
        totalRequests.increment();
        Counter.builder("gateway.requests.by.operation")
                .tag("operation", operation)
                .register(meterRegistry)
                .increment();
    }

    public Sample startTimer() {
        return new Sample(Timer.start(meterRegistry));
    }

    public void recordSuccess(Sample sample, String service) {
        successRequests.increment();
        sample.timerSample.stop(requestTimer);
        Counter.builder("gateway.requests.success.by.service")
                .tag("service", service)
                .register(meterRegistry)
                .increment();
    }

    public void recordError(Sample sample, String service, String errorType) {
        sample.timerSample.stop(requestTimer);
        errorCounters
                .computeIfAbsent(
                        errorType,
                        type ->
                                Counter.builder("gateway.requests.errors")
                                        .tag("type", type)
                                        .tag("service", service)
                                        .register(meterRegistry))
                .increment();
    }

    public void recordDiscovery(String service, int instanceCount) {
        meterRegistry
                .gauge("gateway.discovery.instances", java.util.List.of(
                        io.micrometer.core.instrument.Tag.of("service", service)),
                        instanceCount);
    }

    /** Opaque timer sample used to record duration across asynchronous operations. */
    public record Sample(Timer.Sample timerSample) {}
}

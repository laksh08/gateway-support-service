package gateway.discovery;

import com.github.benmanes.caffeine.cache.AsyncCache;
import gateway.consul.ConsulClient;
import gateway.consul.model.ConsulServiceEntry;
import gateway.consul.model.ConsulServiceEntry.ConsulService;
import gateway.consul.model.ConsulWeights;
import gateway.metrics.GatewayMetrics;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Consul-backed service discovery with Caffeine caching.
 *
 * <h3>Discovery flow</h3>
 * <ol>
 *   <li>Check Caffeine {@link AsyncCache} for a cached list of instances.
 *   <li>On cache miss: call {@link ConsulClient#getHealthyInstances(String)}, which hits
 *       {@code GET /v1/health/service/{name}?passing=true} on the Consul HTTP API.
 *   <li>Parse each entry: extract {@code Service.Address || Node.Address} + {@code Service.Port}
 *       (both dynamically allocated by Nomad at scheduling time — never static).
 *   <li>Populate and return the cache.
 * </ol>
 *
 * <p>This class is the single point where the gateway obtains {@link ServiceInstance} objects.
 * All callers downstream receive instances whose address and port come directly from Consul.
 */
@Service
public class ConsulDiscoveryService implements ServiceDiscovery {

    private static final Logger log = LoggerFactory.getLogger(ConsulDiscoveryService.class);

    private final ConsulClient consulClient;
    private final AsyncCache<String, List<ServiceInstance>> discoveryCache;
    private final GatewayMetrics metrics;

    public ConsulDiscoveryService(
            ConsulClient consulClient,
            AsyncCache<String, List<ServiceInstance>> discoveryCache,
            GatewayMetrics metrics) {
        this.consulClient = consulClient;
        this.discoveryCache = discoveryCache;
        this.metrics = metrics;
    }

    /**
     * Returns all currently healthy instances of the named service.
     *
     * <p>Results are served from the Caffeine cache when available (TTL configured via
     * {@code gateway.cache.discovery-ttl}). On cache miss the Consul health API is queried.
     */
    @Override
    public Flux<ServiceInstance> discover(String serviceName) {
        return Mono.fromFuture(
                        discoveryCache.get(
                                serviceName,
                                (key, executor) ->
                                        fetchFromConsul(key).collectList().toFuture()))
                .doOnNext(
                        instances -> {
                            log.debug(
                                    "Discovery: {} → {} healthy instance(s)",
                                    serviceName,
                                    instances.size());
                            metrics.recordDiscovery(serviceName, instances.size());
                        })
                .flatMapMany(Flux::fromIterable);
    }

    /** Bypasses the cache and queries Consul directly; used for cache refresh. */
    public Mono<List<ServiceInstance>> refresh(String serviceName) {
        return fetchFromConsul(serviceName)
                .collectList()
                .doOnNext(
                        instances -> {
                            discoveryCache.put(
                                    serviceName, CompletableFuture.completedFuture(instances));
                            log.info(
                                    "Discovery cache refreshed for '{}': {} instance(s)",
                                    serviceName,
                                    instances.size());
                        });
    }

    /** Invalidates all cached discovery results, forcing a fresh Consul query on next call. */
    public void invalidateAll() {
        discoveryCache.synchronous().invalidateAll();
        log.info("Discovery cache invalidated");
    }

    /** Returns a snapshot of the current cache for health and diagnostic purposes. */
    public Map<String, List<ServiceInstance>> getCachedSnapshot() {
        return Map.copyOf(discoveryCache.synchronous().asMap());
    }

    private Flux<ServiceInstance> fetchFromConsul(String serviceName) {
        return consulClient
                .getHealthyInstances(serviceName)
                .map(entry -> toServiceInstance(serviceName, entry));
    }

    private ServiceInstance toServiceInstance(String serviceName, ConsulServiceEntry entry) {
        ConsulService svc = entry.service();

        String address = svc.effectiveAddress(entry.node());
        int port = svc.port();
        int weight =
                svc.weights() != null ? svc.weights().passing() : ConsulWeights.defaultWeights().passing();
        String datacenter =
                entry.node() != null ? entry.node().datacenter() : properties(entry);

        ServiceMetadata metadata = new ServiceMetadata(
                svc.meta() != null ? svc.meta() : Map.of());

        log.debug(
                "Discovered instance: service={} address={}:{} weight={} meta={}",
                serviceName,
                address,
                port,
                weight,
                metadata.raw());

        return new ServiceInstance(
                svc.id(),
                svc.name() != null ? svc.name() : serviceName,
                address,
                port,
                svc.tags() != null ? svc.tags() : List.of(),
                metadata,
                weight,
                entry.node() != null ? entry.node().name() : "",
                datacenter,
                svc.namespace() != null ? svc.namespace() : "");
    }

    private String properties(ConsulServiceEntry entry) {
        return entry.node() != null ? entry.node().datacenter() : "";
    }
}

package gateway.cache;

import com.github.benmanes.caffeine.cache.AsyncCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import gateway.config.GatewayProperties;
import gateway.discovery.ServiceInstance;
import gateway.routing.RouteDefinition;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Caffeine cache definitions for the gateway's two major caches:
 * <ol>
 *   <li>Route definitions (keyed by SOAP operation name)</li>
 *   <li>Service instances (keyed by Consul service name)</li>
 * </ol>
 *
 * <p>Both caches use {@link AsyncCache} to avoid blocking the Netty event loop.
 * Refresh tasks are submitted to a virtual-thread executor per the requirement that
 * <em>virtual threads are used only for background tasks</em>.
 */
@Configuration
public class CacheConfiguration {

    /**
     * Dedicated virtual-thread executor used only for background Caffeine refresh tasks.
     * Request handling remains on Netty event-loop threads.
     */
    @Bean(name = "virtualThreadCacheExecutor")
    public Executor virtualThreadCacheExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    /**
     * Caffeine cache for route definitions loaded from YAML or Consul KV.
     *
     * <p>Key: SOAP operation name. Value: resolved {@link RouteDefinition}.
     */
    @Bean
    public AsyncCache<String, RouteDefinition> routeCache(
            GatewayProperties props, Executor virtualThreadCacheExecutor) {
        return Caffeine.newBuilder()
                .expireAfterWrite(props.cache().routeTtl())
                .maximumSize(props.cache().maxSize())
                .executor(virtualThreadCacheExecutor)
                .buildAsync();
    }

    /**
     * Caffeine cache for Consul service discovery results.
     *
     * <p>Key: Consul service name. Value: list of healthy {@link ServiceInstance} records.
     */
    @Bean
    public AsyncCache<String, List<ServiceInstance>> discoveryCache(
            GatewayProperties props, Executor virtualThreadCacheExecutor) {
        return Caffeine.newBuilder()
                .expireAfterWrite(props.cache().discoveryTtl())
                .maximumSize(props.cache().maxSize())
                .executor(virtualThreadCacheExecutor)
                .buildAsync();
    }
}

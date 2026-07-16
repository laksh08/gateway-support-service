package gateway.proxy;

import gateway.discovery.ServiceInstance;
import gateway.discovery.ServiceMetadata;

/**
 * Constructs the full downstream URL from dynamic discovery data.
 *
 * <p>The URL must be built entirely from runtime values – never from hardcoded ports or addresses:
 *
 * <ul>
 *   <li>{@link ServiceInstance#address()} – IP or hostname of the Nomad node running the service
 *   <li>{@link ServiceInstance#port()} – port dynamically allocated by Nomad at scheduling time
 *   <li>Effective path – from metadata override or configured route targetPath
 * </ul>
 */
public interface TargetUrlBuilder {

    /**
     * Builds the downstream URL.
     *
     * @param instance      discovered service instance (address + port from Consul)
     * @param configuredPath route-level target path (may be overridden by metadata)
     * @param metadata      Consul service metadata (may provide {@code soap-context} override)
     * @return full HTTP URL (e.g. {@code http://10.20.15.7:28934/soap/customer})
     */
    String build(ServiceInstance instance, String configuredPath, ServiceMetadata metadata);
}

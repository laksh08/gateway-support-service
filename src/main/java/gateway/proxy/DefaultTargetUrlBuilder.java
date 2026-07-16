package gateway.proxy;

import gateway.discovery.MetadataResolver;
import gateway.discovery.ServiceInstance;
import gateway.discovery.ServiceMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Builds dynamic target URLs from Consul-discovered instance data.
 *
 * <h3>URL construction</h3>
 * <pre>
 *   scheme  = http (HTTPS termination handled by Envoy/Consul Connect mTLS)
 *   host    = ServiceInstance.address()  (dynamically resolved from Consul catalog)
 *   port    = ServiceInstance.port()     (dynamically allocated by Nomad, never hardcoded)
 *   path    = metadata.soap-context OR routeConfig.targetPath
 *
 *   Result: http://{address}:{port}/{effectivePath}
 * </pre>
 *
 * <p>This class never assumes any port value. Both the host and port come exclusively from
 * {@link ServiceInstance} which in turn is populated from the Consul health API response.
 */
@Component
public class DefaultTargetUrlBuilder implements TargetUrlBuilder {

    private static final Logger log = LoggerFactory.getLogger(DefaultTargetUrlBuilder.class);

    private final MetadataResolver metadataResolver;

    public DefaultTargetUrlBuilder(MetadataResolver metadataResolver) {
        this.metadataResolver = metadataResolver;
    }

    @Override
    public String build(ServiceInstance instance, String configuredPath, ServiceMetadata metadata) {
        String host = instance.address();
        int port = instance.port();
        String path = metadataResolver.resolveTargetPath(metadata, configuredPath);

        // Normalise path to always start with /
        if (path != null && !path.isEmpty() && !path.startsWith("/")) {
            path = "/" + path;
        }

        String url = "http://" + host + ":" + port + (path != null ? path : "");
        log.debug(
                "Built target URL: {} [service={}, node={}]",
                url,
                instance.serviceName(),
                instance.node());
        return url;
    }
}

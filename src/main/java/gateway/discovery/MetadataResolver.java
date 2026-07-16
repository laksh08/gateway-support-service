package gateway.discovery;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Utility for resolving effective values from Consul service metadata with fallbacks.
 *
 * <p>Metadata keys are not mandatory; this class centralises the priority logic:
 * <ol>
 *   <li>Service instance metadata (from Consul {@code meta} block)
 *   <li>Route-level configuration
 *   <li>Gateway default
 * </ol>
 */
@Component
public class MetadataResolver {

    /**
     * Resolves the effective upstream path.
     *
     * <p>Priority: {@code soap-context} in metadata → configured route targetPath.
     */
    public String resolveTargetPath(ServiceMetadata metadata, String configuredPath) {
        return metadata.soapContext().orElse(configuredPath);
    }

    /**
     * Resolves the effective per-request timeout.
     *
     * <p>Priority: {@code timeout} in metadata → configured route timeout → gateway default 30s.
     */
    public Duration resolveTimeout(ServiceMetadata metadata, Duration configuredTimeout) {
        return metadata.timeout().orElse(configuredTimeout);
    }

    /**
     * Reads a raw metadata value by key, returning an {@link Optional}.
     */
    public Optional<String> resolve(ServiceMetadata metadata, String key) {
        return metadata.get(key);
    }

    /**
     * Returns the entire raw metadata map for inspection or custom resolution.
     */
    public Map<String, String> rawMetadata(ServiceMetadata metadata) {
        return metadata.raw();
    }
}

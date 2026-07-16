package gateway.discovery;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

/**
 * Typed view over Consul service metadata tags ({@code meta} block in service registration).
 *
 * <p>All fields are optional — Consul metadata is freeform and no key is required. Callers
 * use the typed accessors which return {@link Optional} so missing keys are handled explicitly.
 *
 * <h3>Conventional metadata keys recognised by this gateway</h3>
 * <pre>
 *   api-version  – semantic version of the SOAP API (e.g. "v2")
 *   soap-context – upstream path override (e.g. "/soap/customer")
 *   protocol     – transport protocol hint (e.g. "soap", "json")
 *   timeout      – ISO-8601 duration string (e.g. "PT30S")
 *   owner        – team or domain owner label
 *   region       – deployment region (e.g. "ap-south")
 * </pre>
 */
public record ServiceMetadata(Map<String, String> raw) {

    public ServiceMetadata {
        raw = raw != null ? Map.copyOf(raw) : Map.of();
    }

    public static ServiceMetadata empty() {
        return new ServiceMetadata(Map.of());
    }

    public Optional<String> get(String key) {
        return Optional.ofNullable(raw.get(key));
    }

    public Optional<String> apiVersion() {
        return get("api-version");
    }

    /**
     * Upstream path override – when present, replaces the route's configured {@code targetPath}.
     */
    public Optional<String> soapContext() {
        return get("soap-context");
    }

    public Optional<String> protocol() {
        return get("protocol");
    }

    /**
     * Per-instance timeout override.  Parsed from ISO-8601 duration string (e.g. {@code PT45S}).
     */
    public Optional<Duration> timeout() {
        return get("timeout").map(Duration::parse);
    }

    public Optional<String> owner() {
        return get("owner");
    }

    public Optional<String> region() {
        return get("region");
    }
}

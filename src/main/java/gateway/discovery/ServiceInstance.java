package gateway.discovery;

import java.util.List;

/**
 * Immutable snapshot of a single healthy Consul service instance.
 *
 * <p>All fields are read directly from the Consul catalog and health API response.
 * The gateway uses {@link #address()} and {@link #port()} to build the dynamic target URL –
 * both values come from Consul and are never hardcoded.
 *
 * @param serviceId   unique identifier of this registration (e.g. {@code customer-svc-abc123})
 * @param serviceName logical Consul service name (e.g. {@code customer-service})
 * @param address     IP or hostname of the node running this instance (Nomad dynamic allocation)
 * @param port        dynamically allocated port (assigned by Nomad at scheduling time)
 * @param tags        Consul service tags
 * @param metadata    structured view of the Consul {@code meta} block
 * @param weight      Consul service weight (used by {@link gateway.loadbalancer.WeightedLoadBalancer})
 * @param node        Nomad / Consul node name
 * @param datacenter  Consul datacenter
 * @param namespace   Consul namespace (empty string if namespaces not enabled)
 */
public record ServiceInstance(
        String serviceId,
        String serviceName,
        String address,
        int port,
        List<String> tags,
        ServiceMetadata metadata,
        int weight,
        String node,
        String datacenter,
        String namespace) {

    public ServiceInstance {
        tags = tags != null ? List.copyOf(tags) : List.of();
        metadata = metadata != null ? metadata : ServiceMetadata.empty();
        namespace = namespace != null ? namespace : "";
    }

    /** Human-readable identifier for logging. */
    public String displayId() {
        return serviceName + "@" + address + ":" + port + " [" + serviceId + "]";
    }
}

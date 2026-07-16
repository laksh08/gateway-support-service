package gateway.consul.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * Full health-check response entry from Consul's health API:
 * {@code GET /v1/health/service/{name}?passing=true}.
 *
 * <p>Each entry contains the Node, Service, and Checks sections.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ConsulServiceEntry(
        @JsonProperty("Node") ConsulNode node,
        @JsonProperty("Service") ConsulService service,
        @JsonProperty("Checks") List<ConsulCheck> checks) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ConsulNode(
            @JsonProperty("ID") String id,
            @JsonProperty("Node") String name,
            @JsonProperty("Address") String address,
            @JsonProperty("Datacenter") String datacenter) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ConsulService(
            @JsonProperty("ID") String id,
            @JsonProperty("Service") String name,
            @JsonProperty("Address") String address,
            @JsonProperty("Port") int port,
            @JsonProperty("Tags") List<String> tags,
            @JsonProperty("Meta") Map<String, String> meta,
            @JsonProperty("Weights") ConsulWeights weights,
            @JsonProperty("Namespace") String namespace) {

        /** Returns the service address if set, otherwise falls back to the node address. */
        public String effectiveAddress(ConsulNode parentNode) {
            String svcAddr = address();
            if (svcAddr != null && !svcAddr.isBlank()) {
                return svcAddr;
            }
            return parentNode != null ? parentNode.address() : "";
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ConsulCheck(
            @JsonProperty("Status") String status,
            @JsonProperty("Name") String name,
            @JsonProperty("CheckID") String checkId) {}
}

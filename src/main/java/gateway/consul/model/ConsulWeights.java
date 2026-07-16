package gateway.consul.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Consul service weights used by the weighted load balancer. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ConsulWeights(
        @JsonProperty("Passing") int passing, @JsonProperty("Warning") int warning) {

    public static ConsulWeights defaultWeights() {
        return new ConsulWeights(1, 0);
    }
}

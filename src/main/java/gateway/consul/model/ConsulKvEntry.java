package gateway.consul.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** JSON model for a single entry returned by the Consul KV API. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ConsulKvEntry(
        @JsonProperty("Key") String key,
        @JsonProperty("Value") String value,
        @JsonProperty("Flags") long flags,
        @JsonProperty("CreateIndex") long createIndex,
        @JsonProperty("ModifyIndex") long modifyIndex) {}

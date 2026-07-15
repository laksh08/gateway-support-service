package com.gateway.smartrouter.routing;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RouteTargetTest {

    @Test
    void parsesServiceNameOnly() {
        RouteTarget t = RouteTarget.parse("customer-service");
        assertThat(t.host()).isEqualTo("customer-service");
        assertThat(t.upstreamPath()).isNull();
        assertThat(t.port()).isEqualTo(0);
        assertThat(t.hasUpstreamPath()).isFalse();
        assertThat(t.hasPort()).isFalse();
    }

    @Test
    void parsesServiceNameWithPath() {
        RouteTarget t = RouteTarget.parse("customer-service/soap/CustomerService");
        assertThat(t.host()).isEqualTo("customer-service");
        assertThat(t.upstreamPath()).isEqualTo("/soap/CustomerService");
        assertThat(t.hasUpstreamPath()).isTrue();
        assertThat(t.port()).isEqualTo(0);
    }

    @Test
    void parsesServiceNameWithPort() {
        RouteTarget t = RouteTarget.parse("customer-service:9091");
        assertThat(t.host()).isEqualTo("customer-service");
        assertThat(t.port()).isEqualTo(9091);
        assertThat(t.hasPort()).isTrue();
        assertThat(t.upstreamPath()).isNull();
    }

    @Test
    void parsesServiceNameWithPortAndPath() {
        RouteTarget t = RouteTarget.parse("customer-service:9091/soap/CustomerService");
        assertThat(t.host()).isEqualTo("customer-service");
        assertThat(t.port()).isEqualTo(9091);
        assertThat(t.upstreamPath()).isEqualTo("/soap/CustomerService");
    }

    @Test
    void parsesIpWithPort() {
        RouteTarget t = RouteTarget.parse("127.0.0.1:9091/soap/Service");
        assertThat(t.host()).isEqualTo("127.0.0.1");
        assertThat(t.port()).isEqualTo(9091);
        assertThat(t.upstreamPath()).isEqualTo("/soap/Service");
    }

    @Test
    void preservesRawValue() {
        String raw = "customer-service:9091/soap/CustomerService";
        RouteTarget t = RouteTarget.parse(raw);
        assertThat(t.rawValue()).isEqualTo(raw);
        assertThat(t.toString()).isEqualTo(raw);
    }

    @ParameterizedTest
    @CsvSource({" , blank value", "'', blank value"})
    void rejectsBlankValue(String value, String desc) {
        assertThatThrownBy(() -> RouteTarget.parse(value))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsInvalidPort() {
        assertThatThrownBy(() -> RouteTarget.parse("customer-service:abc"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid port");
    }
}

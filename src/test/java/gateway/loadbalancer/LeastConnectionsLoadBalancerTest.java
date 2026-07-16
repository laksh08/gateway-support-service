package gateway.loadbalancer;

import gateway.discovery.ServiceInstance;
import gateway.discovery.ServiceMetadata;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LeastConnectionsLoadBalancerTest {

    private LeastConnectionsLoadBalancer lb;

    @BeforeEach
    void setUp() {
        lb = new LeastConnectionsLoadBalancer();
    }

    @Test
    void selectsInstanceWithFewestConnections() {
        ServiceInstance a = instance("a", 8001);
        ServiceInstance b = instance("b", 8002);

        lb.trackStart(a.serviceId());
        lb.trackStart(a.serviceId());

        // a has 2 connections, b has 0 — b should be selected
        ServiceInstance selected = lb.select(List.of(a, b)).orElseThrow();
        Assertions.assertThat(selected.address()).isEqualTo("b");
    }

    @Test
    void trackEndDecrementsActiveCount() {
        ServiceInstance a = instance("a", 8001);
        lb.trackStart(a.serviceId());
        lb.trackStart(a.serviceId());

        Assertions.assertThat(lb.getActiveCount(a.serviceId())).isEqualTo(2);

        lb.trackEnd(a.serviceId());
        Assertions.assertThat(lb.getActiveCount(a.serviceId())).isEqualTo(1);
    }

    @Test
    void returnsEmptyForEmptyList() {
        Assertions.assertThat(lb.select(List.of())).isEmpty();
    }

    @Test
    void strategyName() {
        Assertions.assertThat(lb.name()).isEqualTo("least-connections");
    }

    private ServiceInstance instance(String address, int port) {
        return new ServiceInstance(
                "id-" + address, "svc", address, port, List.of(), ServiceMetadata.empty(), 1, "n", "dc1", "");
    }
}

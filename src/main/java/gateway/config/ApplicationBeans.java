package gateway.config;

import gateway.consul.ConsulProperties;
import gateway.loadbalancer.LeastConnectionsLoadBalancer;
import gateway.loadbalancer.LoadBalancer;
import gateway.loadbalancer.RandomLoadBalancer;
import gateway.loadbalancer.RoundRobinLoadBalancer;
import gateway.loadbalancer.WeightedLoadBalancer;
import gateway.routing.ConsulKVRouteResolver;
import gateway.routing.RouteResolver;
import gateway.routing.YamlRouteResolver;
import tools.jackson.databind.ObjectMapper;
import io.netty.channel.ChannelOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

/**
 * Factory beans for pluggable strategy components: {@link RouteResolver} and {@link LoadBalancer}.
 *
 * <p>The active implementation is selected via configuration:
 * <ul>
 *   <li>{@code gateway.route-resolver=yaml} → {@link YamlRouteResolver}
 *   <li>{@code gateway.route-resolver=consul-kv} → {@link ConsulKVRouteResolver}
 *   <li>{@code gateway.load-balancer=round-robin} → {@link RoundRobinLoadBalancer}
 *   <li>{@code gateway.load-balancer=random} → {@link RandomLoadBalancer}
 *   <li>{@code gateway.load-balancer=least-connections} → {@link LeastConnectionsLoadBalancer}
 *   <li>{@code gateway.load-balancer=weighted} → {@link WeightedLoadBalancer}
 * </ul>
 */
@Configuration
public class ApplicationBeans {

    private static final Logger log = LoggerFactory.getLogger(ApplicationBeans.class);

    @Bean
    public RouteResolver routeResolver(
            GatewayProperties gatewayProperties,
            ConsulProperties consulProperties,
            ObjectMapper objectMapper) {

        String resolverType = gatewayProperties.routeResolver();
        log.info("Route resolver: {}", resolverType);
        return switch (resolverType.toLowerCase()) {
            case "consul-kv" -> {
                WebClient consulWebClient = buildConsulWebClient(consulProperties);
                yield new ConsulKVRouteResolver(consulWebClient, consulProperties, objectMapper);
            }
            default -> {
                if (!"yaml".equalsIgnoreCase(resolverType)) {
                    log.warn("Unknown route-resolver '{}', defaulting to 'yaml'", resolverType);
                }
                yield new YamlRouteResolver(gatewayProperties);
            }
        };
    }

    @Bean
    public LoadBalancer loadBalancer(
            GatewayProperties properties,
            RoundRobinLoadBalancer roundRobin,
            RandomLoadBalancer random,
            LeastConnectionsLoadBalancer leastConnections,
            WeightedLoadBalancer weighted) {

        String strategy = properties.loadBalancer();
        log.info("Load balancer strategy: {}", strategy);
        return switch (strategy.toLowerCase()) {
            case "random" -> random;
            case "least-connections" -> leastConnections;
            case "weighted" -> weighted;
            default -> {
                if (!"round-robin".equalsIgnoreCase(strategy)) {
                    log.warn("Unknown load-balancer '{}', defaulting to 'round-robin'", strategy);
                }
                yield roundRobin;
            }
        };
    }

    private WebClient buildConsulWebClient(ConsulProperties consulProperties) {
        HttpClient httpClient =
                HttpClient.create()
                        .option(
                                ChannelOption.CONNECT_TIMEOUT_MILLIS,
                                (int) consulProperties.connectTimeout().toMillis())
                        .responseTimeout(consulProperties.readTimeout());

        WebClient.Builder builder =
                WebClient.builder()
                        .baseUrl(consulProperties.baseUrl())
                        .clientConnector(new ReactorClientHttpConnector(httpClient));

        if (consulProperties.token() != null && !consulProperties.token().isBlank()) {
            builder = builder.defaultHeader("X-Consul-Token", consulProperties.token());
        }
        return builder.build();
    }
}

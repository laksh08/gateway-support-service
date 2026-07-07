package com.gateway.smartrouter.config;

import com.gateway.smartrouter.routing.ConsulRoutingProvider;
import com.gateway.smartrouter.routing.RoutingCache;
import com.gateway.smartrouter.routing.RoutingProvider;
import com.gateway.smartrouter.routing.YamlRoutingProvider;
import io.netty.channel.ChannelOption;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;

@Configuration
@EnableScheduling
@EnableConfigurationProperties({
        RoutingProperties.class,
        ForwardingProperties.class,
        RoutesProperties.class
})
public class ApplicationConfiguration {

    @Bean
    public RoutingProvider routingProvider(
            RoutingProperties routingProperties,
            RoutesProperties routesProperties,
            RoutingCache routingCache) {

        return switch (routingProperties.provider().toLowerCase()) {
            case "consul" -> new ConsulRoutingProvider(routingProperties, routingCache);
            case "yaml" -> new YamlRoutingProvider(routesProperties, routingCache);
            default -> throw new IllegalStateException(
                    "Unsupported routing provider: " + routingProperties.provider());
        };
    }

    @Bean
    public WebClient forwardingWebClient(ForwardingProperties forwardingProperties) {
        ConnectionProvider connectionProvider = ConnectionProvider.builder("forwarding-pool")
                .maxConnections(forwardingProperties.maxConnections())
                .maxIdleTime(Duration.ofSeconds(forwardingProperties.maxIdleTimeSeconds()))
                .build();

        HttpClient httpClient = HttpClient.create(connectionProvider)
                .responseTimeout(Duration.ofMillis(forwardingProperties.readTimeoutMs()))
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, forwardingProperties.connectTimeoutMs());

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}

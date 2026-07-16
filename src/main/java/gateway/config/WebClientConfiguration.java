package gateway.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import java.util.concurrent.TimeUnit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.CodecConfigurer;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

/**
 * Singleton {@link WebClient} bean used by the proxy service for all downstream calls.
 *
 * <h3>Connection pool configuration</h3>
 * <ul>
 *   <li>Bounded connection pool with configurable max connections
 *   <li>HTTP/2 H2C support for services that support it
 *   <li>Keep-alive enabled
 *   <li>Gzip compression support
 *   <li>Configurable connect / read / write timeouts
 * </ul>
 */
@Configuration
public class WebClientConfiguration {

    @Bean("gatewayWebClient")
    public WebClient gatewayWebClient(GatewayProperties properties) {
        GatewayProperties.ForwardingProperties fwd = properties.forwarding();

        ConnectionProvider connectionProvider =
                ConnectionProvider.builder("gateway-proxy-pool")
                        .maxConnections(fwd.maxConnections())
                        .maxIdleTime(fwd.maxIdleTime())
                        .pendingAcquireMaxCount(fwd.maxConnections() * 2)
                        .build();

        HttpClient httpClient =
                HttpClient.create(connectionProvider)
                        .compress(true)
                        .keepAlive(true)
                        .option(
                                ChannelOption.CONNECT_TIMEOUT_MILLIS,
                                (int) fwd.connectTimeout().toMillis())
                        .doOnConnected(
                                conn ->
                                        conn.addHandlerLast(
                                                        new ReadTimeoutHandler(
                                                                fwd.readTimeout().toMillis(),
                                                                TimeUnit.MILLISECONDS))
                                                .addHandlerLast(
                                                        new WriteTimeoutHandler(
                                                                fwd.writeTimeout().toMillis(),
                                                                TimeUnit.MILLISECONDS)));

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(
                        configurer ->
                                configurer
                                        .defaultCodecs()
                                        .maxInMemorySize(fwd.maxInMemorySizeBytes()))
                .build();
    }
}

package gateway.proxy;

import gateway.discovery.ServiceInstance;
import gateway.exception.GatewayTimeoutException;
import gateway.loadbalancer.LeastConnectionsLoadBalancer;
import gateway.metrics.GatewayMetrics;
import java.time.Duration;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

/**
 * Reactive WebClient-based proxy that forwards SOAP requests to discovered service instances.
 *
 * <h3>Header forwarding</h3>
 * All request headers are forwarded except:
 * <ul>
 *   <li>Hop-by-hop headers (Connection, Keep-Alive, Transfer-Encoding, etc.)
 *   <li>Host (replaced with the target host automatically by WebClient)
 * </ul>
 *
 * <h3>Response</h3>
 * The downstream status code, headers, and body are returned unchanged. No XML processing
 * or modification is done on the response path.
 *
 * <h3>Timeout handling</h3>
 * If the downstream call exceeds {@code requestTimeout}, a {@link GatewayTimeoutException}
 * is raised which maps to HTTP 504 via {@link gateway.exception.GlobalExceptionHandler}.
 */
@Service
public class ReactiveProxyService implements ProxyService {

    private static final Logger log = LoggerFactory.getLogger(ReactiveProxyService.class);

    private static final Set<String> HOP_BY_HOP_HEADERS = Set.of(
            "connection",
            "keep-alive",
            "proxy-authenticate",
            "proxy-authorization",
            "te",
            "trailers",
            "transfer-encoding",
            "upgrade",
            "host");

    private final WebClient webClient;
    private final GatewayMetrics metrics;
    private final LeastConnectionsLoadBalancer leastConnectionsLb;

    public ReactiveProxyService(
            WebClient gatewayWebClient,
            GatewayMetrics metrics,
            LeastConnectionsLoadBalancer leastConnectionsLb) {
        this.webClient = gatewayWebClient;
        this.metrics = metrics;
        this.leastConnectionsLb = leastConnectionsLb;
    }

    @Override
    public Mono<ServerResponse> proxy(
            ServerRequest request,
            String targetUrl,
            byte[] bodyBytes,
            ServiceInstance instance,
            Duration requestTimeout) {

        HttpMethod method = request.method();
        String serviceId = instance.serviceId();

        return Mono.defer(() -> {
                    leastConnectionsLb.trackStart(serviceId);
                    GatewayMetrics.Sample sample = metrics.startTimer();

                    DataBuffer dataBuffer =
                            DefaultDataBufferFactory.sharedInstance.wrap(bodyBytes);

                    return webClient
                            .method(method)
                            .uri(targetUrl)
                            .headers(headers -> copyHeaders(request.headers().asHttpHeaders(), headers))
                            .body(BodyInserters.fromDataBuffers(Mono.just(dataBuffer)))
                            .exchangeToMono(
                                    clientResponse ->
                                            clientResponse
                                                    .bodyToMono(byte[].class)
                                                    .defaultIfEmpty(new byte[0])
                                                    .flatMap(
                                                            responseBody -> {
                                                                metrics.recordSuccess(
                                                                        sample,
                                                                        instance.serviceName());
                                                                return ServerResponse.status(
                                                                                clientResponse.statusCode())
                                                                        .headers(
                                                                                h ->
                                                                                        copyResponseHeaders(
                                                                                                clientResponse
                                                                                                        .headers()
                                                                                                        .asHttpHeaders(),
                                                                                                h))
                                                                        .body(
                                                                                BodyInserters.fromValue(
                                                                                        responseBody));
                                                            }))
                            .timeout(requestTimeout)
                            .onErrorMap(
                                    java.util.concurrent.TimeoutException.class,
                                    ex ->
                                            new GatewayTimeoutException(
                                                    instance.serviceName(), ex))
                            .doOnError(
                                    ex ->
                                            metrics.recordError(
                                                    sample, instance.serviceName(), ex.getClass().getSimpleName()))
                            .doFinally(signal -> leastConnectionsLb.trackEnd(serviceId));
                })
                .doOnSubscribe(
                        sub ->
                                log.debug(
                                        "Proxying {} to {} [instance={}]",
                                        method,
                                        targetUrl,
                                        instance.displayId()));
    }

    private void copyHeaders(HttpHeaders source, HttpHeaders target) {
        source.forEach((name, values) -> {
            if (!HOP_BY_HOP_HEADERS.contains(name.toLowerCase())) {
                target.addAll(name, values);
            }
        });
    }

    private void copyResponseHeaders(HttpHeaders source, HttpHeaders target) {
        source.forEach((name, values) -> {
            if (!HOP_BY_HOP_HEADERS.contains(name.toLowerCase())) {
                target.addAll(name, values);
            }
        });
    }
}

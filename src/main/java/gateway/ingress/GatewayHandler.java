package gateway.ingress;

import gateway.discovery.ConsulDiscoveryService;
import gateway.discovery.MetadataResolver;
import gateway.discovery.ServiceInstance;
import gateway.exception.NoHealthyInstanceException;
import gateway.exception.RouteNotFoundException;
import gateway.loadbalancer.LoadBalancer;
import gateway.metrics.GatewayMetrics;
import gateway.parser.SoapOperation;
import gateway.parser.SoapOperationExtractor;
import gateway.proxy.ProxyService;
import gateway.proxy.TargetUrlBuilder;
import gateway.routing.RouteDefinition;
import gateway.routing.RouteResolver;
import gateway.validation.AllowedHostValidator;
import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

/**
 * Core request handler for all {@code /WebServices/**} SOAP requests.
 *
 * <h3>Request pipeline</h3>
 * <ol>
 *   <li>Validate the {@code Host} header against the allowed-hosts list
 *   <li>Read and buffer the raw request body (needed for both parsing and forwarding)
 *   <li>Extract the SOAP operation name using StAX streaming parser
 *   <li>Resolve the route (service name + target path + timeout)
 *   <li>Discover all healthy instances via Consul catalog API
 *   <li>Select one instance with the configured load-balancing strategy
 *   <li>Build the dynamic target URL from the discovered address:port
 *   <li>Proxy the original request body unchanged to the target URL
 *   <li>Return the downstream response as-is
 * </ol>
 *
 * <p>No SOAP body modification occurs at any step.  The gateway is transparent.
 */
@Component
public class GatewayHandler {

    private static final Logger log = LoggerFactory.getLogger(GatewayHandler.class);

    private final AllowedHostValidator hostValidator;
    private final SoapOperationExtractor soapExtractor;
    private final RouteResolver routeResolver;
    private final ConsulDiscoveryService discoveryService;
    private final LoadBalancer loadBalancer;
    private final TargetUrlBuilder urlBuilder;
    private final ProxyService proxyService;
    private final MetadataResolver metadataResolver;
    private final GatewayMetrics metrics;
    private final int maxBodySize;

    public GatewayHandler(
            AllowedHostValidator hostValidator,
            SoapOperationExtractor soapExtractor,
            RouteResolver routeResolver,
            ConsulDiscoveryService discoveryService,
            LoadBalancer loadBalancer,
            TargetUrlBuilder urlBuilder,
            ProxyService proxyService,
            MetadataResolver metadataResolver,
            GatewayMetrics metrics,
            gateway.config.GatewayProperties gatewayProperties) {
        this.hostValidator = hostValidator;
        this.soapExtractor = soapExtractor;
        this.routeResolver = routeResolver;
        this.discoveryService = discoveryService;
        this.loadBalancer = loadBalancer;
        this.urlBuilder = urlBuilder;
        this.proxyService = proxyService;
        this.metadataResolver = metadataResolver;
        this.metrics = metrics;
        this.maxBodySize = gatewayProperties.forwarding().maxInMemorySizeBytes();
    }

    public Mono<ServerResponse> handle(ServerRequest request) {
        return hostValidator
                .validate(request)
                .then(readBodyBytes(request))
                .flatMap(
                        bodyBytes ->
                                soapExtractor
                                        .extract(new ByteArrayInputStream(bodyBytes))
                                        .flatMap(op -> processRequest(request, op, bodyBytes)));
    }

    private Mono<ServerResponse> processRequest(
            ServerRequest request, SoapOperation op, byte[] bodyBytes) {
        metrics.recordRequest(op.localName());
        log.info(
                "[{}] SOAP operation={} ns={} ver={}",
                request.exchange().getRequest().getId(),
                op.localName(),
                op.namespace(),
                op.soapVersion());

        return routeResolver
                .resolve(op.localName())
                .switchIfEmpty(Mono.error(new RouteNotFoundException(op.localName())))
                .flatMap(
                        route ->
                                discoveryService
                                        .discoverAll(route.serviceName())
                                        .flatMap(
                                                instances ->
                                                        selectAndProxy(
                                                                request, bodyBytes, route, instances)));
    }

    private Mono<ServerResponse> selectAndProxy(
            ServerRequest request,
            byte[] bodyBytes,
            RouteDefinition route,
            List<ServiceInstance> instances) {

        return Mono.defer(() -> {
            ServiceInstance instance =
                    loadBalancer
                            .select(instances)
                            .orElseThrow(() -> new NoHealthyInstanceException(route.serviceName()));

            Duration timeout = metadataResolver.resolveTimeout(
                    instance.metadata(), route.timeout());
            String targetUrl = urlBuilder.build(
                    instance, route.targetPath(), instance.metadata());

            log.info(
                    "Routing {} → {} [lb={} instance={}]",
                    route.operation(),
                    targetUrl,
                    loadBalancer.name(),
                    instance.displayId());

            return proxyService.proxy(request, targetUrl, bodyBytes, instance, timeout);
        });
    }

    private Mono<byte[]> readBodyBytes(ServerRequest request) {
        return request.exchange()
                .getRequest()
                .getBody()
                .map(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);
                    return bytes;
                })
                .reduce(
                        new java.io.ByteArrayOutputStream(),
                        (baos, chunk) -> {
                            try {
                                baos.write(chunk);
                            } catch (java.io.IOException e) {
                                throw new gateway.exception.InvalidSoapRequestException(
                                        "Failed to read request body", e);
                            }
                            return baos;
                        })
                .map(java.io.ByteArrayOutputStream::toByteArray);
    }
}

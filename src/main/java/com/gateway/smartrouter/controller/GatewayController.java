package com.gateway.smartrouter.controller;

import com.gateway.smartrouter.parser.ServiceMethodExtractor;
import com.gateway.smartrouter.service.GatewayMetricsService;
import com.gateway.smartrouter.service.RequestForwardingService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.io.ByteArrayInputStream;

@RestController
public class GatewayController {

    private final ServiceMethodExtractor serviceMethodExtractor;
    private final RequestForwardingService requestForwardingService;
    private final GatewayMetricsService gatewayMetricsService;

    public GatewayController(
            ServiceMethodExtractor serviceMethodExtractor,
            RequestForwardingService requestForwardingService,
            GatewayMetricsService gatewayMetricsService) {
        this.serviceMethodExtractor = serviceMethodExtractor;
        this.requestForwardingService = requestForwardingService;
        this.gatewayMetricsService = gatewayMetricsService;
    }

    @PostMapping(
            value = "/WebServices/Gateway/CBISvc",
            consumes = {MediaType.TEXT_XML_VALUE, MediaType.APPLICATION_XML_VALUE, "application/soap+xml"}
    )
    public Mono<ResponseEntity<byte[]>> handleGatewayRequest(
            @RequestBody byte[] requestBody,
            ServerWebExchange exchange) {

        long start = System.currentTimeMillis();
        String serviceMethod;
        try {
            serviceMethod = serviceMethodExtractor.extract(new ByteArrayInputStream(requestBody));
        } catch (RuntimeException ex) {
            gatewayMetricsService.recordFailure("parse-error", System.currentTimeMillis() - start, 400);
            throw ex;
        }

        return requestForwardingService.forward(exchange, serviceMethod, requestBody)
                .doOnSuccess(response -> {
                    long latency = System.currentTimeMillis() - start;
                    int status = response.getStatusCode().value();
                    if (status >= 200 && status < 400) {
                        gatewayMetricsService.recordSuccess(serviceMethod, latency);
                    } else {
                        gatewayMetricsService.recordFailure(serviceMethod, latency, status);
                    }
                })
                .doOnError(error -> gatewayMetricsService.recordFailure(
                        serviceMethod, System.currentTimeMillis() - start, 500));
    }
}

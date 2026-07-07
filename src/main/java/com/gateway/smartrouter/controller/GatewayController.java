package com.gateway.smartrouter.controller;

import com.gateway.smartrouter.parser.ServiceMethodExtractor;
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

    public GatewayController(
            ServiceMethodExtractor serviceMethodExtractor,
            RequestForwardingService requestForwardingService) {
        this.serviceMethodExtractor = serviceMethodExtractor;
        this.requestForwardingService = requestForwardingService;
    }

    @PostMapping(
            value = "/WebServices/Gateway/CBISvc",
            consumes = {MediaType.TEXT_XML_VALUE, MediaType.APPLICATION_XML_VALUE, "application/soap+xml"}
    )
    public Mono<ResponseEntity<byte[]>> handleGatewayRequest(
            @RequestBody byte[] requestBody,
            ServerWebExchange exchange) {

        String serviceMethod = serviceMethodExtractor.extract(new ByteArrayInputStream(requestBody));
        return requestForwardingService.forward(exchange, serviceMethod, requestBody);
    }
}

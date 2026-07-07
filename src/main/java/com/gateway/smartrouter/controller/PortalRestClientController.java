package com.gateway.smartrouter.controller;

import com.gateway.smartrouter.model.RestClientExecuteRequest;
import com.gateway.smartrouter.model.RestClientExecuteResponse;
import com.gateway.smartrouter.service.PortalRestClientService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/portal/rest-client")
public class PortalRestClientController {

    private final PortalRestClientService portalRestClientService;

    public PortalRestClientController(PortalRestClientService portalRestClientService) {
        this.portalRestClientService = portalRestClientService;
    }

    @PostMapping("/execute")
    public Mono<RestClientExecuteResponse> execute(@Valid @RequestBody RestClientExecuteRequest request) {
        return portalRestClientService.execute(request);
    }
}

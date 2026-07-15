package com.gateway.smartrouter.model;

import jakarta.validation.constraints.NotBlank;

/**
 * API model for managing route entries.
 *
 * @param serviceMethod SOAP service method name (e.g. {@code getCustomer})
 * @param routeValue    Route target string: {@code serviceName[/upstreamPath][:port]}
 *                      (e.g. {@code customer-service/soap/CustomerService})
 */
public record RouteEntry(
        String serviceMethod,
        @NotBlank String routeValue,
        String resolvedHost,
        String upstreamPath,
        int port
) {
}

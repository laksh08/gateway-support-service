package com.gateway.smartrouter.exception;

public class RouteNotFoundException extends RuntimeException {

    public RouteNotFoundException(String serviceMethod) {
        super("No route configured for serviceMethod: " + serviceMethod);
    }
}

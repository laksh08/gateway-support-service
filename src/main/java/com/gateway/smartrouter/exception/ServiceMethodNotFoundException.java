package com.gateway.smartrouter.exception;

public class ServiceMethodNotFoundException extends RuntimeException {

    public ServiceMethodNotFoundException(String message) {
        super(message);
    }

    public ServiceMethodNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}

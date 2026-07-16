package gateway.exception;

import org.springframework.http.HttpStatus;

/** Raised when the gateway cannot reach the Consul HTTP API. */
public final class ConsulCommunicationException extends GatewayException {

    public ConsulCommunicationException(String message, Throwable cause) {
        super(HttpStatus.SERVICE_UNAVAILABLE, "CONSUL_UNREACHABLE", message, cause);
    }
}

package gateway.exception;

import org.springframework.http.HttpStatus;

/** Raised when a downstream request exceeds its configured timeout. */
public final class GatewayTimeoutException extends GatewayException {

    public GatewayTimeoutException(String serviceName, Throwable cause) {
        super(
                HttpStatus.GATEWAY_TIMEOUT,
                "GATEWAY_TIMEOUT",
                "Request to service timed out: " + serviceName,
                cause);
    }
}

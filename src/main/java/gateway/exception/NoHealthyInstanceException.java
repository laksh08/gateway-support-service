package gateway.exception;

import org.springframework.http.HttpStatus;

/** Raised when Consul has instances registered but none are currently passing health checks. */
public final class NoHealthyInstanceException extends GatewayException {

    public NoHealthyInstanceException(String serviceName) {
        super(
                HttpStatus.SERVICE_UNAVAILABLE,
                "NO_HEALTHY_INSTANCE",
                "No healthy instance available in Consul for service: " + serviceName);
    }
}

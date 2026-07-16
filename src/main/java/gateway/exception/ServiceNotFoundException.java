package gateway.exception;

import org.springframework.http.HttpStatus;

/** Raised when Consul returns no registered instances for the requested service. */
public final class ServiceNotFoundException extends GatewayException {

    public ServiceNotFoundException(String serviceName) {
        super(
                HttpStatus.NOT_FOUND,
                "SERVICE_NOT_FOUND",
                "No instances registered in Consul for service: " + serviceName);
    }
}

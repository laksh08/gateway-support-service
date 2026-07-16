package gateway.exception;

import org.springframework.http.HttpStatus;

/** Raised when no route is configured for the extracted SOAP operation. */
public final class RouteNotFoundException extends GatewayException {

    public RouteNotFoundException(String operation) {
        super(
                HttpStatus.NOT_FOUND,
                "ROUTE_NOT_FOUND",
                "No route configured for SOAP operation: " + operation);
    }
}

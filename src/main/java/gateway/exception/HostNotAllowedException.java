package gateway.exception;

import org.springframework.http.HttpStatus;

/** Raised when the request's Host header is not in the allowed-hosts list. */
public final class HostNotAllowedException extends GatewayException {

    public HostNotAllowedException(String host) {
        super(HttpStatus.FORBIDDEN, "HOST_NOT_ALLOWED", "Host not allowed: " + host);
    }
}

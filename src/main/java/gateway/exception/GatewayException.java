package gateway.exception;

import org.springframework.http.HttpStatus;

/** Base class for all gateway-specific exceptions. */
public abstract class GatewayException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;

    protected GatewayException(HttpStatus status, String errorCode, String message) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    protected GatewayException(
            HttpStatus status, String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.errorCode = errorCode;
    }

    public HttpStatus status() {
        return status;
    }

    public String errorCode() {
        return errorCode;
    }
}

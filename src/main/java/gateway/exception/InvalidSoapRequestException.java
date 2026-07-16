package gateway.exception;

import org.springframework.http.HttpStatus;

/** Raised when the incoming request body is not valid SOAP XML. */
public final class InvalidSoapRequestException extends GatewayException {

    public InvalidSoapRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, "INVALID_SOAP_REQUEST", message);
    }

    public InvalidSoapRequestException(String message, Throwable cause) {
        super(HttpStatus.BAD_REQUEST, "INVALID_SOAP_REQUEST", message, cause);
    }
}

package gateway.parser;

import java.io.InputStream;
import reactor.core.publisher.Mono;

/**
 * Extracts the SOAP operation from a raw XML input stream without deserialising the envelope.
 *
 * <p>Implementations MUST:
 * <ul>
 *   <li>Use streaming XML processing (StAX or equivalent).
 *   <li>Stop parsing immediately after finding the operation element.
 *   <li>Never map XML into Java DTOs.
 *   <li>Never block the calling thread.
 * </ul>
 */
public interface SoapOperationExtractor {

    /**
     * Extracts the SOAP operation name and metadata from {@code xmlStream}.
     *
     * @param xmlStream raw request body as a byte stream (must not be closed by the caller before
     *                  the returned Mono terminates)
     * @return a {@link Mono} that emits the parsed {@link SoapOperation} or errors with
     *         {@link gateway.exception.InvalidSoapRequestException} when the body is not valid SOAP
     */
    Mono<SoapOperation> extract(InputStream xmlStream);
}

package gateway.parser;

import gateway.exception.InvalidSoapRequestException;
import java.io.InputStream;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * StAX-based SOAP operation extractor.
 *
 * <p>Parsing strategy:
 * <ol>
 *   <li>Walk {@code START_ELEMENT} events until a {@code Body} element in a known SOAP namespace
 *       is found.
 *   <li>The very next {@code START_ELEMENT} is the operation element – its local name and
 *       namespace URI are captured.
 *   <li>The reader is closed immediately; the rest of the document is never parsed.
 * </ol>
 *
 * <p>This implementation is safe to use from reactive pipelines; StAX parsing is offloaded to
 * {@link Schedulers#boundedElastic()} so the Netty event loop is never blocked.
 */
@Component
public class StaxSoapOperationExtractor implements SoapOperationExtractor {

    private static final Logger log = LoggerFactory.getLogger(StaxSoapOperationExtractor.class);

    private final XMLInputFactory xmlInputFactory;

    public StaxSoapOperationExtractor() {
        XMLInputFactory factory = XMLInputFactory.newInstance();
        factory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.TRUE);
        factory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.FALSE);
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
        this.xmlInputFactory = factory;
    }

    @Override
    public Mono<SoapOperation> extract(InputStream xmlStream) {
        return Mono.fromCallable(() -> doParse(xmlStream))
                .subscribeOn(Schedulers.boundedElastic());
    }

    private SoapOperation doParse(InputStream xmlStream) {
        XMLStreamReader reader = null;
        try {
            reader = xmlInputFactory.createXMLStreamReader(xmlStream);
            SoapVersion detectedVersion = null;
            boolean insideBody = false;

            while (reader.hasNext()) {
                int event = reader.next();

                if (event != XMLStreamConstants.START_ELEMENT) {
                    continue;
                }

                String localName = reader.getLocalName();
                String ns = reader.getNamespaceURI();

                if ("Body".equals(localName) && SoapVersion.isSoapNamespace(ns)) {
                    insideBody = true;
                    detectedVersion =
                            SoapVersion.SOAP_11.namespaceUri().equals(ns)
                                    ? SoapVersion.SOAP_11
                                    : SoapVersion.SOAP_12;
                    continue;
                }

                if (insideBody && !SoapVersion.isSoapNamespace(ns)) {
                    SoapOperation op = new SoapOperation(localName, ns, detectedVersion);
                    log.debug("Extracted SOAP operation: {} [ns={}, ver={}]", localName, ns, detectedVersion);
                    return op;
                }
            }

            throw new InvalidSoapRequestException(
                    "Could not find SOAP operation element inside <Body>");

        } catch (InvalidSoapRequestException ex) {
            throw ex;
        } catch (XMLStreamException ex) {
            throw new InvalidSoapRequestException("Failed to parse SOAP XML: " + ex.getMessage(), ex);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (XMLStreamException ignored) {
                    // best-effort close
                }
            }
        }
    }
}

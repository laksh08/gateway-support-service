package com.gateway.smartrouter.parser;

import com.gateway.smartrouter.exception.ServiceMethodNotFoundException;
import org.springframework.stereotype.Component;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.InputStream;

/**
 * Streaming StAX parser that extracts the {@code serviceMethod} element value
 * and stops immediately after finding it. Namespace-safe, minimal allocations.
 */
@Component
public class ServiceMethodExtractor {

    private static final String SERVICE_METHOD_ELEMENT = "serviceMethod";

    private final XMLInputFactory inputFactory;

    public ServiceMethodExtractor() {
        this.inputFactory = XMLInputFactory.newInstance();
        this.inputFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.TRUE);
        this.inputFactory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);
        this.inputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
        this.inputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
    }

    public String extract(InputStream inputStream) {
        XMLStreamReader reader = null;
        try {
            reader = inputFactory.createXMLStreamReader(inputStream);
            while (reader.hasNext()) {
                int event = reader.next();
                if (event == XMLStreamConstants.START_ELEMENT
                        && SERVICE_METHOD_ELEMENT.equals(reader.getLocalName())) {
                    String value = readElementText(reader);
                    if (value == null || value.isBlank()) {
                        throw new ServiceMethodNotFoundException("serviceMethod element is empty");
                    }
                    return value.trim();
                }
            }
            throw new ServiceMethodNotFoundException("serviceMethod element not found in request");
        } catch (XMLStreamException e) {
            throw new ServiceMethodNotFoundException("Failed to parse XML request: " + e.getMessage(), e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (XMLStreamException ignored) {
                    // reader already consumed or closed
                }
            }
        }
    }

    private String readElementText(XMLStreamReader reader) throws XMLStreamException {
        if (reader.next() == XMLStreamConstants.CHARACTERS) {
            return reader.getText();
        }
        if (reader.getEventType() == XMLStreamConstants.END_ELEMENT) {
            return "";
        }
        return null;
    }
}

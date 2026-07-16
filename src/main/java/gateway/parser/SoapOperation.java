package gateway.parser;

/**
 * Immutable result of parsing a SOAP envelope.
 *
 * <p>Only the data needed for routing is extracted; the original XML body is never mutated.
 *
 * @param localName  local name of the first element inside {@code <soap:Body>}
 *                   – this is the SOAP operation name (e.g. {@code createCustomer})
 * @param namespace  XML namespace URI of the operation element (e.g. {@code http://example.com/customer})
 * @param soapVersion detected SOAP version from the envelope namespace
 */
public record SoapOperation(String localName, String namespace, SoapVersion soapVersion) {}

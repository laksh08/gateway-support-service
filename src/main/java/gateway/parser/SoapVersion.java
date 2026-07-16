package gateway.parser;

/** SOAP envelope namespace URIs used to detect the protocol version during StAX parsing. */
public enum SoapVersion {
    SOAP_11("http://schemas.xmlsoap.org/soap/envelope/"),
    SOAP_12("http://www.w3.org/2003/05/soap-envelope");

    private final String namespaceUri;

    SoapVersion(String namespaceUri) {
        this.namespaceUri = namespaceUri;
    }

    public String namespaceUri() {
        return namespaceUri;
    }

    public static boolean isSoapNamespace(String uri) {
        return SOAP_11.namespaceUri.equals(uri) || SOAP_12.namespaceUri.equals(uri);
    }
}

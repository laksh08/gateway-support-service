package gateway.parser;

import gateway.exception.InvalidSoapRequestException;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import reactor.test.StepVerifier;

class StaxSoapOperationExtractorTest {

    private StaxSoapOperationExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new StaxSoapOperationExtractor();
    }

    @Test
    void extractsSoap11Operation() {
        String xml = """
                <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                                  xmlns:cust="http://example.com/customer">
                  <soapenv:Header/>
                  <soapenv:Body>
                    <cust:createCustomer>
                      <name>John Doe</name>
                    </cust:createCustomer>
                  </soapenv:Body>
                </soapenv:Envelope>
                """;

        StepVerifier.create(extractor.extract(toStream(xml)))
                .assertNext(op -> {
                    Assertions.assertThat(op.localName()).isEqualTo("createCustomer");
                    Assertions.assertThat(op.namespace()).isEqualTo("http://example.com/customer");
                    Assertions.assertThat(op.soapVersion()).isEqualTo(SoapVersion.SOAP_11);
                })
                .verifyComplete();
    }

    @Test
    void extractsSoap12Operation() {
        String xml = """
                <soap:Envelope xmlns:soap="http://www.w3.org/2003/05/soap-envelope"
                               xmlns:ord="http://example.com/order">
                  <soap:Body>
                    <ord:createOrder>
                      <orderId>123</orderId>
                    </ord:createOrder>
                  </soap:Body>
                </soap:Envelope>
                """;

        StepVerifier.create(extractor.extract(toStream(xml)))
                .assertNext(op -> {
                    Assertions.assertThat(op.localName()).isEqualTo("createOrder");
                    Assertions.assertThat(op.soapVersion()).isEqualTo(SoapVersion.SOAP_12);
                })
                .verifyComplete();
    }

    @Test
    void stopsParsingImmediatelyAfterOperation() {
        // Body with large payload after the operation element should still parse correctly
        String xml = """
                <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/"
                            xmlns:t="http://example.com/test">
                  <s:Body>
                    <t:testOperation>
                      <payload>%s</payload>
                    </t:testOperation>
                  </s:Body>
                </s:Envelope>
                """.formatted("X".repeat(100_000));

        StepVerifier.create(extractor.extract(toStream(xml)))
                .assertNext(op -> Assertions.assertThat(op.localName()).isEqualTo("testOperation"))
                .verifyComplete();
    }

    @Test
    void errorOnEmptyBody() {
        String xml = """
                <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/">
                  <s:Body/>
                </s:Envelope>
                """;

        StepVerifier.create(extractor.extract(toStream(xml)))
                .expectError(InvalidSoapRequestException.class)
                .verify();
    }

    @Test
    void errorOnInvalidXml() {
        StepVerifier.create(extractor.extract(toStream("NOT XML AT ALL")))
                .expectError(InvalidSoapRequestException.class)
                .verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"<root/>", "<Envelope><Body><op/></Body></Envelope>"})
    void errorWhenNoSoapNamespace(String xml) {
        // Element named Body but without the SOAP namespace is not recognised
        StepVerifier.create(extractor.extract(toStream(xml)))
                .expectError(InvalidSoapRequestException.class)
                .verify();
    }

    private ByteArrayInputStream toStream(String xml) {
        return new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
    }
}

package com.gateway.smartrouter.parser;

import com.gateway.smartrouter.exception.ServiceMethodNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ServiceMethodExtractorTest {

    private ServiceMethodExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new ServiceMethodExtractor();
    }

    @Test
    void extractsServiceMethodFromSimpleRequest() {
        String xml = """
                <Request>
                    <serviceMethod>getCustomer</serviceMethod>
                </Request>
                """;

        String result = extractor.extract(toStream(xml));

        assertThat(result).isEqualTo("getCustomer");
    }

    @Test
    void extractsServiceMethodWithNamespace() {
        String xml = """
                <ns:Request xmlns:ns="http://example.com/gateway">
                    <ns:serviceMethod>makePayment</ns:serviceMethod>
                </ns:Request>
                """;

        String result = extractor.extract(toStream(xml));

        assertThat(result).isEqualTo("makePayment");
    }

    @Test
    void stopsParsingAfterFindingServiceMethod() {
        String xml = """
                <Request>
                    <serviceMethod>issuePolicy</serviceMethod>
                    <largePayload>should not be fully parsed for routing</largePayload>
                </Request>
                """;

        String result = extractor.extract(toStream(xml));

        assertThat(result).isEqualTo("issuePolicy");
    }

    @ParameterizedTest
    @ValueSource(strings = {"<Request></Request>", "<Request><other>value</other></Request>"})
    void throwsWhenServiceMethodMissing(String xml) {
        assertThatThrownBy(() -> extractor.extract(toStream(xml)))
                .isInstanceOf(ServiceMethodNotFoundException.class);
    }

    @Test
    void throwsWhenServiceMethodEmpty() {
        String xml = "<Request><serviceMethod>  </serviceMethod></Request>";

        assertThatThrownBy(() -> extractor.extract(toStream(xml)))
                .isInstanceOf(ServiceMethodNotFoundException.class)
                .hasMessageContaining("empty");
    }

    private ByteArrayInputStream toStream(String xml) {
        return new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
    }
}

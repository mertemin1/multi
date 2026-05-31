import net.jqwik.api.*;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RESPONSE mesajlarinin map/filter/reduce ve Optional ile
 * islenisini gosteren birim ve property-based testler.
 */
public class ResponseProcessingTest {

    @Property
    void parseResponseShouldBeCaseInsensitiveAndTrimmed(@ForAll("responseVariants") String raw) {
        Optional<Boolean> parsed = ResponseProcessing.parseResponse(raw);
        assertTrue(parsed.isPresent());
        assertTrue(parsed.get());
    }

    @Provide
    Arbitrary<String> responseVariants() {
        List<String> samples = Arrays.asList(
                "RESPONSE",
                " response",
                "RESPONSE ",
                " response ",
                "ReSpOnSe",
                "\tRESPONSE\n"
        );
        return Arbitraries.of(samples);
    }

    @Property
    void nonResponseStringsShouldReturnEmpty(@ForAll("nonResponseStrings") String raw) {
        Optional<Boolean> parsed = ResponseProcessing.parseResponse(raw);
        assertFalse(parsed.isPresent());
    }

    @Provide
    Arbitrary<String> nonResponseStrings() {
        List<String> samples = Arrays.asList(
                "",
                "OK",
                "RESPONS",
                "RESP0NSE",
                "ERROR",
                "PING",
                "HELLO"
        );
        return Arbitraries.of(samples);
    }

    @Property
    void countValidResponsesShouldMatchNumberOfParsableElements(@ForAll("mixedMessages") List<String> messages) {
        long expected = messages.stream()
                .map(ResponseProcessing::parseResponse)
                .filter(Optional::isPresent)
                .count();

        long actual = ResponseProcessing.countValidResponses(messages);

        assertEquals(expected, actual);
    }

    @Provide
    Arbitrary<List<String>> mixedMessages() {
        return Arbitraries.oneOf(responseVariants(), nonResponseStrings())
                .list()
                .ofMinSize(0)
                .ofMaxSize(50);
    }

    @Test
    void firstValidResponseShouldReturnFirstParsableResponse() {
        List<String> messages = Arrays.asList("OK", "  RESPONSE ", "RESPONSE", "ERROR");

        Optional<String> first = ResponseProcessing.firstValidResponse(messages);

        assertTrue(first.isPresent());
        assertEquals("  RESPONSE ", first.get());
    }
}


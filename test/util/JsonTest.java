package util;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class JsonTest {
    @Test void exactNumbersEscapesAndStableFormatting() throws Exception {
        Map<String, Object> input = new HashMap<>();
        input.put("z", List.of(Long.MAX_VALUE, Long.MIN_VALUE, Double.MIN_VALUE, -0.0));
        input.put("á", "\"\\\b\f\n\r\t\u0000🧑\ud800");
        String encoded = Json.escrever(input);
        Map<?, ?> result = (Map<?, ?>) Json.ler(encoded);
        assertEquals(input.get("á"), result.get("á"));
        List<?> values = (List<?>) result.get("z");
        assertEquals(Long.MAX_VALUE, ((BigDecimal) values.get(0)).longValueExact());
        assertEquals(Long.MIN_VALUE, ((BigDecimal) values.get(1)).longValueExact());
        assertEquals(Double.MIN_VALUE, ((BigDecimal) values.get(2)).doubleValue());
        assertEquals(Long.MIN_VALUE, Double.doubleToRawLongBits((Double) values.get(3)));
        assertEquals(encoded, Json.escrever(result));
        assertTrue(encoded.indexOf("\"z\"") < encoded.indexOf("\"á\""));
    }

    @ParameterizedTest @ValueSource(strings = {"01", "-", "1.", "1e", "+1", "NaN", "Infinity",
            "[1,]", "{\"a\":1,}", "{\"a\":null,\"a\":2}", "true false", "\"\\q\"", "\"\n\"", "\"\\uQQQQ\"", "\"\\u１２３４\""})
    void rejectsInvalidSyntax(String input) {
        assertThrows(IOException.class, () -> Json.ler(input));
    }

    @Test void limitsNesting() {
        assertThrows(IOException.class, () -> Json.ler("[".repeat(130) + "0" + "]".repeat(130)));
    }
}

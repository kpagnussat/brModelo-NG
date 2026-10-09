package util;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

class TemaDoSistemaTest {
    // Guards forced Portuguese/English theme values and case folding independent of desktop settings.
    @ParameterizedTest @CsvSource({"escuro,true", "dark,true", "ESCURO,true", "DARK,true", "claro,false", "light,false", "CLARO,false", "LIGHT,false"})
    void forcedValues(String value, boolean expected) {
        String previous = System.getProperty("brmodelo.tema");
        try {
            System.setProperty("brmodelo.tema", value);
            assertEquals(expected, TemaDoSistema.escuro());
        } finally {
            if (previous == null) System.clearProperty("brmodelo.tema"); else System.setProperty("brmodelo.tema", previous);
        }
    }
}

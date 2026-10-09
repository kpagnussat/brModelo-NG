package brmodelo;

import controlador.Diagrama;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.*;

class RoundTripTest {
    // Guards both the historical fixture semantics and lossless saving of every diagram type.
    @ParameterizedTest(name = "{0}")
    @MethodSource("brmodelo.Fixtures#names")
    void roundTrip(String name) throws Exception {
        Diagrama first = Fixtures.load(name);
        String before = CanonicalDump.dump(first);
        assertEquals(Fixtures.expected(name), before, "Committed fixture semantics");
        assertEquals(before, CanonicalDump.dump(Fixtures.load(Fixtures.save(first))));
    }
}

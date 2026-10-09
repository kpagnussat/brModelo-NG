package brmodelo;

import controlador.Diagrama;
import controlador.apoios.GuardaPadraoBrM;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import util.FormatoBrMj;
import util.LeitorSeguro;
import static org.junit.jupiter.api.Assertions.*;

class JsonRoundTripTest {
    @ParameterizedTest(name = "{0}")
    @MethodSource("brmodelo.Fixtures#names")
    void preservesEveryFixture(String name) throws Exception {
        GuardaPadraoBrM envelope;
        try (LeitorSeguro in = new LeitorSeguro(new ByteArrayInputStream(Fixtures.bytes(name)))) {
            envelope = (GuardaPadraoBrM) in.readObject();
        }
        Diagrama original = Fixtures.load(name);
        byte[] json = FormatoBrMj.escrever(original, envelope);
        assertArrayEquals(json, FormatoBrMj.escrever(original, envelope));
        assertTrue(new String(json, StandardCharsets.UTF_8).endsWith("\n"));
        var loaded = FormatoBrMj.ler(json);
        assertEquals(CanonicalDump.dump(original), CanonicalDump.dump(loaded.diagrama()));
        assertArrayEquals(json, FormatoBrMj.escrever(loaded.diagrama(), loaded.envelope()));
        assertEquals(envelope.Tag, loaded.envelope().Tag);
        assertEquals(envelope.versaoDiagrama, loaded.envelope().versaoDiagrama);
        assertEquals(envelope.autor, loaded.envelope().autor);
        assertEquals(envelope.data, loaded.envelope().data);
        assertEquals(envelope.versao, loaded.envelope().versao);
        assertEquals(CanonicalDump.dump(original), CanonicalDump.dump(Fixtures.load(Fixtures.save(loaded.paraBrM3()))));
    }

    @Test void rejectsDisallowedClassAndUnknownField() throws Exception {
        String json = new String(FormatoBrMj.escrever(Fixtures.load("livre")), StandardCharsets.UTF_8);
        var denied = assertThrows(java.io.InvalidClassException.class,
                () -> FormatoBrMj.ler(json.replace("diagramas.livre.DiagramaLivre", "java.util.PriorityQueue").getBytes(StandardCharsets.UTF_8)));
        assertEquals("java.util.PriorityQueue", denied.classname);
        String withUnknown = json.replaceFirst("\"campos\": \\{", "\"campos\": {\"doesNotExist\": null,");
        var failure = assertThrows(java.io.IOException.class, () -> FormatoBrMj.ler(withUnknown.getBytes(StandardCharsets.UTF_8)));
        assertTrue(failure.getMessage().contains("Unknown field"), failure.getMessage());
    }
}

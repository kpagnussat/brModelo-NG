package brmodelo;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SerializedFormTest {
    @Test void diagramDescriptorsCannotReferToRetiredHelp() throws Exception {
        for (String block : SerializedForm.dump().split("(?m)(?=^class )")) {
            String name = block.lines().findFirst().orElse("");
            if (name.equals("class controlador.Diagrama") || name.startsWith("class desenho.")
                    || name.startsWith("class diagramas.") || name.startsWith("class controlador.apoios."))
                assertFalse(block.contains("Lhelper/"), name);
        }
        assertTrue(java.lang.reflect.Modifier.isTransient(controlador.Diagrama.class.getDeclaredField("master").getModifiers()),
                "UI editor and window are excluded from diagram serialization");
    }

    @Test void matchesGolden() throws Exception {
        try (var in = getClass().getResourceAsStream("/serialized-form.txt")) {
            assertNotNull(in, "Serialized-form golden must exist");
            assertEquals(new String(in.readAllBytes(), StandardCharsets.UTF_8), SerializedForm.dump(),
                    "Serialized form changed; review compatibility before intentionally regenerating");
        }
    }
}

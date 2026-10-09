package brmodelo;

import controlador.Diagrama;
import java.io.*;
import java.net.URLClassLoader;
import java.nio.file.Path;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

class OfficialCompatibilityTest {
    private static Object read(byte[] bytes, ClassLoader loader) throws Exception {
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes)) {
            @Override protected Class<?> resolveClass(ObjectStreamClass desc) throws ClassNotFoundException {
                return Class.forName(desc.getName(), false, loader);
            }
        }) {
            return in.readObject();
        }
    }

    // Guards fork-to-official and official-to-fork serialization without sharing application classes.
    @ParameterizedTest(name = "{0}")
    @MethodSource("brmodelo.Fixtures#names")
    void compatibleInBothDirections(String name) throws Exception {
        String jar = System.getProperty("oficialJar");
        assumeTrue(jar != null && !jar.isBlank(), "Set -PoficialJar=/path/to/official/brModelo.jar");
        Diagrama original = Fixtures.load(name);
        verify(original, jar);
        var converted = util.FormatoBrMj.ler(util.FormatoBrMj.escrever(original));
        verify(converted.diagrama(), jar, Fixtures.save(converted.paraBrM3()));
    }

    static void verify(Diagrama fork, String jar) throws Exception {
        verify(fork, jar, Fixtures.save(fork));
    }

    static void verify(Diagrama fork, String jar, byte[] savedBytes) throws Exception {
        try (URLClassLoader loader = new URLClassLoader(new java.net.URL[]{Path.of(jar).toUri().toURL()},
                ClassLoader.getPlatformClassLoader())) {
            Object officialEnvelope = read(savedBytes, loader);
            Object official = officialEnvelope.getClass().getMethod("getDiagrama").invoke(officialEnvelope);
            assertNotNull(official, "Official nested stream must deserialize");
            Class<?> diagramClass = loader.loadClass("controlador.Diagrama");
            assertTrue(diagramClass.isInstance(official));
            assertSame(loader, official.getClass().getClassLoader());
            assertEquals(fork.getClass().getName(), official.getClass().getName());
            var items = (java.util.List<?>) diagramClass.getMethod("getListaDeItens").invoke(official);
            assertEquals(fork.getListaDeItens().size(), items.size());
            // The official constructor calls its own Diagrama.SaveToStream, then we write its envelope.
            Object saved = loader.loadClass("controlador.apoios.GuardaPadraoBrM")
                    .getConstructor(diagramClass).newInstance(official);
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (ObjectOutputStream out = new ObjectOutputStream(bytes)) { out.writeObject(saved); }
            assertEquals(CanonicalDump.dump(fork), CanonicalDump.dump(Fixtures.load(bytes.toByteArray())));
        }
    }
}

package brmodelo;

import java.io.*;
import java.lang.reflect.Proxy;
import java.util.*;
import javax.management.BadAttributeValueExpException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import util.LeitorSeguro;
import util.ReadWitness;
import static org.junit.jupiter.api.Assertions.*;

class SafeReaderTest {
    // Guards rejection at descriptor resolution, before queue/comparator or proxy-handler callbacks.
    @ParameterizedTest
    @ValueSource(strings = {"queue", "exception", "proxy"})
    void rejectsBeforeConstruction(String kind) throws Exception {
        Object payload = switch (kind) {
            case "queue" -> {
                PriorityQueue<String> queue = new PriorityQueue<>(new ReadWitness());
                queue.add("Aluno"); queue.add("Curso");
                yield queue;
            }
            case "exception" -> new BadAttributeValueExpException("invented");
            case "proxy" -> Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[]{Runnable.class}, new ReadWitness());
            default -> throw new AssertionError(kind);
        };
        String rejected = switch (kind) {
            case "queue" -> "java.util.PriorityQueue";
            case "exception" -> "javax.management.BadAttributeValueExpException";
            default -> "java.lang.reflect.Proxy";
        };
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) { out.writeObject(payload); }
        ReadWitness.reads = 0;
        List<String> resolved = new ArrayList<>();
        try (LeitorSeguro in = new LeitorSeguro(new ByteArrayInputStream(bytes.toByteArray())) {
            @Override protected Class<?> resolveClass(ObjectStreamClass descriptor)
                    throws IOException, ClassNotFoundException {
                Class<?> result = super.resolveClass(descriptor);
                resolved.add(descriptor.getName());
                return result;
            }
        }) {
            InvalidClassException failure = assertThrows(InvalidClassException.class, in::readObject);
            assertEquals(rejected, failure.classname);
            assertFalse(resolved.contains(rejected), "Disallowed class was never resolved");
            assertEquals(0, ReadWitness.reads, "No nested callback may run");
        }
    }

    // Guards acceptance of both envelope and nested model descriptors for all supported diagram types.
    @ParameterizedTest(name = "{0}")
    @MethodSource("brmodelo.Fixtures#names")
    void acceptsFixtures(String name) throws Exception {
        assertNotNull(Fixtures.load(name));
    }
}

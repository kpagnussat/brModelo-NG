package brmodelo;

import controlador.Diagrama;
import controlador.apoios.GuardaPadraoBrM;
import java.io.*;
import java.nio.file.*;
import java.util.stream.Stream;
import util.LeitorSeguro;

final class Fixtures {
    private Fixtures() {}

    static Stream<String> names() {
        return Stream.of("conceitual", "logico", "fluxo", "atividade", "eap", "livre");
    }

    static byte[] bytes(String name) throws IOException {
        try (InputStream in = Fixtures.class.getResourceAsStream("/fixtures/" + name + ".brM3")) {
            if (in == null) throw new FileNotFoundException(name);
            return in.readAllBytes();
        }
    }

    static Diagrama load(byte[] bytes) throws Exception {
        try (LeitorSeguro in = new LeitorSeguro(new ByteArrayInputStream(bytes))) {
            GuardaPadraoBrM envelope = (GuardaPadraoBrM) in.readObject();
            // Check the nested stream explicitly: the application's accessor swallows failures.
            var field = GuardaPadraoBrM.class.getDeclaredField("diagrama");
            field.setAccessible(true);
            try (LeitorSeguro nested = new LeitorSeguro(new ByteArrayInputStream((byte[]) field.get(envelope)))) {
                return (Diagrama) nested.readObject();
            }
        }
    }

    static Diagrama load(String name) throws Exception { return load(bytes(name)); }

    static byte[] save(Diagrama diagram) throws IOException {
        // Same envelope and version metadata as Diagrama.Salvar, without GUI/autosave effects.
        GuardaPadraoBrM envelope = new GuardaPadraoBrM(diagram);
        envelope.versaoDiagrama = Diagrama.VERSAO_A + "." + Diagrama.VERSAO_B + "." + Diagrama.VERSAO_C;
        return save(envelope);
    }

    static byte[] save(GuardaPadraoBrM envelope) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) { out.writeObject(envelope); }
        return bytes.toByteArray();
    }

    static String expected(String name) throws IOException {
        try (InputStream in = Fixtures.class.getResourceAsStream("/fixtures/" + name + ".txt")) {
            if (in == null) throw new FileNotFoundException(name + ".txt");
            return new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        }
    }
}

package brmodelo;

import controlador.apoios.GuardaPadraoBrM;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;
import util.FormatoBrMj;
import util.LeitorSeguro;

/** Read-only batch conversion audit using the same canonical dump as the fixture tests. */
public final class VerificaBrMj {
    private VerificaBrMj() {}

    public static void main(String[] args) throws Exception {
        Path output = Path.of(System.getProperty("brmodelo.verifica.saida", "build/verificaBrMj")).resolve("conversoes");
        Files.createDirectories(output);
        Set<Path> inputs = new TreeSet<>();
        for (String arg : args) {
            Path path = Path.of(arg).toAbsolutePath();
            if (Files.isDirectory(path)) {
                try (Stream<Path> paths = Files.walk(path)) {
                    paths.filter(Files::isRegularFile)
                            .filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".brm3"))
                            .forEach(inputs::add);
                }
            } else inputs.add(path);
        }
        if (inputs.isEmpty()) throw new IOException("No .brM3 files found; pass directories/files with -Pargs");
        StringBuilder report = new StringBuilder(); int ok = 0, number = 0;
        for (Path path : inputs) {
            String status;
            try {
                byte[] source = Files.readAllBytes(path);
                GuardaPadraoBrM envelope;
                try (LeitorSeguro in = new LeitorSeguro(new ByteArrayInputStream(source))) {
                    envelope = (GuardaPadraoBrM) in.readObject();
                }
                var diagram = Fixtures.load(source);
                String before = CanonicalDump.dump(diagram);
                byte[] json = FormatoBrMj.escrever(diagram, envelope);
                if (!Arrays.equals(json, FormatoBrMj.escrever(diagram, envelope))) throw new IOException("Non-deterministic save");
                var converted = FormatoBrMj.ler(json);
                if (!before.equals(CanonicalDump.dump(converted.diagrama()))) throw new IOException("Canonical dump differs after JSON load");
                if (!Arrays.equals(json, FormatoBrMj.escrever(converted.diagrama(), converted.envelope()))) throw new IOException("JSON differs after reload");
                ByteArrayOutputStream binary = new ByteArrayOutputStream();
                try (ObjectOutputStream out = new ObjectOutputStream(binary)) { out.writeObject(converted.paraBrM3()); }
                if (!before.equals(CanonicalDump.dump(Fixtures.load(binary.toByteArray())))) throw new IOException("Canonical dump differs after brM3 conversion");
                String jar = System.getProperty("oficialJar");
                if (jar != null && !jar.isBlank()) OfficialCompatibilityTest.verify(converted.diagrama(), jar, binary.toByteArray());
                String stem = String.format(Locale.ROOT, "%03d-%s", ++number, path.getFileName());
                Files.write(output.resolve(stem + ".brMj"), json);
                Files.write(output.resolve(stem + ".roundtrip.brM3"), binary.toByteArray());
                if (!Arrays.equals(source, Files.readAllBytes(path))) throw new IOException("Source changed during audit");
                status = "OK " + path; ok++;
            } catch (Exception | AssertionError e) { status = "FAIL " + path + " : " + e; }
            System.out.println(status); report.append(status).append('\n');
        }
        String totals = "RESULT OK=" + ok + " FAIL=" + (inputs.size() - ok) + " TOTAL=" + inputs.size();
        System.out.println(totals); report.append(totals).append('\n');
        Files.writeString(output.resolve("relatorio.txt"), report);
        if (ok != inputs.size()) throw new IOException(totals);
    }
}

import controlador.Diagrama;
import controlador.apoios.GuardaPadraoBrM;
import partepronta.GerenciadorPartes;
import util.LeitorSeguro;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.ObjectStreamClass;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

/**
 * Dev-only compatibility check for diagrams and ready-made parts. Logging observes
 * descriptors through the same allowlist as the application; it never disables validation.
 * Diagram envelopes contain a second serialized stream, which must also be checked.
 *
 * Usage: ./gradlew verificaLeitura -Pargs="
 *            [--log-classes] <file-or-directory>..."
 */
public class VerificaLeitura {
    private static final Set<String> classesJdk = new TreeSet<>();
    private static boolean log;

    private static class LeitorComLog extends LeitorSeguro {
        LeitorComLog(InputStream entrada) throws IOException {
            super(entrada);
        }

        @Override
        protected Class<?> resolveClass(ObjectStreamClass descritor)
                throws IOException, ClassNotFoundException {
            String nome = descritor.getName();
            if (log && (nome.startsWith("java.") || nome.startsWith("javax.")
                    || nome.startsWith("["))) {
                classesJdk.add(nome);
            }
            return super.resolveClass(descritor);
        }
    }

    private static boolean arquivoModelo(Path path) {
        String nome = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return nome.endsWith(".brm3") || nome.endsWith(".brmt");
    }

    private static void verificar(Path path) throws Exception {
        Object objeto;
        try (LeitorSeguro in = new LeitorComLog(Files.newInputStream(path))) {
            objeto = in.readObject();
        }
        String nome = path.getFileName().toString().toLowerCase(Locale.ROOT);
        if (nome.endsWith(".brm3")) {
            GuardaPadraoBrM envelope = (GuardaPadraoBrM) objeto;
            // Inspect the envelope without getDiagrama(), which catches read failures.
            Field campo = GuardaPadraoBrM.class.getDeclaredField("diagrama");
            campo.setAccessible(true);
            try (LeitorSeguro in = new LeitorComLog(
                    new ByteArrayInputStream((byte[]) campo.get(envelope)))) {
                Diagrama diagrama = (Diagrama) in.readObject();
                if (diagrama == null) {
                    throw new IOException("Empty diagram");
                }
            }
        } else if (!(objeto instanceof GerenciadorPartes)) {
            throw new IOException("Expected GerenciadorPartes");
        }
    }

    public static void main(String[] args) throws Exception {
        Set<Path> arquivos = new TreeSet<>();
        for (String arg : args) {
            if (arg.equals("--log-classes")) {
                log = true;
                continue;
            }
            Path path = Paths.get(arg).toAbsolutePath().normalize();
            if (Files.isDirectory(path)) {
                try (Stream<Path> paths = Files.walk(path)) {
                    paths.filter(Files::isRegularFile).filter(VerificaLeitura::arquivoModelo)
                            .forEach(arquivos::add);
                }
            } else {
                arquivos.add(path);
            }
        }
        if (arquivos.isEmpty()) {
            System.err.println("Usage: VerificaLeitura [--log-classes] <file-or-directory>...");
            System.exit(2);
        }
        int ok = 0;
        for (Path path : arquivos) {
            try {
                verificar(path);
                System.out.println("OK " + path);
                ok++;
            } catch (InvalidClassException e) {
                System.out.println("REJECTED " + path + " : " + e.classname);
            } catch (Exception e) {
                System.out.println("ERROR " + path + " : " + e);
            }
        }
        if (log) {
            for (String nome : classesJdk) {
                System.out.println("CLASS " + nome);
            }
        }
        System.out.println("RESULT OK=" + ok + " FAILED=" + (arquivos.size() - ok)
                + " TOTAL=" + arquivos.size());
        if (ok != arquivos.size()) {
            System.exit(1);
        }
    }
}

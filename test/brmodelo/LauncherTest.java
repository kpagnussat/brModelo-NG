package brmodelo;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import principal.Aplicacao;
import static org.junit.jupiter.api.Assertions.*;

@Tag("gui")
@Timeout(30)
class LauncherTest {
    @TempDir Path temporary;
    @Test void mainOpensEveryArgumentAfterStartup() throws Exception {
        String conceptual = Path.of("test-resources/fixtures/conceitual.brM3").toAbsolutePath().toString();
        String logical = Path.of("test-resources/fixtures/logico.brM3").toAbsolutePath().toString();
        Path json = temporary.resolve("model with spaces.brMj");
        util.FormatoBrMj.salvar(json, Fixtures.load("livre"));
        Aplicacao.main(new String[] {"missing-file.brM3", conceptual, logical, json.toString(), conceptual});
        SwingUtilities.invokeAndWait(() -> {
            var frame = Aplicacao.fmPrincipal;
            try {
                assertTrue(frame.isShowing());
                assertEquals("brModelo NG", frame.getTitle());
                assertEquals(8, frame.getIconImages().size());
                assertEquals(List.of(conceptual, logical, json.toString()), frame.getEditor().getDiagramas().stream()
                        .map(d -> d.getArquivo()).toList());
                assertFalse(frame.getEditor().diagramaAtual.getListaDeItens().isEmpty());
            } finally {
                frame.dispose();
            }
        });
        CountDownLatch drained = new CountDownLatch(1);
        SwingUtilities.invokeAndWait(() -> Aplicacao.fmPrincipal.getEditor().EndAutoSave(drained::countDown));
        assertTrue(drained.await(10, TimeUnit.SECONDS));
        Aplicacao.fmPrincipal = null;
    }
}

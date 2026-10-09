package util;

import java.awt.Window;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import static org.junit.jupiter.api.Assertions.*;

/** Opt-in human check; every dialog must show desktop bookmarks, recents and search. */
@Tag("gui")
@EnabledIfSystemProperty(named = "brmodelo.seletor.manual", matches = "true")
class SeletorPortalManualTest {
    @Test void allFiveDialogsUseTheDesktopPortal() throws Exception {
        principal.FramePrincipal[] frame = new principal.FramePrincipal[1];
        String previous = System.getProperty("brmodelo.seletor");
        System.setProperty("brmodelo.seletor", "portal");
        try {
            SwingUtilities.invokeAndWait(() -> {
                frame[0] = new principal.FramePrincipal();
                principal.Aplicacao.fmPrincipal = frame[0];
                frame[0].setVisible(true);
                var editor = frame[0].getEditor();
                var root = frame[0].getRootPane();
                var diagram = editor.diagramaAtual;
                // This check only prints paths; it never saves the chosen diagram/image/text files.
                System.out.println("Image: " + Dialogos.ShowDlgFileImg(root));
                System.out.println("Diagram: " + Dialogos.ShowDlgSaveDiagrama(root, diagram));
                System.out.println("Export image: " + Dialogos.ShowDlgSaveAsImg(root, diagram));
                System.out.println("Export any: " + Dialogos.ShowDlgSaveAsAny(root, "consulta.sql"));
                System.out.println("Open diagram: " + Dialogos.ShowDlgLoadDiagrama("", editor));
            });
        } finally {
            CountDownLatch stopped = new CountDownLatch(1);
            if (frame[0] != null) {
                SwingUtilities.invokeAndWait(() -> frame[0].getEditor().EndAutoSave(stopped::countDown));
                assertTrue(stopped.await(10, TimeUnit.SECONDS));
            }
            SwingUtilities.invokeAndWait(() -> {
                for (Window window : Window.getWindows()) window.dispose();
                principal.Aplicacao.fmPrincipal = null;
            });
            if (previous == null) System.clearProperty("brmodelo.seletor");
            else System.setProperty("brmodelo.seletor", previous);
        }
    }
}

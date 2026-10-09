package util;

import java.awt.Color;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MensagensStatusTest {
    @Test void replacesTimerPreservesErrorTextAndExpires() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JLabel label = new JLabel();
            Timer first = MensagensStatus.mostrar(label, "Autosave", false);
            Timer second = MensagensStatus.mostrar(label, "ERROR_LOAD (java: falha)", true);
            assertFalse(first.isRunning());
            assertTrue(second.isRunning());
            assertFalse(second.isRepeats());
            assertEquals(5000, second.getInitialDelay());
            assertEquals("ERROR_LOAD (java: falha)", label.getText());
            assertEquals(MensagensStatus.corErro(), label.getForeground());
            // Trigger the expiration callback without a five-second wall-clock wait.
            second.getActionListeners()[0].actionPerformed(null);
            assertEquals("", label.getText());
            MensagensStatus.parar(label);
            assertFalse(second.isRunning());
        });
    }

    @Test void errorsUseThemeColorAndNormalMessagesRestoreForeground() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Object old = UIManager.get("Actions.Red");
            try {
                UIManager.put("Actions.Red", Color.MAGENTA);
                JLabel label = new JLabel();
                MensagensStatus.mostrar(label, "Error", true);
                assertEquals(PaletaTema.contraste(Color.MAGENTA, EstiloUI.cor("Panel.background"), 4.5), label.getForeground());
                assertTrue(PaletaTema.razao(label.getForeground(), EstiloUI.cor("Panel.background")) >= 4.5);
                MensagensStatus.mostrar(label, "Info", false);
                assertEquals(UIManager.getColor("Label.foreground"), label.getForeground());
                MensagensStatus.parar(label);
            } finally { UIManager.put("Actions.Red", old); }
        });
    }

    @Test void backgroundLoggerCountsEntriesAndDeliversExactTextOnEdt() throws Exception {
        JLabel[] label = new JLabel[1];
        SwingUtilities.invokeAndWait(() -> {
            label[0] = new JLabel();
            BrLogger.setStatus(label[0]);
            BrLogger.setAtualizarIndicador(() -> assertTrue(SwingUtilities.isEventDispatchThread()));
            BrLogger.Clean();
        });
        try {
            Thread worker = new Thread(() -> {
                BrLogger.Logger("MSG_TEST", null);
                BrLogger.Logger("ERRO_SAME_FILE", " (diagrama)", "falha");
                BrLogger.Logger("MSG_LATE", null);
            });
            worker.start();
            worker.join();
            SwingUtilities.invokeAndWait(() -> {
                assertEquals(3, BrLogger.novasMensagens().total());
                assertEquals("ERRO_SAME_FILE (diagrama) (java: falha)", label[0].getText());
                BrLogger.marcarLidas();
                assertEquals(0, BrLogger.novasMensagens().total());
                assertFalse(BrLogger.Logs.isEmpty(), "Reading does not delete history");
            });
        } finally {
            SwingUtilities.invokeAndWait(() -> {
                BrLogger.Clean();
                BrLogger.setStatus(null);
                BrLogger.setAtualizarIndicador(() -> {});
            });
        }
    }
}

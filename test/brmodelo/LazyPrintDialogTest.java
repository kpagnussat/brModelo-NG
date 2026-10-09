package brmodelo;

import java.awt.Window;
import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import static org.junit.jupiter.api.Assertions.*;

@Tag("gui")
class LazyPrintDialogTest {
    @Test @Timeout(30)
    void firstPrintCreatesDialogAndLaterPrintsReuseSettings() throws Exception {
        principal.FramePrincipal[] frame = new principal.FramePrincipal[1];
        Field field = principal.FramePrincipal.class.getDeclaredField("controladorImpressao");
        field.setAccessible(true);
        try {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    frame[0] = new principal.FramePrincipal();
                    principal.Aplicacao.fmPrincipal = frame[0];
                    assertNull(field.get(frame[0]), "Printer discovery is deferred until first print");
                    Timer close = new Timer(10, event -> {
                        try {
                            var dialog = (principal.fmImpressao) field.get(frame[0]);
                            if (dialog != null && dialog.isVisible()) dialog.setVisible(false);
                        } catch (IllegalAccessException e) { throw new RuntimeException(e); }
                    });
                    close.start();
                    try {
                        frame[0].DoComandoExterno(controlador.Controler.menuComandos.cmdPrint);
                        var dialog = (principal.fmImpressao) field.get(frame[0]);
                        assertNotNull(dialog);
                        Field printerField = principal.fmImpressao.class.getDeclaredField("prn");
                        printerField.setAccessible(true);
                        util.PrintControler printer = (util.PrintControler) printerField.get(dialog);
                        printer.getPage().setOrientation(java.awt.print.PageFormat.LANDSCAPE);
                        frame[0].DoComandoExterno(controlador.Controler.menuComandos.cmdPrint);
                        assertSame(dialog, field.get(frame[0]));
                        assertEquals(java.awt.print.PageFormat.LANDSCAPE, printer.getPage().getOrientation());
                    } finally { close.stop(); }
                } catch (Exception e) { throw new RuntimeException(e); }
            });
        } finally {
            if (frame[0] != null) {
                CountDownLatch done = new CountDownLatch(1);
                SwingUtilities.invokeAndWait(() -> frame[0].getEditor().EndAutoSave(done::countDown));
                assertTrue(done.await(10, TimeUnit.SECONDS));
            }
            SwingUtilities.invokeAndWait(() -> {
                for (Window window : Window.getWindows()) window.dispose();
                principal.Aplicacao.fmPrincipal = null;
            });
        }
    }
}

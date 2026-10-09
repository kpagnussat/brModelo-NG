package principal;

import java.awt.Window;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import util.BrLogger;
import util.MensagensStatus;
import static org.junit.jupiter.api.Assertions.*;

@Tag("gui")
@Timeout(30)
class BarraStatusTest {
    @ParameterizedTest @ValueSource(strings = {"flatclaro", "flatescuro", "metal"})
    void unreadStatesExpirationAndOpeningTheActualLog(String tema) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            var original = UIManager.getLookAndFeel();
            FramePrincipal frame = null;
            try {
                if (tema.equals("flatclaro")) com.formdev.flatlaf.FlatLightLaf.setup();
                else if (tema.equals("flatescuro")) com.formdev.flatlaf.FlatDarkLaf.setup();
                else UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
                BrLogger.Clean();
                frame = new FramePrincipal();
                frame.getEditor().setAutoSaveInterval(0);
                frame.setSize(1400, 1050);
                frame.addNotify();
                frame.validate();
                JButton indicator = (JButton) component(frame, "btnLogs");
                JLabel message = frame.getEditor().getLblStatus();
                JPanel panel = (JPanel) component(frame, "statusPanel");
                assertEquals(1, panel.getBorder().getBorderInsets(panel).top);
                assertFalse(indicator.isBorderPainted());
                assertFalse(indicator.isContentAreaFilled());
                assertNotNull(indicator.getIcon());
                assertEquals("", indicator.getText());
                assertEquals("Nenhuma mensagem nova — clique para ver o log", indicator.getToolTipText());

                BrLogger.Logger("MSG_TEST", null);
                assertEquals("1", indicator.getText());
                assertEquals("1 aviso — clique para ver o log", indicator.getToolTipText());
                var infoIcon = indicator.getIcon();
                BrLogger.Logger("ERROR_TEST", "erro mais antigo");
                BrLogger.Logger("ERRO_TEST", " (diagrama)", "erro mais novo");
                assertNotSame(infoIcon, indicator.getIcon());
                assertEquals("3", indicator.getText());
                assertEquals("2 erros, 1 aviso — clique para ver o log", indicator.getToolTipText());
                assertEquals(indicator.getToolTipText(), indicator.getAccessibleContext().getAccessibleName());
                assertEquals("ERRO_TEST (diagrama) (java: erro mais novo)", message.getText());
                assertEquals(MensagensStatus.corErro(), message.getForeground());
                Timer expiration = (Timer) message.getClientProperty("brmodelo.status.timer");
                expiration.getActionListeners()[0].actionPerformed(null);
                assertEquals("", message.getText());
                assertEquals("3", indicator.getText(), "The badge outlives the transient error");

                AtomicBoolean opened = new AtomicBoolean();
                Timer close = new Timer(20, event -> {
                    for (Window window : Window.getWindows()) {
                        if (window instanceof FormaLogs && window.isShowing()) {
                            opened.set(true);
                            try {
                                assertEquals(0, BrLogger.novasMensagens().total());
                                assertEquals(3, BrLogger.Logs.size(), "Read acknowledgement preserves history");
                            } finally { window.setVisible(false); }
                        }
                    }
                });
                close.start();
                try { indicator.doClick(0); }
                finally { close.stop(); }
                assertTrue(opened.get());
                assertEquals("", indicator.getText());
                assertEquals("Nenhuma mensagem nova — clique para ver o log", indicator.getToolTipText());
                BrLogger.Logger("MSG_AFTER_READ", null);
                assertEquals("1", indicator.getText());
                assertEquals("1 aviso — clique para ver o log", indicator.getToolTipText());
            } catch (Exception e) { throw new RuntimeException(e); }
            finally {
                if (frame != null) frame.getEditor().EndAutoSave();
                BrLogger.Clean();
                BrLogger.setStatus(null);
                BrLogger.setAtualizarIndicador(() -> {});
                for (Window window : Window.getWindows()) window.dispose();
                try { UIManager.setLookAndFeel(original); }
                catch (Exception e) { throw new RuntimeException(e); }
            }
        });
    }

    private static Object component(FramePrincipal frame, String name) throws Exception {
        Field field = FramePrincipal.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(frame);
    }
}

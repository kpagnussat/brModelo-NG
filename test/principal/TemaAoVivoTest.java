package principal;

import com.formdev.flatlaf.FlatLightLaf;
import java.awt.Window;
import java.util.HashMap;
import javax.swing.*;
import org.junit.jupiter.api.*;
import controlador.Editor;
import util.*;
import static org.junit.jupiter.api.Assertions.*;

@Tag("gui")
class TemaAoVivoTest {
    @Test void sameEditorAndDialogsRefreshAndDocumentStaysWhite() throws Exception {
        var old = UIManager.getLookAndFeel();
        var config = new HashMap<>(Editor.fromConfiguracao.getConfiguracao());
        FramePrincipal[] frame = new FramePrincipal[1];
        FrameSobre[] about = new FrameSobre[1];
        try {
            SwingUtilities.invokeAndWait(() -> {
                FlatLightLaf.setup();
                frame[0] = new FramePrincipal();
                frame[0].getEditor().setAutoSaveInterval(0);
                frame[0].setSize(1366, 768);
                frame[0].addNotify(); frame[0].validate();
                about[0] = new FrameSobre(frame[0], false);
                MensagensStatus.mostrar(frame[0].getEditor().getLblStatus(), "Erro de exemplo", true);
            });
            var diagram = frame[0].getEditor().diagramaAtual;
            for (String theme : CatalogoTemas.TODOS.stream().map(CatalogoTemas.Tema::id).toArray(String[]::new)) {
                SwingUtilities.invokeAndWait(() -> TemaAplicacao.aplicar(theme));
                for (int pass = 0; pass < 8; pass++) SwingUtilities.invokeAndWait(() -> frame[0].validate());
                // Queue after the single LAF listener's reapplication.
                SwingUtilities.invokeAndWait(() -> {
                    assertSame(diagram, frame[0].getEditor().diagramaAtual);
                    assertEquals(EstiloUI.mesa(), frame[0].getEditor().getBackground());
                    assertEquals(java.awt.Color.WHITE, frame[0].getEditor().getBox().getBackground());
                    assertEquals(UIManager.getColor("Panel.foreground"), about[0].getContentPane().getForeground());
                    assertEquals(MensagensStatus.corErro(), frame[0].getEditor().getLblStatus().getForeground());
                    var logIcon = util.Icones.de("/imagens/log_erro.svg");
                    var iconPixels = new java.awt.image.BufferedImage(logIcon.getIconWidth(), logIcon.getIconHeight(), java.awt.image.BufferedImage.TYPE_INT_ARGB);
                    var iconGraphics = iconPixels.createGraphics(); logIcon.paintIcon(null, iconGraphics, 0, 0); iconGraphics.dispose();
                    boolean errorInk = false;
                    for (int y = 0; y < iconPixels.getHeight(); y++) for (int x = 0; x < iconPixels.getWidth(); x++)
                        if (iconPixels.getRGB(x, y) == MensagensStatus.corErro().getRGB()) errorInk = true;
                    assertTrue(errorInk, theme + " log indicator must use the same readable error ink");
                    assertEquals(CatalogoTemas.buscar(theme).escuro(), CoresTema.escuro());
                    var inspector = frame[0].getEditor().getInspectorEditor();
                    assertEquals(EstiloUI.cor("Panel.background"), inspector.getBox().getBackground());
                    assertEquals(EstiloUI.cor("Panel.background"), frame[0].getEditor().getDicas().getBackground());
                    for (String state : new String[]{"", "focused", "hover", "pressed"}) {
                        String background = "Button.default." + (state.isEmpty() ? "startBackground" : state + "Background");
                        String foreground = "Button.default." + (state.isEmpty() ? "foreground" : state + "Foreground");
                        assertEquals(EstiloUI.cor("Component.accentColor"), UIManager.getColor("Button.default.startBackground"));
                        assertTrue(PaletaTema.razao(UIManager.getColor(foreground), UIManager.getColor(background)) >= 4.5, theme + state);
                    }
                    try {
                        var f = FramePrincipal.class.getDeclaredField("BarraDeBotoes"); f.setAccessible(true);
                        var palette = (JPanel) f.get(frame[0]);
                        JViewport viewport = (JViewport) palette.getParent();
                        int available = viewport.getHeight() - palette.getInsets().top - palette.getInsets().bottom;
                        int size = Math.max(com.formdev.flatlaf.util.UIScale.scale(28), Math.min(com.formdev.flatlaf.util.UIScale.scale(40), available / palette.getComponentCount()));
                        for (var child : palette.getComponents()) assertEquals(new java.awt.Dimension(size, size), child.getSize(), theme);
                        var button = (AbstractButton) palette.getComponent(0);
                        assertTrue(button.isSelected());
                        var painted = new java.awt.image.BufferedImage(size, size, java.awt.image.BufferedImage.TYPE_INT_RGB);
                        var graphics = painted.createGraphics();
                        button.paint(graphics); graphics.dispose();
                        assertEquals(EstiloUI.cor("Component.accentColor").getRGB(), painted.getRGB(1, size / 2), theme + " painted palette accent");
                    } catch (ReflectiveOperationException e) { throw new RuntimeException(e); }
                });
            }
        } finally {
            SwingUtilities.invokeAndWait(() -> {
                if (frame[0] != null) frame[0].getEditor().EndAutoSave();
                for (Window window : Window.getWindows()) window.dispose();
                Editor.fromConfiguracao.setConfiguracao(config);
                try { UIManager.setLookAndFeel(old); } catch (Exception e) { throw new RuntimeException(e); }
            });
        }
    }
}

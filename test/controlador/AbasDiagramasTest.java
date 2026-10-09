package controlador;

import com.formdev.flatlaf.FlatLightLaf;
import java.awt.*;
import java.util.function.IntConsumer;
import javax.swing.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

@Tag("gui")
class AbasDiagramasTest {
    @Test void rowAlignsAndNativeTabActionsReachTheEditorAfterThemeChanges() throws Exception {
        var old = UIManager.getLookAndFeel();
        principal.FramePrincipal[] window = new principal.FramePrincipal[1];
        try {
            SwingUtilities.invokeAndWait(() -> {
                FlatLightLaf.setup();
                window[0] = new principal.FramePrincipal();
                window[0].getEditor().setAutoSaveInterval(0);
                window[0].setSize(1400, 950);
                window[0].addNotify();
                window[0].validate();
            });
            SwingUtilities.invokeAndWait(() -> {
                Editor editor = window[0].getEditor();
                Mostrador host = editor.getShowDiagramas();
                Diagrama second = editor.Novo(Diagrama.TipoDeDiagrama.tpConceitual);
                second.setNome("Segundo modelo");
                second.setArquivo("test-resources/fixtures/conceitual.brM3");
                editor.getDiagramas().add(second);
                host.Construa();
                window[0].validate();
                JTabbedPane tabs = (JTabbedPane) host.getComponent(0);
                assertEquals(2, tabs.getTabCount());
                assertEquals(new java.io.File(second.getArquivo()).getAbsolutePath(), tabs.getToolTipTextAt(1));
                tabs.setSelectedIndex(1);
                assertSame(second, editor.diagramaAtual);
                assertEquals(1, host.getSelectedIndex());
                second.setMudou(true);
                host.Construa();
                assertEquals("*Segundo modelo", tabs.getTitleAt(1));
                second.setMudou(false);
                editor.setSelected(editor.getDiagramas().get(0));
                assertEquals(0, tabs.getSelectedIndex());
                Action next = tabs.getActionMap().get("navigateNext");
                assertNotNull(next);
                next.actionPerformed(new java.awt.event.ActionEvent(tabs, 0, "navigateNext"));
                assertSame(second, editor.diagramaAtual, "Keyboard navigation must activate the diagram");
                editor.setSelected(editor.getDiagramas().get(0));
            });
            for (String theme : new String[]{"escuro", "claro"}) {
                SwingUtilities.invokeAndWait(() -> util.TemaAplicacao.aplicar(theme));
                SwingUtilities.invokeAndWait(() -> {
                    try {
                        var field = principal.FramePrincipal.class.getDeclaredField("TabInspector");
                        field.setAccessible(true);
                        JTabbedPane inspector = (JTabbedPane) field.get(window[0]);
                        Mostrador host = window[0].getEditor().getShowDiagramas();
                        JTabbedPane tabs = (JTabbedPane) host.getComponent(0);
                        assertEquals(inspector.getBoundsAt(0).height, tabs.getBoundsAt(0).height);
                        assertEquals(SwingUtilities.convertPoint(inspector, 0, 0, window[0].getRootPane()).y,
                                SwingUtilities.convertPoint(tabs, 0, 0, window[0].getRootPane()).y);
                        assertEquals(tabs.getPreferredSize().height, host.getHeight());
                        assertTrue(tabs.getUI() instanceof com.formdev.flatlaf.ui.FlatTabbedPaneUI);
                    } catch (ReflectiveOperationException exception) { throw new RuntimeException(exception); }
                });
            }
            SwingUtilities.invokeAndWait(() -> {
                Mostrador host = window[0].getEditor().getShowDiagramas();
                JTabbedPane tabs = (JTabbedPane) host.getComponent(0);
                ((IntConsumer) tabs.getClientProperty("JTabbedPane.tabCloseCallback")).accept(1);
                assertEquals(1, tabs.getTabCount());
                assertEquals(1, window[0].getEditor().getDiagramas().size());
            });
        } finally {
            SwingUtilities.invokeAndWait(() -> {
                if (window[0] != null) {
                    window[0].getEditor().EndAutoSave();
                    window[0].dispose();
                }
                try { UIManager.setLookAndFeel(old); }
                catch (Exception exception) { throw new RuntimeException(exception); }
            });
        }
    }
}

package brmodelo;

import principal.Aplicacao;
import principal.FramePrincipal;
import principal.FormaLogs;

import controlador.editores.EditorDeCampos;
import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import java.lang.reflect.Field;
import javax.swing.*;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@Tag("gui")
class DialogosNormalizadosTest {
    private static Object field(Object object, String name) throws Exception {
        Field field = object.getClass().getDeclaredField(name); field.setAccessible(true); return field.get(object);
    }

    @Test void editedRowsStillWriteBackAddAndDeleteAndBecomeEmpty() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                com.formdev.flatlaf.FlatLightLaf.setup(); util.EstiloUI.iniciar();
                FramePrincipal main = new FramePrincipal(); Aplicacao.fmPrincipal = main;
                main.getEditor().setAutoSaveInterval(0);
                var diagram = (diagramas.logico.DiagramaLogico)brmodelo.Fixtures.load("logico");
                diagram.setMaster(main.getEditor());
                var table = diagram.getListaDeTabelas().getFirst(); diagram.setSelecionado(table);
                EditorDeCampos editor = new EditorDeCampos(main, true); editor.Inicie(diagram);
                JPanel rows = (JPanel)field(editor, "Principal");
                int before = table.getCampos().size();
                JPanel row = (JPanel)rows.getComponent(0);
                JTextField name = null; JComboBox<?> type = null; JCheckBox key = null;
                for (Component cell : row.getComponents()) {
                    if (cell instanceof JTextField text) name = text;
                    if (cell instanceof JComboBox<?> combo) type = combo;
                    if (cell instanceof JCheckBox check && "PK".equals(check.getText())) key = check;
                }
                assertNotNull(name); assertNotNull(type); assertNotNull(key);
                name.setText("identificador");
                var focus = new java.awt.event.FocusEvent(name, java.awt.event.FocusEvent.FOCUS_LOST);
                for (var listener : name.getFocusListeners()) listener.focusLost(focus);
                type.setSelectedItem("BIGINT"); key.doClick();
                assertEquals("identificador", table.getCampos().getFirst().getTexto());
                assertEquals("BIGINT", table.getCampos().getFirst().getTipo());
                assertFalse(table.getCampos().getFirst().isKey());
                ((JButton)field(editor, "Adicionar")).doClick();
                assertEquals(before + 1, table.getCampos().size());
                while (rows.getComponentCount() > 0) {
                    JPanel first = (JPanel)rows.getComponent(0);
                    for (Component cell : first.getComponents()) if (cell instanceof JButton button) { button.doClick(); break; }
                }
                assertTrue(table.getCampos().isEmpty());
                JScrollPane scroll = (JScrollPane)field(editor, "jScrollPane1");
                assertNotSame(rows, scroll.getViewport().getView());
                assertEquals("Fechar", editor.getRootPane().getDefaultButton().getText());
            } catch (Exception e) { throw new RuntimeException(e); }
            finally { for (Window window : Window.getWindows()) window.dispose(); Aplicacao.fmPrincipal = null; }
        });
    }

    @Test void logHasTimeTranslatedMessageRawDetailsClipboardAndClear() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            var clipboard = java.awt.Toolkit.getDefaultToolkit().getSystemClipboard();
            var originalClipboard = clipboard.getContents(null);
            try {
                com.formdev.flatlaf.FlatDarkLaf.setup(); util.EstiloUI.iniciar(); util.BrLogger.Clean();
                util.BrLogger.Logger("Controler.ERRO_SAME_FILE", "aurora.brM3");
                FormaLogs logs = new FormaLogs(null, false);
                JTable table = (JTable)field(logs, "tbPrincipal");
                assertEquals("Hora", table.getColumnName(0)); assertEquals("Mensagem", table.getColumnName(2));
                assertTrue(table.getValueAt(0, 0).toString().matches("\\d{2}:\\d{2}:\\d{2}"));
                assertEquals("Erro", table.getValueAt(0, 1));
                assertFalse(table.getValueAt(0, 2).toString().contains("ERRO_SAME_FILE"));
                assertTrue(table.getValueAt(0, 3).toString().contains("Controler.ERRO_SAME_FILE"));
                assertTrue(table.getValueAt(0, 3).toString().contains("aurora.brM3"));
                JButton copy = find(logs.getContentPane(), "Copiar detalhes");
                assertNotNull(copy); assertFalse(copy.isEnabled());
                table.setRowSelectionInterval(0, 0); assertTrue(copy.isEnabled()); copy.doClick();
                String copied = (String)java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().getData(java.awt.datatransfer.DataFlavor.stringFlavor);
                assertTrue(copied.contains("Controler.ERRO_SAME_FILE"));
                logs.setVisible(true);
                ((JButton)field(logs, "btnLimpar")).doClick();
                assertEquals(0, table.getRowCount()); assertFalse(logs.isVisible()); assertFalse(copy.isEnabled());
                assertTrue(util.BrLogger.Logs.isEmpty());
            } catch (Exception e) { throw new RuntimeException(e); }
            finally { for (Window window : Window.getWindows()) window.dispose(); util.BrLogger.Clean();
                if (originalClipboard != null) clipboard.setContents(originalClipboard, null);
            }
        });
    }

    @Test void alternateEapConstructorsAndConsoleKeepTheirActions() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                com.formdev.flatlaf.FlatLightLaf.setup(); util.EstiloUI.iniciar();
                JDialog owner = new JDialog();
                for (var dialog : new diagramas.eap.EapFormManual[]{
                        new diagramas.eap.EapFormManual((java.awt.Frame)null, false),
                        new diagramas.eap.EapFormManual(owner),
                        new diagramas.eap.EapFormManual(owner, false)}) {
                    assertInstanceOf(java.awt.BorderLayout.class, dialog.getContentPane().getLayout());
                    assertEquals("OK", dialog.getRootPane().getDefaultButton().getText());
                    dialog.getRootPane().getDefaultButton().doClick();
                    assertEquals(JOptionPane.OK_OPTION, dialog.getResultado());
                }
                var console = new principal.cli.FormCli(null, false);
                assertEquals("Fechar", console.getRootPane().getDefaultButton().getText());
                console.setVisible(true);
                console.getRootPane().getDefaultButton().doClick();
                assertFalse(console.isVisible());
            } finally { for (Window window : Window.getWindows()) window.dispose(); }
        });
    }

    @Test void aboutWorksWithoutLegacyHelp() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                com.formdev.flatlaf.FlatLightLaf.setup(); util.EstiloUI.iniciar();
                principal.FrameSobre about = new principal.FrameSobre(null, false);
                var init = principal.FrameSobre.class.getDeclaredMethod("Inicie");
                init.setAccessible(true); init.invoke(about);
                JPanel content = (JPanel)field(about, "Pan");
                JEditorPane credits = assertInstanceOf(JEditorPane.class, content.getComponent(0));
                assertTrue(credits.getDocument().getText(0, credits.getDocument().getLength()).replaceAll("\\s+", " ").contains("Kristofer Pagnussat"));
                assertTrue(credits.getText().contains("https://github.com/chcandido/brModelo"));
            } catch (Exception e) { throw new RuntimeException(e); }
            finally { for (Window window : Window.getWindows()) window.dispose(); }
        });
    }

    @Test void codeColorsRefreshOnThemeChangeWithoutChangingTextOrZoom() throws Exception {
        var reference = new java.util.concurrent.atomic.AtomicReference<controlador.editores.MostradorDeCodigo>();
        String sql = "CREATE TABLE Aluno (id INTEGER PRIMARY KEY);";
        SwingUtilities.invokeAndWait(() -> {
            com.formdev.flatlaf.FlatLightLaf.setup(); util.EstiloUI.iniciar();
            var code = new controlador.editores.MostradorDeCodigo(null, false);
            code.setTexto(sql); reference.set(code);
        });
        String before = reference.get().lblHtml.getText();
        SwingUtilities.invokeAndWait(() -> {
            com.formdev.flatlaf.FlatDarkLaf.setup();
            com.formdev.flatlaf.FlatLaf.updateUI();
        });
        // Flush EstiloUI's queued refresh, the same ordering as a real theme selection.
        SwingUtilities.invokeAndWait(() -> {
            try {
                var code = reference.get();
                assertEquals(sql, code.getTexto());
                assertNotEquals(before, code.lblHtml.getText());
                assertTrue(code.lblHtml.getText().contains(util.ConteudoSobre.cssCor(util.EstiloUI.cor("Actions.Red"))));
            } finally { for (Window window : Window.getWindows()) window.dispose(); }
        });
    }

    private static JButton find(Container container, String text) {
        for (Component component : container.getComponents()) {
            if (component instanceof JButton button && text.equals(button.getText())) return button;
            if (component instanceof Container child) { JButton found = find(child, text); if (found != null) return found; }
        }
        return null;
    }
}

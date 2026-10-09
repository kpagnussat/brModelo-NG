package util;

import controlador.Editor;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.swing.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

@Tag("gui")
class AparenciaTest {
    @Test void startupQuickMenuCardsPreferencesAndEscapeShareOneState() throws Exception {
        var old = UIManager.getLookAndFeel();
        var config = new HashMap<>(Editor.fromConfiguracao.getConfiguracao());
        String command = System.getProperty("brmodelo.tema");
        JDialog[] dialog = new JDialog[1];
        try {
            SwingUtilities.invokeAndWait(() -> {
                Editor.fromConfiguracao.getConfiguracao().put(TemaAplicacao.CHAVE, "nord");
                Editor.fromConfiguracao.getConfiguracao().put(TemaAplicacao.CLARO, "arc");
                Editor.fromConfiguracao.getConfiguracao().put(TemaAplicacao.ESCURO, "dracula");
                System.setProperty("brmodelo.tema", "sistema");
                assertTrue(TemaAplicacao.iniciar());
                assertEquals("auto", TemaAplicacao.escolha());
                assertEquals(TemaDoSistema.escuroDetectado() ? "dracula" : "arc", TemaAplicacao.ativo());
                assertEquals("nord", Editor.fromConfiguracao.getValor(TemaAplicacao.CHAVE), "CLI must not rewrite saved choice");
                JPopupMenu popup = TemaAplicacao.menuRapido(null);
                assertEquals(5, popup.getComponentCount());
                assertEquals("Seguir o sistema", ((JMenuItem) popup.getComponent(0)).getText());
                assertEquals("Mais temas…", ((JMenuItem) popup.getComponent(4)).getText());
                assertTrue(((JRadioButtonMenuItem) popup.getComponent(0)).isSelected());
                ((JMenuItem) popup.getComponent(1)).doClick();
                assertEquals("arc", TemaAplicacao.escolha());
                ((JMenuItem) popup.getComponent(2)).doClick();
                assertEquals("dracula", TemaAplicacao.escolha());
                ((JMenuItem) popup.getComponent(0)).doClick();
                dialog[0] = Aparencia.criar(null); dialog[0].addNotify(); dialog[0].validate();
                assertEquals(Dialog.ModalityType.APPLICATION_MODAL, dialog[0].getModalityType());
                var controls = descendants(dialog[0]);
                var cards = controls.stream().filter(c -> c instanceof JToggleButton b && b.getClientProperty("brmodelo.tema.id") != null)
                        .map(c -> (JToggleButton)c).toList();
                assertEquals(18, cards.size());
                for (var card : cards) {
                    assertTrue(card.isFocusable()); assertNotNull(card.getIcon());
                    for (int key : new int[]{KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT, KeyEvent.VK_UP, KeyEvent.VK_DOWN})
                        assertNotNull(card.getInputMap().get(KeyStroke.getKeyStroke(key, 0)));
                }
                card(cards, "macos-claro").doClick();
                card(cards, "nord").doClick();
                assertEquals("auto", TemaAplicacao.escolha());
                assertEquals("macos-claro", Editor.fromConfiguracao.getValor(TemaAplicacao.CLARO));
                assertEquals("nord", Editor.fromConfiguracao.getValor(TemaAplicacao.ESCURO));
                assertEquals(TemaDoSistema.escuroDetectado() ? "nord" : "macos-claro", TemaAplicacao.ativo());
                TemaAplicacao.sistemaAlterado(false);
                assertEquals("macos-claro", TemaAplicacao.ativo());
                TemaAplicacao.sistemaAlterado(true);
                assertEquals("nord", TemaAplicacao.ativo());
                assertEquals("auto", Editor.fromConfiguracao.getValor(TemaAplicacao.CHAVE));
                assertTrue(card(cards, "macos-claro").isSelected()); assertTrue(card(cards, "nord").isSelected());
                var combos = controls.stream().filter(c -> c instanceof JComboBox).map(c -> (JComboBox<?>)c).toList();
                assertEquals(2, combos.size());
                // Explicitly exercise the native combo action, not just the card path.
                combos.get(0).setSelectedItem(CatalogoTemas.buscar("github-claro"));
                assertEquals("github-claro", TemaAplicacao.preferido(false));
                controls.stream().filter(c -> c instanceof JRadioButton b && b.getText().startsWith("Usar sempre"))
                        .map(c -> (JRadioButton)c).findFirst().orElseThrow().doClick();
                card(cards, "one-dark").doClick();
                TemaAplicacao.sistemaAlterado(false);
                assertEquals("one-dark", TemaAplicacao.ativo(), "Fixed theme ignores desktop changes");
                assertEquals("one-dark", TemaAplicacao.escolha());
                assertEquals("one-dark", Editor.fromConfiguracao.getValor(TemaAplicacao.CHAVE));
                assertEquals(1, cards.stream().filter(AbstractButton::isSelected).count());
                for (var listener : popup.getPopupMenuListeners()) listener.popupMenuWillBecomeVisible(new javax.swing.event.PopupMenuEvent(popup));
                for (int i = 0; i < 3; i++) assertFalse(((JRadioButtonMenuItem)popup.getComponent(i)).isSelected(), "Other fixed themes do not mislabel the preferred quick pair");
                assertEquals("github-claro", Aparencia.vizinho(CatalogoTemas.buscar("macos-claro"), 4).id());
                assertEquals("solarized-claro", Aparencia.vizinho(CatalogoTemas.buscar("escuro"), -4).id());
                assertEquals("material-claro", Aparencia.vizinho(CatalogoTemas.buscar("escuro"), -1).id());
                assertTrue(combos.stream().noneMatch(Component::isEnabled));
                System.clearProperty("brmodelo.tema");
                assertTrue(TemaAplicacao.iniciar());
                assertEquals("one-dark", TemaAplicacao.ativo(), "Saved theme ID is restored");
                assertEquals("github-claro", TemaAplicacao.preferido(false));
                assertEquals("nord", TemaAplicacao.preferido(true));
                Object binding = dialog[0].getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0));
                dialog[0].getRootPane().getActionMap().get(binding).actionPerformed(new java.awt.event.ActionEvent(dialog[0], 0, "escape"));
                assertFalse(dialog[0].isDisplayable());
            });
        } finally {
            SwingUtilities.invokeAndWait(() -> {
                if (dialog[0] != null) dialog[0].dispose();
                Editor.fromConfiguracao.setConfiguracao(config);
                if (command == null) System.clearProperty("brmodelo.tema"); else System.setProperty("brmodelo.tema", command);
                try { UIManager.setLookAndFeel(old); } catch (Exception e) { throw new RuntimeException(e); }
            });
        }
    }
    private static JToggleButton card(List<JToggleButton> cards, String id) {
        return cards.stream().filter(b -> id.equals(b.getClientProperty("brmodelo.tema.id"))).findFirst().orElseThrow();
    }
    private static List<Component> descendants(Container parent) {
        List<Component> list = new ArrayList<>();
        for (Component c : parent.getComponents()) { list.add(c); if (c instanceof Container nested) list.addAll(descendants(nested)); }
        return list;
    }
}

package util;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ResourceBundle;
import javax.swing.*;
import javax.swing.text.JTextComponent;

/** Descriptive hints and full clipped values without changing serialized Swing classes. */
public final class DicasInterface {
    private static final ResourceBundle TEXT = ResourceBundle.getBundle("principal/Formularios_pt_BR");
    private DicasInterface() {}
    public static String texto(String key) { return TEXT.getString("Dica." + key); }
    public static String escape(String text) {
        return text == null ? "" : text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
    public static void dica(JComponent component, String key) {
        String description = texto(key);
        component.putClientProperty("brmodelo.dica.descricao", description);
        component.setToolTipText(description);
        component.getAccessibleContext().setAccessibleDescription(description);
        if (component instanceof JComboBox<?> combo) {
            if (combo.getEditor().getEditorComponent() instanceof JComponent editor) dica(editor, key);
        }
        if (component instanceof JSpinner spinner) propagar(spinner, description);
        if (component instanceof JTextComponent || component instanceof JComboBox<?> || component instanceof JTree || component instanceof JList<?>) {
            if (Boolean.TRUE.equals(component.getClientProperty("brmodelo.dica.instalada"))) return;
            component.putClientProperty("brmodelo.dica.instalada", true);
            MouseAdapter listener = new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { refresh(e); }
                @Override public void mouseMoved(MouseEvent e) { refresh(e); }
                private void refresh(MouseEvent event) {
                    String description = (String) component.getClientProperty("brmodelo.dica.descricao");
                    String value = "";
                    boolean clipped = false;
                    if (component instanceof JTextComponent text) {
                        value = text.getText();
                        clipped = value.contains("\n") || text.getFontMetrics(text.getFont()).stringWidth(value)
                                > text.getWidth() - text.getInsets().left - text.getInsets().right;
                    } else if (component instanceof JComboBox<?> combo) {
                        value = String.valueOf(combo.getSelectedItem() == null ? "" : combo.getSelectedItem());
                        clipped = combo.getFontMetrics(combo.getFont()).stringWidth(value) > combo.getWidth() - 40;
                    } else if (component instanceof JTree tree) {
                        var path = tree.getPathForLocation(event.getX(), event.getY());
                        value = path == null ? "" : path.getLastPathComponent().toString();
                        clipped = !value.isEmpty();
                    } else if (component instanceof JList<?> list) {
                        int index = list.locationToIndex(event.getPoint());
                        if (index >= 0 && list.getCellBounds(index, index).contains(event.getPoint())) {
                            value = String.valueOf(list.getModel().getElementAt(index));
                            clipped = true;
                        }
                    }
                    component.setToolTipText(clipped ? "<html><div style='width: 340px'>" + escape(description)
                            + "<br><br>" + escape(value).replace("\n", "<br>") + "</div></html>" : description);
                }
            };
            component.addMouseListener(listener);
            component.addMouseMotionListener(listener);
            ToolTipManager.sharedInstance().registerComponent(component);
        }
    }
    private static void propagar(Container parent, String tip) {
        for (Component child : parent.getComponents()) {
            if (child instanceof JComponent component) component.setToolTipText(tip);
            if (child instanceof Container container) propagar(container, tip);
        }
    }
    /** Swing recreates the overflow arrows on a theme change; retain their hints too. */
    public static void abas(JTabbedPane tabs) {
        botoesDasAbas(tabs, tabs);
        tabs.addPropertyChangeListener("UI", event -> SwingUtilities.invokeLater(() -> botoesDasAbas(tabs, tabs)));
    }
    private static void botoesDasAbas(Container parent, JTabbedPane tabs) {
        for (Component child : parent.getComponents()) {
            if (tabs.indexOfComponent(child) >= 0) continue;
            if (child instanceof javax.swing.plaf.basic.BasicArrowButton arrow) {
                int direction = arrow.getDirection();
                dica(arrow, direction == SwingConstants.EAST || direction == SwingConstants.SOUTH ? "tabsNext" : "tabsPrevious");
            } else if (child instanceof Container nested) botoesDasAbas(nested, tabs);
        }
    }
    /** Action names remain short; menu/toolbar hints can explain the same command. */
    public static String acao(String description) {
        String key = description.contains(".descricao") ? description.replace(".descricao", ".dica") : description + ".dica";
        String value = controlador.Editor.fromConfiguracao.getValor(key);
        return value.equals(key) ? controlador.Editor.fromConfiguracao.getValor(description) : value;
    }
}

package util;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.util.ColorFunctions;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import javax.swing.*;

/** Shared geometry and semantic surfaces. Only this class listens for LAF changes. */
public final class EstiloUI {
    private static boolean iniciado;
    private EstiloUI() {}

    public static void iniciar() {
        if (iniciado) return;
        iniciado = true;
        defaults();
        UIManager.addPropertyChangeListener(event -> {
            if (!"lookAndFeel".equals(event.getPropertyName())) return;
            defaults();
            SwingUtilities.invokeLater(() -> {
                for (Window window : Window.getWindows()) {
                    atualizar(window);
                    window.invalidate();
                    window.validate();
                    window.repaint();
                }
            });
        });
    }

    private static void defaults() {
        if (!(UIManager.getLookAndFeel() instanceof FlatLaf)) return;
        UIManager.put("Component.arc", 8);
        UIManager.put("Button.arc", 8);
        UIManager.put("TextComponent.arc", 8);
        UIManager.put("Component.focusWidth", 1);
        UIManager.put("Component.minimumHeight", 32);
        UIManager.put("Button.minimumWidth", 88);
        UIManager.put("Button.minimumHeight", 32);
        java.awt.Color accent = cor("Component.accentColor");
        Color ink = PaletaTema.contraste(cor("List.selectionForeground"), accent, 4.5);
        Color hover = CoresTema.mistura(accent, cor("Label.foreground"), .9);
        Color pressed = CoresTema.mistura(accent, cor("Panel.background"), .8);
        // IntelliJ themes may supply gradients that otherwise override the semantic accent.
        UIManager.put("Button.default.background", accent);
        UIManager.put("Button.default.startBackground", accent);
        UIManager.put("Button.default.endBackground", accent);
        UIManager.put("Button.default.foreground", ink);
        UIManager.put("Button.default.focusedBackground", accent);
        UIManager.put("Button.default.focusedForeground", ink);
        UIManager.put("Button.default.hoverBackground", hover);
        UIManager.put("Button.default.hoverForeground", PaletaTema.contraste(ink, hover, 4.5));
        UIManager.put("Button.default.pressedBackground", pressed);
        UIManager.put("Button.default.pressedForeground", PaletaTema.contraste(ink, pressed, 4.5));
        UIManager.put("Button.default.boldText", false);
        UIManager.put("Button.margin", new java.awt.Insets(4, 12, 4, 12));
        UIManager.put("TextField.margin", new java.awt.Insets(4, 8, 4, 8));
        UIManager.put("ComboBox.padding", new java.awt.Insets(4, 8, 4, 8));
        UIManager.put("Spinner.padding", new java.awt.Insets(4, 8, 4, 8));
        UIManager.put("CheckBox.icon.arc", 4);
        UIManager.put("Table.rowHeight", 32);
        UIManager.put("Table.cellMargins", new java.awt.Insets(4, 8, 4, 8));
        UIManager.put("Table.showHorizontalLines", true);
        UIManager.put("Table.showVerticalLines", false);
        UIManager.put("Table.intercellSpacing", new java.awt.Dimension(0, 1));
        UIManager.put("Table.alternateRowColor", null);
        UIManager.put("Table.gridColor", separador());
        UIManager.put("TableHeader.font", UIManager.getFont("Label.font"));
        UIManager.put("TableHeader.height", 32);
        UIManager.put("ScrollPane.focusWidth", 0);
        UIManager.put("ScrollPane.borderWidth", 1);
        UIManager.put("ScrollPane.arc", 8);
        UIManager.put("SplitPane.dividerSize", 1);
        UIManager.put("SplitPaneDivider.gripDotCount", 0);
        UIManager.put("SplitPaneDivider.style", "plain");
        UIManager.put("ScrollBar.width", 12);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("ScrollBar.thumbInsets", new java.awt.Insets(2, 2, 2, 2));
        UIManager.put("ScrollBar.showButtons", false);
        UIManager.put("TabbedPane.tabType", "underlined");
        UIManager.put("TabbedPane.tabInsets", new java.awt.Insets(8, 8, 8, 8));
        UIManager.put("ToolBar.borderMargins", new java.awt.Insets(4, 4, 4, 4));
        UIManager.put("Button.toolbar.spacingInsets", new java.awt.Insets(2, 2, 2, 2));
        UIManager.put("MenuItem.margin", new java.awt.Insets(4, 8, 4, 8));
        UIManager.put("TitlePane.menuBarEmbedded", true);
        DecoracoesJanela.defaults();
    }

    public static Color cor(String key) {
        return PaletaTema.cor(UIManager::getColor, key);
    }

    public static Color separador() {
        Color color = UIManager.getColor("Component.borderColor");
        if (color == null) color = cor("Separator.foreground");
        return CoresTema.mistura(color, cor("Panel.background"), .45);
    }

    public static Color elevado() {
        return CoresTema.mistura(cor("Label.foreground"), cor("Panel.background"), .045);
    }

    public static Color mesa() {
        Color base = cor("Panel.background");
        return CoresTema.escuro() ? base : ColorFunctions.darken(base, .035f);
    }

    /** Remember the semantic role so explicit colors refresh when FlatLaf updates UI. */
    public static Color fundo(JComponent component, String key) {
        component.putClientProperty("brmodelo.superficie", key);
        return key.equals("mesa") ? mesa() : key.equals("elevado") ? elevado() : cor(key);
    }

    public static Color texto(JComponent component, String key) {
        component.putClientProperty("brmodelo.texto", key);
        return cor(key);
    }

    public static void divisorSutil(JSplitPane split) {
        split.putClientProperty("brmodelo.divisorSutil", true);
        if (split.getUI() instanceof javax.swing.plaf.basic.BasicSplitPaneUI ui)
            ui.getDivider().setBackground(separador());
    }

    private static void atualizar(Component component) {
        if (component instanceof controlador.editores.MostradorDeCodigo code && code.lblHtml != null)
            code.setTexto(code.getTexto());
        if (component instanceof JComponent c) {
            if (c.getClientProperty("brmodelo.texto") instanceof String ink) c.setForeground(cor(ink));
            if (c instanceof JEditorPane page && Boolean.TRUE.equals(c.getClientProperty("brmodelo.creditos")))
                ConteudoSobre.estilizar(page);
            if (c.getClientProperty("brmodelo.linha") != null)
                c.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, separador()));
            if (c instanceof TextLineNumber numbers) numbers.setCurrentLineForeground(cor("Component.accentColor"));
            if (c instanceof JSplitPane split && c.getClientProperty("brmodelo.divisorSutil") != null) divisorSutil(split);
            Object role = c.getClientProperty("brmodelo.superficie");
            if (c instanceof JLabel && c.getClientProperty("brmodelo.status.erro") instanceof Boolean error)
                c.setForeground(error ? MensagensStatus.corErro() : cor("Label.foreground"));
            if (role instanceof String key) c.setBackground(fundo(c, key));
            if (c instanceof controlador.inspector.Inspector inspector) {
                inspector.getBox().setBackground(cor("Panel.background"));
                inspector.getViewport().setBackground(cor("Panel.background"));
            }
            if (c instanceof controlador.inspector.InspectorDicas) c.setBackground(cor("Panel.background"));
            if (c.getClientProperty("brmodelo.separador") != null)
                c.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, separador()));
        }
        if (component instanceof Container container)
            for (Component child : container.getComponents()) atualizar(child);
    }

    public static void toolbar(AbstractButton button) {
        button.putClientProperty("JButton.buttonType", "toolBarButton");
        button.setRolloverEnabled(true);
        button.setFocusable(false);
        button.setMargin(new java.awt.Insets(4, 4, 4, 4));
    }
}

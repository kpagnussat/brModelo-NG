package controlador.inspector;

import com.formdev.flatlaf.util.ColorFunctions;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.Border;
import util.EstiloUI;
import util.Icones;
import static controlador.inspector.InspectorProperty.TipoDeProperty.*;

/** Presentation and input helpers outside the Serializable inspector item hierarchy. */
final class GradePropriedades {
    private GradePropriedades() {}
    private static final String HOVER = "brmodelo.inspector.hover";
    private static final String PRESSED = "brmodelo.inspector.pressed";
    private static final JCheckBox CHECK = new JCheckBox();
    static final MouseListener REPINTAR_AO_SAIR = new MouseAdapter() {
        @Override public void mouseEntered(MouseEvent e) {
            ((JComponent)e.getComponent()).putClientProperty(HOVER, e.getPoint());
            e.getComponent().repaint();
        }
        @Override public void mouseExited(MouseEvent e) {
            ((JComponent)e.getComponent()).putClientProperty(HOVER, null);
            ((JComponent)e.getComponent()).putClientProperty(PRESSED, null);
            e.getComponent().repaint();
        }
        @Override public void mouseReleased(MouseEvent e) {
            ((JComponent)e.getComponent()).putClientProperty(PRESSED, null);
            e.getComponent().repaint();
        }
    };
    static void instalar(Inspector inspector) {
        inspector.setBackground(EstiloUI.fundo(inspector, "Panel.background"));
        inspector.getVerticalScrollBar().setBackground(EstiloUI.fundo(inspector.getVerticalScrollBar(), "Panel.background"));
        inspector.getVerticalScrollBar().putClientProperty("FlatLaf.style",
                "track: $Panel.background; hoverTrackColor: $Panel.background; pressedTrackColor: $Panel.background");
        inspector.TipoTexto.setBorder(new BordaCampo());
        inspector.TipoTexto.setMargin(new Insets(0, 0, 0, 0));
        inspector.TipoMenu.putClientProperty("FlatLaf.style", "focusWidth: 0; borderWidth: 0; arc: 0; padding: 0,8,0,8");
        inspector.TipoMenu.setBorder(new BordaCampo());
    }
    static void instalar(InspectorItemBase row) {
        row.addMouseListener(REPINTAR_AO_SAIR);
        row.addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                row.putClientProperty(HOVER, e.getPoint());
                row.repaint();
            }
        });
        ActionListener activate = e -> {
            if (!row.CanEdit() || leitura(row)) return;
            row.Criador.PerformSelect(row);
            if (row instanceof InspectorItemExtender action) action.ExternalRun();
            else if (row instanceof InspectorItemSN) row.Criador.TipoSN.doClick();
            else if (row instanceof InspectorItemMenu) row.Criador.TipoMenu.showPopup();
        };
        row.registerKeyboardAction(activate, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), JComponent.WHEN_FOCUSED);
        row.registerKeyboardAction(activate, KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), JComponent.WHEN_FOCUSED);
    }
    static boolean leitura(InspectorItemBase row) {
        return row.getPropriedade().tipo == tpApenasLeituraTexto || row.getPropriedade().tipo == tpApenasLeituraCor;
    }
    static boolean campo(InspectorItemBase row) {
        return switch (row.getPropriedade().tipo) {
            case tpTextoNormal, tpNumero, tpTextoLongo -> true;
            default -> false;
        };
    }
    static boolean foco(InspectorItemBase row) {
        Component focus = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        return focus != null && row.Criador != null && SwingUtilities.isDescendingFrom(focus, row.Criador);
    }
    static Color fundoSelecao(InspectorItemBase row) {
        return EstiloUI.cor(foco(row) ? "Table.selectionBackground" : "Table.selectionInactiveBackground");
    }
    static Color texto(InspectorItemBase row) {
        if (!row.CanEdit() || leitura(row)) return EstiloUI.cor("Label.disabledForeground");
        if (row.falhou) return EstiloUI.cor("Component.error.focusedBorderColor");
        return EstiloUI.cor("Label.foreground");
    }
    static Color mistura(Color foreground, Color background, float amount) {
        return ColorFunctions.mix(foreground, background, amount);
    }
    static int divisor(InspectorItemBase row) {
        return (int)(row.getWidth() * row.Criador.getDivisor());
    }
    static int linhaBase(InspectorItemBase row, Graphics2D g) {
        return (row.getHeight() - g.getFontMetrics().getHeight()) / 2 + g.getFontMetrics().getAscent();
    }
    static Point mouse(InspectorItemBase row) {
        return row.getClientProperty(HOVER) instanceof Point p ? p : null;
    }
    // A section whose only row is a command with the same caption shows just the button,
    // starting where the property names start; collapsing a lone button has no use.
    static int recuoComando() {
        return 5;
    }
    static Rectangle botao(InspectorItemBase row) {
        int h = row.getHeight();
        return new Rectangle(row.getWidth() - h, 2, h - 3, h - 4);
    }
    static void selecionar(InspectorItemBase row, boolean selected) {
        JComponent editor = row.getOndeEditar();
        if (editor == null) return;
        if (selected) editor.setEnabled(row.CanEdit() && !leitura(row));
        if (!selected || !row.CanEdit() || leitura(row)) {
            row.remove(editor);
            return;
        }
        editor.setEnabled(true);
        editor.setFont(row.getFont());
        // Dialog actions, swatches and checkboxes retain their value rendering while selected.
        // The shared editors still hold the value used by Inspector.EndEdit.
        if (editor == row.Criador.TipoTexto || editor == row.Criador.TipoMenu) {
            row.add(editor);
            editor.setVisible(true);
            posicionarEditor(row);
            editor.requestFocusInWindow();
            if (editor instanceof JTextField field) field.selectAll();
        } else {
            row.requestFocusInWindow();
        }
    }
    static void posicionarEditor(InspectorItemBase row) {
        JComponent editor = row.getOndeEditar();
        if (editor != null && editor.getParent() == row) {
            int d = divisor(row);
            editor.setBounds(d, 0, Math.max(0, row.getWidth() - d), row.getHeight());
            editor.setFont(row.getFont());
            editor.doLayout();
        }
    }
    static void pressionar(InspectorItemBase row, MouseEvent e) {
        InspectorItemSeparador header = DisposicaoPropriedades.cabecalhoDoComando(row);
        row.requestFocusInWindow();
        row.Criador.PerformSelect(row);
        if (row.getArea() != null && row.getArea().contains(e.getPoint())) {
            row.isMouseDown = true;
            return;
        }
        if (!row.CanEdit() || leitura(row) || header == null && e.getX() < divisor(row)) return;
        row.putClientProperty(PRESSED, true);
        if (row instanceof InspectorItemSN) row.Criador.TipoSN.doClick();
        else if (row instanceof InspectorItemMenu) {
            SwingUtilities.invokeLater(() -> {
                if (row.isSelecionado() && row.Criador.TipoMenu.isShowing()) row.Criador.TipoMenu.showPopup();
            });
        } else if (row instanceof InspectorItemExtender action &&
                (row.getPropriedade().tipo == tpCommand || row.getPropriedade().tipo == tpCor || botao(row).contains(e.getPoint()))) {
            try { action.ExternalRun(); }
            finally { row.putClientProperty(PRESSED, null); }
        }
        row.repaint();
    }
    static void pintar(InspectorItemBase row, Graphics2D g) {
        int width = row.getWidth(), height = row.getHeight(), d = divisor(row);
        InspectorItemSeparador commandHeader = DisposicaoPropriedades.cabecalhoDoComando(row);
        row.setArea(commandHeader == null ? new Rectangle(d - 2, 0, 4, height) : null);
        if (commandHeader != null) d = recuoComando();
        Point mouse = mouse(row);
        Color base = EstiloUI.cor("Panel.background");
        Color bg = mouse != null ? mistura(EstiloUI.cor("Label.foreground"), base, .045f) : base;
        g.setColor(bg);
        g.fillRect(0, 0, width, height);
        if (row.isSelecionado()) {
            g.setColor(fundoSelecao(row));
            g.fillRect(0, 0, d, height);
            g.setColor(mistura(fundoSelecao(row), base, .18f));
            g.fillRect(d, 0, width - d, height);
        }
        g.setColor(EstiloUI.separador());
        g.fillRect(0, height - 1, width, 1);
        g.fillRect(d, 0, 1, height);
        g.setFont(row.getFont());
        g.setColor(row.isSelecionado() ? EstiloUI.cor(foco(row) ? "Table.selectionForeground" : "Table.selectionInactiveForeground") : EstiloUI.cor("Label.foreground"));
        Shape clip = g.getClip();
        g.clipRect(0, 0, Math.max(0, d - 8), height);
        if (commandHeader == null) g.drawString(row.getTexto(), 8, linhaBase(row, g));
        g.setClip(clip);
        if (row.getOndeEditar() != null && row.getOndeEditar().getParent() == row) return;
        if (!leitura(row) && row.CanEdit() && (row.isSelecionado() || mouse != null && campo(row))) {
            if (row.isSelecionado() && campo(row)) {
                g.setColor(EstiloUI.cor("TextField.background"));
                g.fillRect(d + 1, 1, width - d - 2, height - 2);
            }
            g.setColor(row.isSelecionado() ? EstiloUI.cor(foco(row) ? "Component.focusColor" : "Component.borderColor") : EstiloUI.separador());
            g.drawRect(d + 1, 1, Math.max(0, width - d - 3), height - 3);
        }
        if (row.getPropriedade().tipo == tpCommand) {
            Rectangle b = new Rectangle(d + 3, 2, width - d - 6, height - 4);
            g.setColor(EstiloUI.cor(row.getClientProperty(PRESSED) != null ? "Button.toolbar.pressedBackground" : mouse != null ? "Button.toolbar.hoverBackground" : "Button.background"));
            g.fillRoundRect(b.x, b.y, b.width, b.height, 6, 6);
            g.setColor(EstiloUI.separador());
            g.drawRoundRect(b.x, b.y, b.width - 1, b.height - 1, 6, 6);
            g.setColor(texto(row));
            g.clipRect(d + 8, 0, Math.max(0, width - d - 16), height);
            g.drawString(row.getTexto().replaceFirst("(?:\\.\\.\\.|…)\\s*$", "") + "…", d + 8, linhaBase(row, g));
            g.setClip(clip);
            return;
        }
        int x = d + 8, reserve = 8;
        boolean action = row.getPropriedade().tipo == tpSelecObject || row.getPropriedade().tipo == tpTextoLongo && (row.isSelecionado() || mouse != null);
        if (row.getPropriedade().tipo == tpMenu || action) reserve = height;
        if (row.getPropriedade().tipo == tpBooleano) {
            CHECK.setSelected(Boolean.parseBoolean(row.getValor()));
            CHECK.setEnabled(row.CanEdit());
            Icon icon = UIManager.getIcon("CheckBox.icon");
            if (icon != null) {
                icon.paintIcon(CHECK, g, x, (height - icon.getIconHeight()) / 2);
                x += icon.getIconWidth() + 6;
            }
        }
        if (row.getPropriedade().tipo == tpCor || row.getPropriedade().tipo == tpApenasLeituraCor) {
            int size = Math.min(height - 10, g.getFontMetrics().getHeight());
            int y = (height - size) / 2;
            g.setColor(EstiloUI.separador());
            g.fillRoundRect(x, y, size, size, 5, 5);
            try {
                Color color = util.Utilidades.StringToColor(row.getTransValor());
                g.setColor(leitura(row) || !row.CanEdit() ? mistura(color, base, .5f) : color);
                g.fillRoundRect(x + 1, y + 1, size - 2, size - 2, 4, 4);
            } catch (IllegalArgumentException ignored) { /* Invalid values retain the theme edge. */ }
            x += size + 6;
        }
        g.setColor(texto(row));
        g.clipRect(x, 0, Math.max(0, width - x - reserve), height);
        g.drawString(row.getTransValor().replace("\n", " | "), x, linhaBase(row, g));
        g.setClip(clip);
        if (row.getPropriedade().tipo == tpMenu || action) {
            Rectangle b = botao(row);
            if (action && mouse != null && b.contains(mouse)) {
                g.setColor(EstiloUI.cor("Button.toolbar.hoverBackground"));
                g.fillRoundRect(b.x, b.y, b.width, b.height, 6, 6);
            }
            String resource = row.getPropriedade().tipo == tpMenu ? "chevron-down" : row.getPropriedade().tipo == tpSelecObject ? "crosshair" : "pencil";
            pintarIcone(row, g, resource, b, mouse != null && row.CanEdit());
        }
    }
    private static void pintarIcone(InspectorItemBase row, Graphics2D g, String name, Rectangle b, boolean strong) {
        Icon icon = Icones.de("/imagens/" + name + ".svg");
        Graphics2D ig = (Graphics2D)g.create();
        ig.setComposite(AlphaComposite.SrcOver.derive(strong ? 1f : .6f));
        if (icon != null) icon.paintIcon(row, ig, b.x + (b.width - icon.getIconWidth()) / 2, b.y + (b.height - icon.getIconHeight()) / 2);
        ig.dispose();
    }
    static void cabecalho(InspectorItemSeparador row, Graphics2D g) {
        Color header = EstiloUI.elevado();
        if (mouse(row) != null) header = mistura(EstiloUI.cor("Label.foreground"), header, .045f);
        g.setColor(row.isSelecionado() ? fundoSelecao(row) : header);
        g.fillRect(0, 0, row.getWidth(), row.getHeight());
        if (row.endOFF) return;
        row.setArea(null);
        g.setColor(EstiloUI.separador());
        g.fillRect(0, 0, row.getWidth(), 1);
        g.fillRect(0, row.getHeight() - 1, row.getWidth(), 1);
        Icon icon = Icones.de(row.getEstado() == '+' ? "/imagens/chevron-right.svg" : "/imagens/chevron-down.svg");
        icon.paintIcon(row, g, 8, (row.getHeight() - icon.getIconHeight()) / 2);
        g.setFont(row.getFont().deriveFont(Font.BOLD));
        g.setColor(row.isSelecionado() ? EstiloUI.cor(foco(row) ? "Table.selectionForeground" : "Table.selectionInactiveForeground") : texto(row));
        g.drawString(row.getTexto(), 8 + icon.getIconWidth() + 8, linhaBase(row, g));
    }
    /** A theme-aware one-pixel outline wholly inside the editor; no outside focus ring. */
    private static final class BordaCampo implements Border {
        @Override public Insets getBorderInsets(Component c) { return new Insets(0, 8, 0, 8); }
        @Override public boolean isBorderOpaque() { return false; }
        @Override public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            g.setColor(EstiloUI.cor(c.isFocusOwner() ? "Component.focusColor" : "Component.borderColor"));
            g.drawRect(x, y, w - 1, h - 1);
        }
    }
}

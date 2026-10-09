package util;

import com.formdev.flatlaf.util.UIScale;
import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.swing.*;
import javax.swing.border.Border;

/** Shared dialog geometry and actions, outside the serialized Swing form classes. */
public final class AcabamentoDialogos {
    private AcabamentoDialogos() {}
    public static int px(int value) { return UIScale.scale(value); }

    public static void aplicar(JDialog dialog) { aplicar((Window) dialog); }

    public static void aplicar(Window window) {
        if (!(window instanceof RootPaneContainer root)) return;
        Container content = root.getContentPane();
        if (content instanceof JComponent panel)
            panel.setBorder(BorderFactory.createEmptyBorder(px(12), px(12), px(12), px(12)));
        normalizar(content);
        if (content.getLayout() instanceof BorderLayout layout
                && layout.getLayoutComponent(BorderLayout.SOUTH) instanceof JPanel actions
                && Boolean.TRUE.equals(actions.getClientProperty("brmodelo.acoes"))) {
            int gap = layout.getVgap();
            layout.setVgap(0); // The action row owns its 12px top padding.
            Component top = layout.getLayoutComponent(BorderLayout.NORTH);
            if (top != null && gap > 0) {
                JPanel heading = new JPanel(new BorderLayout());
                heading.setBorder(BorderFactory.createEmptyBorder(0, 0, gap, 0));
                heading.add(top);
                content.add(heading, BorderLayout.NORTH);
            }
        }
        List<JButton> buttons = botoes(content);
        JButton primary = buttons.stream().filter(b -> b.isVisible() && b.getText() != null)
                .max(Comparator.comparingInt(AcabamentoDialogos::prioridade)).orElse(null);
        if (primary != null && prioridade(primary) > 0) root.getRootPane().setDefaultButton(primary);
        JButton cancel = buttons.stream().filter(b -> b.getText() != null
                && (b.getText().startsWith("Cancelar") || b.getText().equals("Fechar"))).findFirst().orElse(null);
        root.getRootPane().registerKeyboardAction(e -> {
            if (cancel != null && cancel.isEnabled()) cancel.doClick();
            else window.setVisible(false);
        }, KeyStroke.getKeyStroke("ESCAPE"), JComponent.WHEN_IN_FOCUSED_WINDOW);
        if (window instanceof JDialog dialog) dialog.setResizable(true);
        if (window instanceof JDialog dialog) dialog.pack();
        else if (window instanceof JFrame frame) frame.pack();
        window.setMinimumSize(new Dimension(Math.min(window.getWidth(), px(360)), Math.min(window.getHeight(), px(240))));
        limitar(window);
    }

    public static void limitar(Window window) {
        GraphicsConfiguration configuration = window.getGraphicsConfiguration();
        if (configuration == null) return;
        Rectangle screen = configuration.getBounds();
        Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(configuration);
        int width = screen.width - insets.left - insets.right - px(24);
        int height = screen.height - insets.top - insets.bottom - px(24);
        window.setSize(Math.min(window.getWidth(), width), Math.min(window.getHeight(), height));
    }

    private static int prioridade(JButton button) {
        String text = button.getText();
        if (text == null) return 0;
        if (text.startsWith("Continuar") || text.equals("Aplicar") || text.equals("Salvar") || text.equals("OK") || text.equals("Pronto")) return 4;
        if (text.equals("Imprimir")) return 3;
        if (text.equals("Fechar")) return 1;
        return 0;
    }

    private static List<JButton> botoes(Container parent) {
        List<JButton> result = new ArrayList<>();
        for (Component child : parent.getComponents()) {
            if (child instanceof JButton button) result.add(button);
            else if (child instanceof Container container) result.addAll(botoes(container));
        }
        return result;
    }

    public static String frase(String text) {
        if (text == null || text.startsWith("<html>") || text.length() < 4) return text;
        if (text.equals(text.toUpperCase(java.util.Locale.ROOT)))
            return text.substring(0, 1) + text.substring(1).toLowerCase(java.util.Locale.forLanguageTag("pt-BR"));
        return text;
    }

    public static Border bordaSecao(JComponent panel, String title) {
        panel.putClientProperty("brmodelo.secao", frase(title));
        return BorderFactory.createEmptyBorder();
    }

    private static void normalizar(Container parent) {
        for (Component child : parent.getComponents()) {
            if (child instanceof AbstractButton button && !(button instanceof JMenuItem)) {
                button.setText(frase(button.getText()));
                if (button.getText() != null && button.getText().startsWith("Adicionar")) {
                    button.setIcon(Icones.de("/imagens/mais.png"));
                    button.setVerticalTextPosition(SwingConstants.CENTER);
                    button.setHorizontalTextPosition(SwingConstants.RIGHT);
                }
            }
            if (child instanceof JLabel label) label.setText(frase(label.getText()));
            if (child instanceof JTable table) {
                for (int i = 0; i < table.getColumnCount(); i++) {
                    var column = table.getColumnModel().getColumn(i);
                    if (column.getHeaderValue() instanceof String text) column.setHeaderValue(frase(text));
                }
            }
            if (child instanceof JSplitPane split) {
                split.setBorder(BorderFactory.createEmptyBorder());
                split.setDividerSize(px(1));
                EstiloUI.divisorSutil(split);
            }
            if (child instanceof JScrollPane scroll) {
                scroll.putClientProperty("FlatLaf.style", "focusWidth: 0; arc: 8");
                scroll.getVerticalScrollBar().setUnitIncrement(px(24));
                EstadosVazios.instalar(scroll);
            }
            if (child instanceof Container container) normalizar(container);
            if (child instanceof JPanel panel && panel.getClientProperty("brmodelo.secao") instanceof String title)
                formulario(panel, title);
            if (child instanceof JPanel panel && panel.getClientProperty("brmodelo.adicoes") == null && panel.getComponentCount() > 0
                    && java.util.Arrays.stream(panel.getComponents()).allMatch(c -> c instanceof JButton))
                acoes(panel);
        }
        if (parent.getLayout() instanceof GroupLayout layout) {
            layout.setAutoCreateGaps(true);
        }
    }

    public static void acoes(JPanel panel) {
        panel.putClientProperty("brmodelo.acoes", true);
        List<Component> buttons = new ArrayList<>(java.util.Arrays.stream(panel.getComponents()).filter(c -> c instanceof JButton).toList());
        buttons.sort(Comparator.comparingInt(c -> c instanceof JButton b ? prioridade(b) : 0));
        panel.removeAll();
        panel.setBorder(BorderFactory.createEmptyBorder(px(12), 0, 0, 0));
        panel.setLayout(new GridBagLayout());
        GridBagConstraints glue = new GridBagConstraints();
        glue.gridx = 0; glue.gridy = 0; glue.weightx = 1; glue.fill = GridBagConstraints.HORIZONTAL;
        panel.add(Box.createHorizontalGlue(), glue);
        for (int index = 0; index < buttons.size(); index++) {
            GridBagConstraints c = new GridBagConstraints(); c.gridx = index + 1; c.gridy = 0;
            c.insets = new Insets(0, index == 0 ? 0 : px(8), 0, 0); panel.add(buttons.get(index), c);
        }
    }

    /** Replaces builder spacer columns with label/control pairs that grow with the font. */
    public static void formulario(JPanel panel, String title) {
        Component[] components = panel.getComponents();
        panel.putClientProperty("brmodelo.secao", null);
        panel.removeAll();
        panel.setBorder(BorderFactory.createEmptyBorder());
        panel.setLayout(new GridBagLayout());
        int y = 0;
        if (title != null && !title.isBlank()) {
            JLabel heading = new JLabel(title);
            heading.putClientProperty("FlatLaf.style", "font: bold");
            GridBagConstraints h = new GridBagConstraints();
            h.gridx = 0; h.gridy = y++; h.gridwidth = 2; h.weightx = 1;
            h.anchor = GridBagConstraints.WEST; h.insets = new Insets(0, 0, px(8), 0);
            panel.add(heading, h);
        }
        for (int i = 0; i < components.length; i++) {
            Component component = components[i];
            GridBagConstraints c = new GridBagConstraints();
            c.gridy = y++; c.gridx = 0; c.anchor = GridBagConstraints.WEST;
            c.insets = new Insets(0, 0, px(8), px(8));
            if (component instanceof JLabel label && i + 1 < components.length
                    && !(components[i + 1] instanceof JLabel)) {
                panel.add(component, c);
                Component field = components[++i];
                label.setLabelFor(field);
                if (field instanceof JTextField text) text.setColumns(24);
                c.gridx = 1; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL;
                c.insets.right = 0;
                panel.add(field, c);
            } else {
                c.gridwidth = 2; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL;
                c.insets.right = 0;
                panel.add(component, c);
            }
        }
    }

    public static void editor(JDialog dialog, JPanel selection, JComponent middle, JPanel actions) {
        Container root = dialog.getContentPane();
        root.removeAll();
        root.setLayout(new BorderLayout(px(16), px(16)));
        root.add(selection, BorderLayout.NORTH);
        root.add(middle, BorderLayout.CENTER);
        root.add(actions, BorderLayout.SOUTH);
        aplicar(dialog);
    }

    public static JPanel coluna(Component... components) {
        JPanel panel = new JPanel(new GridBagLayout());
        for (int i = 0; i < components.length; i++) {
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0; c.gridy = i; c.weightx = 1;
            c.fill = GridBagConstraints.BOTH;
            c.insets = new Insets(i == 0 ? 0 : px(8), 0, 0, 0);
            if (components[i] instanceof JScrollPane || components[i] instanceof JSplitPane) c.weighty = 1;
            panel.add(components[i], c);
        }
        return panel;
    }
}

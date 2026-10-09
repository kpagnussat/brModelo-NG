package util;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import javax.swing.*;

/** Content-based, shared column widths for editable rows. Does not enter serialized models. */
public final class LinhasDialogos implements LayoutManager {
    private LinhasDialogos() {}

    public static void preparar(JPanel list) {
        list.setPreferredSize(null);
        list.setLayout(new GridBagLayout());
    }

    public static void adicionar(JPanel list, JPanel row) {
        if (!(list.getLayout() instanceof LinhasLista)) preparar(list);
        row.setLayout(new LinhasDialogos());
        row.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, EstiloUI.separador()));
        row.putClientProperty("brmodelo.linha", true);
        row.setBackground(EstiloUI.fundo(row, "Panel.background"));
        for (Component child : row.getComponents()) {
            child.setPreferredSize(null);
            if (child instanceof JTextField text) {
                text.setColumns(18);
                DicasInterface.dica(text, text.isEditable() ? "name" : "nameRead");
            }
            if (child instanceof JComboBox<?> combo) DicasInterface.dica(combo, "type");
            if (child instanceof JCheckBox check) {
                String label = check.getText();
                if (label.toLowerCase(java.util.Locale.ROOT).contains("primária")) check.setText("PK");
                if (label.toLowerCase(java.util.Locale.ROOT).contains("estrangeira")) check.setText("FK");
                if (label.equalsIgnoreCase("UNIQUE")) check.setText("Único");
                String key = label.toLowerCase(java.util.Locale.ROOT).contains("primária") || label.equals("PK") ? "pk"
                        : label.toLowerCase(java.util.Locale.ROOT).contains("estrangeira") || label.equals("FK") ? "fk"
                        : label.equalsIgnoreCase("UNIQUE") || label.equals("Único") ? "unique"
                        : label.toLowerCase(java.util.Locale.ROOT).contains("identificador") ? "identifier" : "constraintField";
                DicasInterface.dica(check, key);
            }
            if (child instanceof JButton button) {
                button.setIcon(Icones.de("/imagens/trash-2.svg"));
                button.setText("");
                button.putClientProperty("FlatLaf.style", "minimumWidth: 32; borderWidth: 0; background: $Panel.background; hoverForeground: $Actions.Red");
                DicasInterface.dica(button, "delete");
                button.addActionListener(event -> { list.remove(row); list.revalidate(); list.repaint(); });
                button.getAccessibleContext().setAccessibleName(button.getToolTipText());
            }
            if (child instanceof JComponent component) component.setOpaque(false);
        }
        MouseAdapter hover = new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { row.setBackground(EstiloUI.elevado()); }
            public void mouseExited(MouseEvent e) { row.setBackground(EstiloUI.cor("Panel.background")); }
        };
        row.addMouseListener(hover);
        for (Component child : row.getComponents()) child.addMouseListener(hover);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0; c.gridy = list.getComponentCount(); c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL; c.anchor = GridBagConstraints.NORTHWEST;
        list.add(row, c);
        // Anchor the stack at the top while allowing the viewport to grow.
        list.setLayout(new LinhasLista());
        list.revalidate(); list.repaint();
    }

    private int[] widths(Container parent) {
        int[] widths = new int[parent.getComponentCount()];
        Container list = parent.getParent();
        Component[] rows = list == null ? new Component[]{parent} : list.getComponents();
        for (Component item : rows) {
            if (!(item instanceof JPanel row) || row.getComponentCount() != widths.length) continue;
            for (int i = 0; i < widths.length; i++) {
                Component cell = row.getComponent(i);
                int width = cell.getPreferredSize().width;
                if (cell instanceof JComboBox<?>) width = Math.max(width, AcabamentoDialogos.px(140));
                if (cell instanceof JButton) width = AcabamentoDialogos.px(32);
                widths[i] = Math.max(widths[i], width);
            }
        }
        return widths;
    }

    public void addLayoutComponent(String name, Component comp) {}
    public void removeLayoutComponent(Component comp) {}
    public Dimension preferredLayoutSize(Container parent) {
        int height = AcabamentoDialogos.px(32);
        for (Component child : parent.getComponents()) height = Math.max(height, child.getPreferredSize().height);
        return new Dimension(Arrays.stream(widths(parent)).sum() + AcabamentoDialogos.px(8) * (parent.getComponentCount() + 1), height + AcabamentoDialogos.px(16) + 1);
    }
    public Dimension minimumLayoutSize(Container parent) { return preferredLayoutSize(parent); }
    public void layoutContainer(Container parent) {
        int[] widths = widths(parent);
        int extra = Math.max(0, parent.getWidth() - preferredLayoutSize(parent).width);
        int x = AcabamentoDialogos.px(8), gap = AcabamentoDialogos.px(8);
        for (int i = 0; i < widths.length; i++) {
            Component child = parent.getComponent(i);
            int width = widths[i];
            if (child instanceof JTextField) { width += extra; extra = 0; }
            int height = child.getPreferredSize().height;
            child.setBounds(x, (parent.getHeight() - height) / 2, width, height);
            x += width + gap;
        }
    }

    private static final class LinhasLista implements LayoutManager {
        public void addLayoutComponent(String name, Component component) {}
        public void removeLayoutComponent(Component component) {}
        public Dimension preferredLayoutSize(Container parent) {
            int width = 0, height = 0;
            for (Component row : parent.getComponents()) {
                Dimension d = row.getPreferredSize(); width = Math.max(width, d.width); height += d.height;
            }
            return new Dimension(width, height);
        }
        public Dimension minimumLayoutSize(Container parent) { return preferredLayoutSize(parent); }
        public void layoutContainer(Container parent) {
            int y = 0;
            for (Component row : parent.getComponents()) {
                int height = row.getPreferredSize().height;
                row.setBounds(0, y, Math.max(parent.getWidth(), row.getPreferredSize().width), height);
                y += height;
            }
        }
    }
}

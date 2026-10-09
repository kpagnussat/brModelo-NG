package util;

import java.awt.*;
import javax.swing.*;

/** Content-specific arrangements built from the existing forms' controls and bindings. */
public final class LayoutDialogos {
    private LayoutDialogos() {}
    public static void pares(JPanel panel, String title, Component... pairs) {
        panel.removeAll(); panel.putClientProperty("brmodelo.secao", null);
        panel.setLayout(new GridBagLayout()); panel.setBorder(BorderFactory.createEmptyBorder());
        int y = 0;
        if (title != null) {
            JLabel label = new JLabel(title); label.putClientProperty("FlatLaf.style", "font: bold");
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0; c.gridy = y++; c.gridwidth = 2; c.anchor = GridBagConstraints.WEST;
            c.insets = new Insets(0, 0, AcabamentoDialogos.px(8), 0); panel.add(label, c);
        }
        for (int i = 0; i < pairs.length; i += 2) {
            Component label = pairs[i], field = pairs[i + 1];
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0; c.gridy = y++; c.anchor = GridBagConstraints.WEST;
            c.insets = new Insets(0, 0, AcabamentoDialogos.px(8), AcabamentoDialogos.px(8));
            if (label instanceof JLabel l) l.setLabelFor(field);
            if (field instanceof JTextField text) text.setColumns(24);
            panel.add(label, c);
            c.gridx = 1; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL; c.insets.right = 0;
            panel.add(field, c);
        }
    }

    public static JPanel adicoes(JToolBar toolbar) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEADING, AcabamentoDialogos.px(8), 0));
        panel.setBorder(BorderFactory.createEmptyBorder());
        panel.putClientProperty("brmodelo.adicoes", true);
        for (Component child : toolbar.getComponents()) {
            if (!(child instanceof JButton button)) continue;
            button.setFocusable(true);
            button.setVerticalTextPosition(SwingConstants.CENTER);
            button.setHorizontalTextPosition(SwingConstants.RIGHT);
            panel.add(button);
        }
        return panel;
    }

    public static void conteudo(JPanel panel, Component... components) {
        panel.removeAll(); panel.setLayout(new BorderLayout(0, AcabamentoDialogos.px(8)));
        panel.add(AcabamentoDialogos.coluna(components));
    }

    public static void area(JScrollPane scroll, int width, int height) {
        scroll.setPreferredSize(new Dimension(AcabamentoDialogos.px(width), AcabamentoDialogos.px(height)));
    }

    public static void texto(JDialog dialog, JToolBar toolbar, JScrollPane scroll, JPanel actions) {
        Container root = dialog.getContentPane(); root.removeAll();
        root.setLayout(new BorderLayout(0, AcabamentoDialogos.px(16)));
        root.add(toolbar, BorderLayout.NORTH); root.add(scroll, BorderLayout.CENTER); root.add(actions, BorderLayout.SOUTH);
        area(scroll, 760, 360); AcabamentoDialogos.aplicar(dialog);
    }

    public static void salvar(JDialog dialog, JScrollPane scroll, JPanel list, JButton all, JButton none, JButton cancel, JButton ok) {
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.TRAILING, AcabamentoDialogos.px(8), 0));
        actions.add(cancel); actions.add(ok);
        JPanel select = new JPanel(new FlowLayout(FlowLayout.LEADING, AcabamentoDialogos.px(8), 0));
        select.add(all); select.add(none);
        JPanel heading = new JPanel(new BorderLayout(0, AcabamentoDialogos.px(8)));
        heading.add(new JLabel("Selecione os diagramas para salvar"), BorderLayout.NORTH);
        heading.add(select, BorderLayout.SOUTH);
        list.setLayout(new BoxLayout(list, BoxLayout.PAGE_AXIS));
        list.setBorder(BorderFactory.createEmptyBorder(AcabamentoDialogos.px(8), AcabamentoDialogos.px(8), AcabamentoDialogos.px(8), AcabamentoDialogos.px(8)));
        area(scroll, 540, 200);
        AcabamentoDialogos.editor(dialog, heading, scroll, actions);
    }

    public static void impressao(JDialog dialog, JToolBar settings, JToolBar tools, JScrollPane paper, JPanel actions) {
        // Builder toolbars included a 171px blank panel: use ordinary controls in two lines.
        JPanel options = new JPanel(new GridBagLayout());
        int x = 0, y = 0;
        for (Component child : settings.getComponents()) {
            if (child instanceof JPanel || child instanceof JSeparator) continue;
            if (child instanceof JCheckBox && x > 0) { y++; x = 0; }
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = x++; c.gridy = y; c.anchor = GridBagConstraints.WEST;
            c.insets = new Insets(0, 0, AcabamentoDialogos.px(8), AcabamentoDialogos.px(8));
            if (child instanceof JSpinner spinner && spinner.getEditor() instanceof JSpinner.DefaultEditor editor)
                editor.getTextField().setColumns(3);
            options.add(child, c);
        }
        JPanel top = AcabamentoDialogos.coluna(adicoes(tools), options);
        area(paper, 760, 340);
        AcabamentoDialogos.editor(dialog, top, paper, actions);
        JButton print = AcabamentoDialogosButton.find(top, "Imprimir");
        if (print != null) {
            print.getParent().remove(print);
            actions.add(print);
            AcabamentoDialogos.acoes(actions);
            dialog.getRootPane().setDefaultButton(print);
            dialog.pack();
        }
        dialog.setTitle("brModelo NG: Impressão");
    }

    // Kept separate from form classes so no serialVersionUID is affected.
    private static final class AcabamentoDialogosButton {
        static JButton find(Container panel, String text) {
            for (Component component : panel.getComponents()) {
                if (component instanceof JButton button && text.equals(button.getText())) return button;
                if (component instanceof Container child) { JButton result = find(child, text); if (result != null) return result; }
            }
            return null;
        }
    }

    public static void laterais(JPanel panel) {
        Component[] buttons = panel.getComponents(); panel.removeAll(); panel.setLayout(new GridBagLayout());
        for (int i = 0; i < buttons.length; i++) {
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0; c.gridy = i; c.fill = GridBagConstraints.HORIZONTAL;
            c.insets = new Insets(i == 0 ? 0 : AcabamentoDialogos.px(8), 0, 0, 0);
            panel.add(buttons[i], c);
        }
        GridBagConstraints fill = new GridBagConstraints(); fill.gridx = 0; fill.gridy = buttons.length; fill.weighty = 1;
        panel.add(Box.createVerticalGlue(), fill);
    }

    public static void fontes(JPanel panel) {
        Component[] fields = panel.getComponents(); panel.removeAll();
        for (int i = 0; i < fields.length; i++) {
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = i; c.gridy = 0; c.weightx = i == 0 ? .6 : i == 1 ? .3 : .1;
            c.fill = GridBagConstraints.BOTH;
            c.insets = new Insets(0, i == 0 ? 0 : AcabamentoDialogos.px(8), 0, 0);
            panel.add(fields[i], c);
        }
    }

    public static void preview(JDialog dialog, JToolBar toolbar, JButton close, JScrollPane paper) {
        toolbar.remove(close);
        for (Component component : toolbar.getComponents())
            if (component instanceof JPanel) toolbar.remove(component);
        JPanel actions = new JPanel(); actions.add(close);
        JPanel top = new JPanel(new BorderLayout()); top.add(toolbar);
        area(paper, 760, 480);
        AcabamentoDialogos.editor(dialog, top, paper, actions);
        dialog.setTitle("brModelo NG: Prévia de impressão");
    }

    public static void eap(JDialog dialog, JPanel position, JPanel settings, JPanel actions,
            JScrollPane processes, JLabel heading, JLabel[] labels, JTextField[] fields,
            JRadioButton... directions) {
        pares(position, null, labels[0], fields[0], labels[1], fields[1], labels[2], fields[2]);
        fields[1].setColumns(6);
        fields[2].setColumns(6);
        JPanel organization = new JPanel(new GridLayout(0, 2, AcabamentoDialogos.px(8), AcabamentoDialogos.px(8)));
        for (JRadioButton direction : directions) organization.add(direction);
        pares(settings, null, labels[3], organization);
        area(processes, 720, 220);
        AcabamentoDialogos.editor(dialog, AcabamentoDialogos.coluna(position, settings, heading), processes, actions);
    }

    private static void secao(JPanel panel, String title, Component content) {
        panel.removeAll(); panel.putClientProperty("brmodelo.secao", null);
        panel.setBorder(BorderFactory.createEmptyBorder());
        panel.setLayout(new BorderLayout(0, AcabamentoDialogos.px(8)));
        JLabel heading = new JLabel(title);
        heading.putClientProperty("FlatLaf.style", "font: bold");
        panel.add(heading, BorderLayout.NORTH); panel.add(content, BorderLayout.CENTER);
    }

    public static void ajudaEdicao(JPanel details, JPanel topic, JPanel location, JComboBox<?> parent,
            JPanel title, JTextField titleField, JPanel html, JScrollPane htmlScroll,
            JPanel links, JComboBox<?> link, JPanel linkList, JToolBar tools, JScrollPane listScroll) {
        java.util.ResourceBundle text = java.util.ResourceBundle.getBundle("principal/Formularios_pt_BR");
        secao(location, text.getString("ajuda.edicao.painel.arvore"), parent);
        secao(title, text.getString("ajuda.editor.painel.titulo"), titleField);
        area(htmlScroll, 560, 140);
        secao(html, text.getString("ajuda.editor.label.html"), htmlScroll);
        linkList.removeAll(); linkList.setLayout(new BorderLayout(AcabamentoDialogos.px(8), 0));
        linkList.add(tools, BorderLayout.WEST); linkList.add(listScroll, BorderLayout.CENTER);
        area(listScroll, 520, 140);
        secao(links, text.getString("ajuda.editor.label.links"), AcabamentoDialogos.coluna(link, linkList));
        topic.removeAll(); topic.putClientProperty("brmodelo.secao", null);
        topic.setLayout(new BorderLayout()); topic.add(AcabamentoDialogos.coluna(title, html, links));
        JScrollPane scroll = new JScrollPane(topic);
        scroll.getVerticalScrollBar().setUnitIncrement(AcabamentoDialogos.px(24));
        details.removeAll(); details.setLayout(new BorderLayout(0, AcabamentoDialogos.px(16)));
        details.add(location, BorderLayout.NORTH); details.add(scroll, BorderLayout.CENTER);
    }
}

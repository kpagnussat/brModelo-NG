package util;

import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

/** Master/detail geometry for the IR editors, outside their serialized form. */
public final class LayoutIr {
    private LayoutIr() {}

    public static void montar(JDialog dialog, JPanel selection, JPanel settings, JPanel detail,
            JToolBar additions, JScrollPane fields, JTextArea sql, JScrollPane preview,
            JPanel actions, JSplitPane split, JPanel master, JList<String> keys,
            JScrollPane keyScroll, JToolBar tools) {
        JPanel fieldSection = new JPanel(new BorderLayout(0, AcabamentoDialogos.px(8)));
        fieldSection.add(LayoutDialogos.adicoes(additions), BorderLayout.NORTH);
        fieldSection.add(fields, BorderLayout.CENTER);
        fields.setMinimumSize(new Dimension(0, 0));
        LayoutDialogos.area(fields, 760, 160);
        sql.setRows(3);
        preview.setMinimumSize(new Dimension(0, 0));
        JPanel body = new JPanel(new BorderLayout(0, AcabamentoDialogos.px(8)));
        if (split == null) {
            body.add(fieldSection, BorderLayout.CENTER);
        } else {
            detail.removeAll();
            detail.setLayout(new BorderLayout(0, AcabamentoDialogos.px(8)));
            detail.add(settings, BorderLayout.NORTH);
            detail.add(fieldSection, BorderLayout.CENTER);
            detail.setMinimumSize(new Dimension(0, settings.getPreferredSize().height));
            split.setRightComponent(detail); // The FK form had a redundant single tab.
            master.removeAll();
            master.setLayout(new BorderLayout());
            master.add(keyScroll, BorderLayout.CENTER);
            tools.setOrientation(SwingConstants.HORIZONTAL);
            for (Component component : tools.getComponents()) {
                if (component instanceof JToolBar.Separator) tools.remove(component);
                if (component instanceof JButton button) {
                    button.setText("");
                    button.setFocusable(true);
                    button.getAccessibleContext().setAccessibleName(button.getToolTipText());
                }
            }
            tools.setBorder(BorderFactory.createEmptyBorder());
            master.add(tools, BorderLayout.SOUTH);
            keyScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            DefaultListCellRenderer renderer = new DefaultListCellRenderer();
            keys.setCellRenderer((list, value, index, selected, focus) -> {
                JLabel label = (JLabel) renderer.getListCellRendererComponent(list, value, index, selected, focus);
                label.setToolTipText(label.getPreferredSize().width > list.getFixedCellWidth() ? value : null);
                return label;
            });
            keyScroll.getViewport().addComponentListener(new ComponentAdapter() {
                @Override public void componentResized(ComponentEvent event) {
                    keys.setFixedCellWidth(Math.max(1, keyScroll.getViewport().getWidth()));
                }
            });
            ListDataListener content = new ListDataListener() {
                public void intervalAdded(ListDataEvent event) { resize(); }
                public void intervalRemoved(ListDataEvent event) { resize(); }
                public void contentsChanged(ListDataEvent event) { resize(); }
                private void resize() { largura(split, master, keys, keyScroll, tools); }
            };
            keys.getModel().addListDataListener(content);
            keys.addPropertyChangeListener("model", event -> {
                ((ListModel<?>) event.getOldValue()).removeListDataListener(content);
                ((ListModel<?>) event.getNewValue()).addListDataListener(content);
                largura(split, master, keys, keyScroll, tools);
            });
            keys.addPropertyChangeListener("font", event -> largura(split, master, keys, keyScroll, tools));
            body.add(split, BorderLayout.CENTER);
        }
        body.add(preview, BorderLayout.SOUTH);
        linhasSql(sql, preview);
        AcabamentoDialogos.editor(dialog, selection, body, actions);
        if (split != null) {
            divisor(split);
            split.addPropertyChangeListener("UI", event -> divisor(split));
            split.setResizeWeight(0);
            largura(split, master, keys, keyScroll, tools);
        }
        sql.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent event) { linhasSql(sql, preview); }
            public void removeUpdate(DocumentEvent event) { linhasSql(sql, preview); }
            public void changedUpdate(DocumentEvent event) { linhasSql(sql, preview); }
        });
        sql.addPropertyChangeListener("font", event -> linhasSql(sql, preview));
        // A horizontal SQL scrollbar must not steal a fraction of the last text line.
        preview.getViewport().addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent event) { linhasSql(sql, preview); }
        });
    }

    private static void divisor(JSplitPane split) {
        // FlatLaf scales the divider itself; passing px(16) would scale it twice.
        split.setDividerSize(split.getUI() instanceof com.formdev.flatlaf.ui.FlatSplitPaneUI
                ? 16 : AcabamentoDialogos.px(16));
    }

    private static void largura(JSplitPane split, JPanel master, JList<String> keys,
            JScrollPane scroll, JToolBar tools) {
        int width = tools.getPreferredSize().width;
        var renderer = keys.getCellRenderer();
        for (int i = 0; i < keys.getModel().getSize(); i++) {
            Component cell = renderer.getListCellRendererComponent(keys, keys.getModel().getElementAt(i), i, false, false);
            width = Math.max(width, cell.getPreferredSize().width);
        }
        Insets insets = scroll.getInsets();
        width = Math.min(width, AcabamentoDialogos.px(320)) + insets.left + insets.right
                + scroll.getVerticalScrollBar().getPreferredSize().width;
        master.setPreferredSize(new Dimension(width, 0));
        master.setMinimumSize(new Dimension(width, 0));
        split.setDividerLocation(width);
        master.revalidate();
    }

    private static void linhasSql(JTextArea sql, JScrollPane preview) {
        int lines = Math.max(3, Math.min(6, sql.getLineCount()));
        sql.setRows(lines);
        FontMetrics metrics = sql.getFontMetrics(sql.getFont());
        Insets text = sql.getInsets(), border = preview.getInsets();
        int textWidth = 0;
        for (String line : sql.getText().split("\n", -1)) textWidth = Math.max(textWidth, metrics.stringWidth(line));
        int horizontal = textWidth + text.left + text.right > preview.getViewport().getWidth()
                ? preview.getHorizontalScrollBar().getPreferredSize().height : 0;
        Dimension size = new Dimension(AcabamentoDialogos.px(760),
                lines * metrics.getHeight() + text.top + text.bottom + border.top + border.bottom + horizontal);
        if (!size.equals(preview.getPreferredSize())) {
            preview.setPreferredSize(size);
            preview.revalidate();
        }
    }

    /** Called after Inicie fills the model; pack from actual rows, then cap to usable screen. */
    public static void dimensionar(JDialog dialog, JPanel rows, JScrollPane fields,
            JTextArea sql, JScrollPane preview) {
        int height = 0, count = 0, rowHeight = AcabamentoDialogos.px(48);
        for (Component row : rows.getComponents()) {
            rowHeight = Math.max(rowHeight, row.getPreferredSize().height);
            height += row.getPreferredSize().height;
            count++;
        }
        height += Math.max(0, 3 - count) * rowHeight;
        Insets border = fields.getInsets();
        int width = Math.max(AcabamentoDialogos.px(760), rows.getPreferredSize().width
                + border.left + border.right + fields.getVerticalScrollBar().getPreferredSize().width);
        fields.setPreferredSize(new Dimension(width, height + border.top + border.bottom));
        linhasSql(sql, preview);
        // Resolve scrollbars at the capped width, then reserve their space as well.
        for (int pass = 0; pass < 3; pass++) {
            dialog.pack();
            AcabamentoDialogos.limitar(dialog);
            dialog.validate();
            int horizontal = fields.getHorizontalScrollBar().isVisible()
                    ? fields.getHorizontalScrollBar().getPreferredSize().height : 0;
            fields.setPreferredSize(new Dimension(width, height + border.top + border.bottom + horizontal));
            linhasSql(sql, preview);
        }
    }
}

package util;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;

/** Translated, timestamped log presentation without changing the serialized dialog structure. */
public final class VisualLogs {
    private VisualLogs() {}
    public static String mensagem(String key) {
        try {
            ResourceBundle bundle = ResourceBundle.getBundle("principal/Propriedades_pt_BR");
            if (bundle.containsKey(key)) return bundle.getString(key);
        } catch (MissingResourceException ignored) { }
        String text = key.replaceFirst("^(?:Controler\\.|ERROR_|ERRO_|INFO_|MSG_)", "")
                .replace('_', ' ').replace('.', ' ').toLowerCase(java.util.Locale.forLanguageTag("pt-BR"));
        return text.isEmpty() ? "Mensagem registrada" : Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    public static void preparar(JTable table, JScrollPane scroll, JPanel actions) {
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        int[] widths = {90, 140, 430, 300};
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(AcabamentoDialogos.px(widths[i]));
            table.getColumnModel().getColumn(i).setWidth(AcabamentoDialogos.px(widths[i]));
        }
        table.getColumnModel().getColumn(0).setMaxWidth(AcabamentoDialogos.px(110));
        table.getColumnModel().getColumn(1).setMinWidth(AcabamentoDialogos.px(140));
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
        table.getColumnModel().getColumn(1).setCellRenderer((t, value, selected, focus, row, col) -> {
            JLabel label = (JLabel) renderer.getTableCellRendererComponent(t, value, selected, focus, row, col);
            label.setIcon(Icones.de("/imagens/" + ("Erro".equals(value) ? "log_erro" : "log_info") + ".svg"));
            label.setIconTextGap(AcabamentoDialogos.px(8));
            return label;
        });
        table.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseMoved(MouseEvent event) {
                int row = table.rowAtPoint(event.getPoint());
                table.setToolTipText(row < 0 ? null : String.valueOf(table.getValueAt(row, 3)));
            }
        });
        if (scroll.getClientProperty("brmodelo.vazio.texto") instanceof JLabel hint)
            hint.setText("Nenhuma mensagem registrada.");
        JButton copy = new JButton("Copiar detalhes", Icones.de("/imagens/copy.png"));
        copy.setEnabled(table.getSelectedRow() >= 0);
        table.getSelectionModel().addListSelectionListener(event -> copy.setEnabled(table.getSelectedRow() >= 0));
        copy.addActionListener(event -> {
            int row = table.getSelectedRow();
            if (row < 0) return;
            String text = table.getValueAt(row, 0) + " · " + table.getValueAt(row, 1) + "\n"
                    + table.getValueAt(row, 2) + "\n" + table.getValueAt(row, 3);
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
        });
        actions.add(copy);
        AcabamentoDialogos.acoes(actions);
        java.awt.Container content = actions.getParent();
        content.removeAll();
        content.setLayout(new java.awt.BorderLayout(0, AcabamentoDialogos.px(16)));
        content.add(scroll, java.awt.BorderLayout.CENTER);
        content.add(actions, java.awt.BorderLayout.SOUTH);
        scroll.setPreferredSize(new java.awt.Dimension(AcabamentoDialogos.px(920), AcabamentoDialogos.px(320)));
    }
}

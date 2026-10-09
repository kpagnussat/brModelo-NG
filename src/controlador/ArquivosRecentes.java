package controlador;

import java.io.File;
import java.util.function.Supplier;
import javax.swing.Action;
import javax.swing.JMenuItem;

/** Populates the existing recent-files menu without changing its action owner. */
final class ArquivosRecentes {
    private ArquivosRecentes() {}
    static void reloadMenuRecentes(Editor editor, Supplier<Action> novaAcao) {
        for (int i = 0; i < 10; i++) {
            if (editor.getRecentes().size() > i) {
                String arq = editor.getRecentes().get(i);
                String tmp = arq;
                if (tmp.lastIndexOf(File.separator) > 0) {
                    tmp = tmp.substring(tmp.lastIndexOf(File.separator) + 1);
                }

                if (editor.getMenuRecente().getItemCount() > i + 2) {
                    editor.getMenuRecente().getItem(i).setText(tmp);
                    editor.getMenuRecente().getItem(i).setToolTipText(arq);
                } else {
                    JMenuItem jmi = new JMenuItem(tmp);
                    jmi.setToolTipText(arq);
                    jmi.addActionListener(novaAcao.get());
                    editor.getMenuRecente().add(jmi, editor.getMenuRecente().getItemCount() - 2);
                }
            } else {
                break;
            }
        }
        editor.getMenuRecente().setEnabled(editor.getMenuRecente().getItemCount() > 2);
        }
}

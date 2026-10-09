/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package util;

import controlador.Diagrama;
import controlador.Editor;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;

/**
 *
 * @author ccandido
 */
public class Dialogos {

    public static int ShowMessageSave(Diagrama afechar) {
        String arq = afechar.getNomeFormatado();
        return (JOptionPane.showConfirmDialog(afechar.getEditor().getParent(), Editor.fromConfiguracao.getValor("Controler.MSG_SAVE") + " " +
                arq,Editor.fromConfiguracao.getValor("Controler.MSG_SAVE_TITLE"), JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE)); 
    }

    public static int ShowMessageConfirm(Component parente, String txtAdicional) {
        if (parente != null) parente.requestFocus();
        return (JOptionPane.showConfirmDialog(parente, Editor.fromConfiguracao.getValor("Controler.MSG_CONFIRM") + (txtAdicional.isEmpty() ? "?"  : " " + txtAdicional),
                Editor.fromConfiguracao.getValor("Controler.MSG_CONFIRM_TITLE"),
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE)); 
    }
    public static int ShowMessageConfirm(Component parente, String msgTexto, boolean msgConfirm) {
        if (msgConfirm) {
            return ShowMessageConfirm(parente, msgTexto);
        }
        if (parente != null) parente.requestFocus();
        return (JOptionPane.showConfirmDialog(parente, (msgTexto.isEmpty() ? "?"  : " " + msgTexto),
                Editor.fromConfiguracao.getValor("Controler.MSG_CONFIRM_TITLE"),
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE)); 
    }
    public static boolean ShowMessageConfirmYES(Component parente, String txtAdicional) {
        return (ShowMessageConfirm(parente, txtAdicional) == JOptionPane.YES_OPTION);
    }
    public static boolean ShowMessageConfirmYES(Component parente, String txtAdicional, boolean msgConfirm) {
        return (ShowMessageConfirm(parente, txtAdicional, msgConfirm) == JOptionPane.YES_OPTION);
    }
    public static void ShowMessageInform(Component parente, String texto) {
        JOptionPane.showMessageDialog(parente, texto,
                Editor.fromConfiguracao.getValor("Controler.MSG_INFORM_TITLE"),
                JOptionPane.INFORMATION_MESSAGE); 
    }

    public static void ShowMessageERROR(Component parente, String texto) {
        JOptionPane.showMessageDialog(parente, texto,
                Editor.fromConfiguracao.getValor("Controler.MSG_ERROR_TITLE"),
                JOptionPane.ERROR_MESSAGE); 
    }

    public Dialogos() {
        super();
    }

    public static String ShowDlgTexto(JComponent form, String texto) {
        DlgExecutor dlg = new DlgExecutor((Frame) form.getParent(), true);
        dlg.Texto.setText(texto);
        dlg.setLocationRelativeTo(form);
        dlg.setVisible(true);

        if (dlg.getResultado() == JOptionPane.OK_OPTION) {
            return dlg.Texto.getText();
        }
        return texto;
    }

    public static String ShowDlgTexto(JComponent form, String texto, String original) {
        DlgExecutor dlg = new DlgExecutor((Frame) form.getParent(), true);
        dlg.Texto.setText(texto);
        dlg.setLocationRelativeTo(form);
        dlg.setVisible(true);

        if (dlg.getResultado() == JOptionPane.OK_OPTION) {
            return dlg.Texto.getText();
        }
        return original;
    }

    public static void ShowDlgTextoReadOnly(JComponent form, String texto) {
        DlgExecutor dlg = new DlgExecutor((Frame) form.getParent(), true);
        dlg.Texto.setText(texto);
        dlg.Texto.setEditable(false);
        dlg.Texto.setForeground(util.EstiloUI.texto(dlg.Texto, "Label.foreground"));
        dlg.Texto.setCaretPosition(0);
        dlg.setLocationRelativeTo(form);
        dlg.btnCancelar.setVisible(false);
        dlg.setVisible(true);

    }

    public static Color c = Color.BLACK;

    public static String ShowDlgCor(JComponent form, String textoCor, Diagrama modelo) {
        try {
            c = Utilidades.StringToColor(textoCor);
        } catch (Exception e) {
            // Keep the previous chooser color when the stored color cannot be parsed.
        }
        final JColorChooser jcc = new JColorChooser();
        jcc.addChooserPanel(new PainelSelecaoCor(modelo));
        jcc.setColor(c);

        JDialog dialog = JColorChooser.createDialog(form,
                Editor.fromConfiguracao.getValor("Controler.MSG_CHOOSE_COLLOR"),
                true, jcc, new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        c = jcc.getColor();
                    }
                },
                null);
        dialog.setVisible(true);
        return c == null ? textoCor : Utilidades.ColorToString(c);
    }

    public static String ShowDlgCor(JComponent form, String textoCor) {
        c = Color.BLACK;
        try {
            c = util.Utilidades.StringToColor(textoCor);
        } catch (Exception e) {
            // Keep the black default when the stored color cannot be parsed.
        }
        c = JColorChooser.showDialog(form, Editor.fromConfiguracao.getValor("Controler.MSG_CHOOSE_COLLOR"), c);
        return c == null ? textoCor : util.Utilidades.ColorToString(c);
    }

    public static String ShowDlgInputText(JComponent form, String textoCor) {
        String res = JOptionPane.showInputDialog(form,
                Editor.fromConfiguracao.getValor("Controler.MSG_INPUT_TEXT_LABEL"), textoCor);
        return res == null ? "" : res;
    }

    private static final String ULTIMA_PASTA = "cfg.seletor.ultimaPasta";

    private static File pastaInicial(String anterior) {
        if (anterior != null && !anterior.isEmpty()) {
            return SeletorDeArquivos.pastaDe(new File(anterior));
        }
        if (Editor.fromConfiguracao.hasValor(ULTIMA_PASTA)) {
            File pasta = new File(Editor.fromConfiguracao.getValor(ULTIMA_PASTA));
            if (pasta.isDirectory()) return pasta;
        }
        return new File(System.getProperty("user.dir"));
    }

    private static File selecionar(Component pai, boolean salvar, String titulo, String aceitar,
                                   String anterior, String nome, java.util.List<SeletorDeArquivos.Filtro> filtros) {
        File file = SeletorDeArquivos.escolher(pai, new SeletorDeArquivos.Pedido(
                salvar, titulo, aceitar, pastaInicial(anterior), nome, filtros, 0));
        if (file != null) {
            Editor.fromConfiguracao.SetAndSaveIfNeed(ULTIMA_PASTA, file.getAbsoluteFile().getParent());
        }
        return file;
    }

    public static String ShowDlgFileImg(JComponent form) {
        String titulo = Editor.fromConfiguracao.getValor("Controler.dlg.image");
        File file = selecionar(form, false, titulo, titulo, null, "",
                java.util.List.of(new SeletorDeArquivos.Filtro("Todos os arquivos")));
        return file == null ? null : file.getAbsolutePath();
    }

    static java.util.List<SeletorDeArquivos.Filtro> filtrosDiagrama(boolean abrir) {
        java.util.List<SeletorDeArquivos.Filtro> filtros = new java.util.ArrayList<>();
        if (abrir) filtros.add(new SeletorDeArquivos.Filtro("Todos os arquivos brModelo", Arquivo.brM3, Arquivo.brMj, Arquivo.xml));
        filtros.add(new SeletorDeArquivos.Filtro("BrModelo(bin)", Arquivo.brM3));
        filtros.add(new SeletorDeArquivos.Filtro("BrModelo JSON (.brMj)", Arquivo.brMj));
        filtros.add(new SeletorDeArquivos.Filtro("BrModelo(xml)", Arquivo.xml));
        if (abrir) filtros.add(new SeletorDeArquivos.Filtro("Todos os arquivos"));
        return java.util.List.copyOf(filtros);
    }

    static java.util.List<SeletorDeArquivos.Filtro> filtrosImagem() {
        return java.util.List.of(new SeletorDeArquivos.Filtro("Imagem (png)", Arquivo.png),
                new SeletorDeArquivos.Filtro("Imagem (bmp)", Arquivo.bmp));
    }

    public static File ShowDlgSaveDiagrama(JComponent form, Diagrama diag) {
        return selecionar(form, true, Editor.fromConfiguracao.getValor("Controler.MSG_SAVE_TITLE") + " " + diag.getNomeFormatado(),
                "Salvar", null, diag.getNome(), filtrosDiagrama(false));
    }

    public static File ShowDlgSaveAsImg(JComponent form, Diagrama diag) {
        return selecionar(form, true, Editor.fromConfiguracao.getValor("Controler.MSG_EPRT_TITLE"),
                "Salvar", null, diag.getNome(), filtrosImagem());
    }

    public static File ShowDlgSaveAsAny(JComponent form, String ar) {
        File sugestao = ar == null || ar.isEmpty() ? null : new File(ar);
        return selecionar(form, true, Editor.fromConfiguracao.getValor("Controler.MSG_EPRT_TITLE"),
                "Salvar", sugestao != null && sugestao.isAbsolute() ? ar : null,
                sugestao == null ? "" : sugestao.getName(),
                java.util.List.of(new SeletorDeArquivos.Filtro("Todos os arquivos")));
    }

    /** preDir can name either a directory or a previously opened file. */
    public static File ShowDlgLoadDiagrama(String preDir, Editor master) {
        return selecionar((Component) master.getFramePrincipal(), false, "Abrir diagrama", "Abrir",
                preDir, "", filtrosDiagrama(true));
    }

    public static JFontChooser JFC = new JFontChooser();
    public static Font ShowDlgFont(JComponent form, Font selected){
        JFontChooser fc = JFC;
        fc.setSelectedFont(selected);
        if (fc.showDialog(form) == JFontChooser.OK_OPTION) {
            fc.makeLastRegistred();
            return fc.getSelectedFont();
        }
        return null;
    }
}

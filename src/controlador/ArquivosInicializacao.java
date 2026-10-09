package controlador;

import java.io.File;
import javax.swing.SwingUtilities;

/** Opens launcher arguments through the same persistence path as the editor menu. */
public final class ArquivosInicializacao {
    private ArquivosInicializacao() {}

    public static void abrir(Editor editor, String[] argumentos) {
        if (!SwingUtilities.isEventDispatchThread()) throw new IllegalStateException("Open files on the EDT");
        for (String argumento : argumentos) {
            File file = new File(argumento).getAbsoluteFile();
            if (!file.isFile()) {
                System.getLogger(ArquivosInicializacao.class.getName()).log(System.Logger.Level.WARNING,
                        "Cannot open file: " + file);
                continue;
            }
            var opened = editor.getDiagramas().stream().filter(d -> file.getPath().equals(d.getArquivo())).findFirst();
            if (opened.isPresent()) editor.setSelected(opened.get());
            else {
                ArquivosEditor.AbrirDiagramaFromFile(editor, file);
                if (file.getPath().equals(editor.diagramaAtual.getArquivo())) {
                    System.getLogger(ArquivosInicializacao.class.getName()).log(System.Logger.Level.INFO,
                            "Opened diagram: " + file + " (" + editor.diagramaAtual.getListaDeItens().size() + " elements)");
                }
            }
        }
    }
}

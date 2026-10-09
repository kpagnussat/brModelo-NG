import java.awt.Window;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.swing.SwingUtilities;

/** Manual desktop check: selections are printed, never written to the chosen files. */
public final class SeletorDemo {
    private SeletorDemo() {}
    public static void main(String[] args) throws Exception {
        System.setProperty("brmodelo.seletor", args.length == 0 ? "portal" : args[0]);
        principal.FramePrincipal[] frame = new principal.FramePrincipal[1];
        try {
            SwingUtilities.invokeAndWait(() -> {
                frame[0] = new principal.FramePrincipal();
                principal.Aplicacao.fmPrincipal = frame[0];
                frame[0].setTitle("brModelo — teste do seletor (estado isolado)");
                frame[0].setSize(1000, 700);
                frame[0].setVisible(true);
                var editor = frame[0].getEditor();
                var root = frame[0].getRootPane();
                var diagram = editor.diagramaAtual;
                System.out.println("START ShowDlgFileImg");
                System.out.println("RESULT " + util.Dialogos.ShowDlgFileImg(root));
                System.out.println("START ShowDlgSaveDiagrama");
                System.out.println("RESULT " + util.Dialogos.ShowDlgSaveDiagrama(root, diagram));
                System.out.println("START ShowDlgSaveAsImg");
                System.out.println("RESULT " + util.Dialogos.ShowDlgSaveAsImg(root, diagram));
                System.out.println("START ShowDlgSaveAsAny");
                System.out.println("RESULT " + util.Dialogos.ShowDlgSaveAsAny(root, "consulta.sql"));
                System.out.println("START ShowDlgLoadDiagrama");
                System.out.println("RESULT " + util.Dialogos.ShowDlgLoadDiagrama("", editor));
            });
        } finally {
            CountDownLatch stopped = new CountDownLatch(1);
            if (frame[0] != null) {
                SwingUtilities.invokeAndWait(() -> frame[0].getEditor().EndAutoSave(stopped::countDown));
                if (!stopped.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Autosave did not stop");
            }
            SwingUtilities.invokeAndWait(() -> {
                for (Window window : Window.getWindows()) window.dispose();
                principal.Aplicacao.fmPrincipal = null;
            });
        }
    }
}

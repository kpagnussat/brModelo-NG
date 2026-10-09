package principal;

import java.awt.Color;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.UIManager;
import javax.swing.border.Border;
import util.BrLogger;
import util.ContagemLogs;
import util.Icones;

/** Presents transient messages and the unread log indicator on the main window. */
final class BarraStatus {
    private BarraStatus() {}

    static Border separador() {
        Color cor = util.EstiloUI.separador();
        return BorderFactory.createMatteBorder(1, 0, 0, 0, cor);
    }

    static void iniciar(JLabel mensagem, JButton indicador, FramePrincipal frame) {
        mensagem.setText("");
        BrLogger.setStatus(mensagem);
        indicador.addActionListener(event -> abrirLog(frame));
        BrLogger.setAtualizarIndicador(() -> atualizar(indicador));
    }

    static void atualizar(JButton indicador) {
        ContagemLogs.Estado estado = BrLogger.novasMensagens();
        indicador.setIcon(Icones.de("/imagens/" + estado.icone() + ".svg"));
        indicador.setText(estado.total() == 0 ? "" : Integer.toString(estado.total()));
        indicador.setToolTipText(estado.total() == 0 ? util.DicasInterface.texto("logs") : estado.descricao());
        indicador.getAccessibleContext().setAccessibleName(indicador.getToolTipText());
    }

    static void abrirLog(FramePrincipal frame) {
        FormaLogs log = new FormaLogs(frame, true);
        log.setLocationRelativeTo(frame);
        BrLogger.marcarLidas();
        log.setVisible(true);
    }
}

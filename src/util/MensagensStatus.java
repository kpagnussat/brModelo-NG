package util;

import java.awt.Color;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;

/** Shares one expiring message slot between editor notifications and log entries. */
public final class MensagensStatus {
    private static final String TIMER = "brmodelo.status.timer";
    private MensagensStatus() {}

    public static Timer mostrar(JLabel label, String texto, boolean erro) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> mostrar(label, texto, erro));
            return null;
        }
        if (label == null) return null;
        parar(label);
        label.putClientProperty("brmodelo.status.erro", erro);
        label.setForeground(erro ? corErro() : UIManager.getColor("Label.foreground"));
        label.setText(texto);
        Timer timer = new Timer(5000, event -> label.setText(""));
        timer.setRepeats(false);
        label.putClientProperty(TIMER, timer);
        timer.start();
        return timer;
    }

    public static void parar(JLabel label) {
        if (label != null && label.getClientProperty(TIMER) instanceof Timer timer) {
            timer.stop();
            label.putClientProperty(TIMER, null);
        }
    }

    public static Color corErro() {
        return EstiloUI.cor("Actions.Red");
    }
}

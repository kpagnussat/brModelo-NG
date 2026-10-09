package util;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;

/** Releases are opened only on an explicit user action; startup never checks the network. */
public final class AtualizacoesNG {
    public static final String RELEASES = "https://github.com/kpagnussat/brModelo-NG/releases";
    private AtualizacoesNG() {}

    public static void abrirReleases() {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE))
                Desktop.getDesktop().browse(URI.create(RELEASES));
        } catch (IOException | SecurityException e) {
            BrLogger.Logger("ERROR_BROWSER_OPEN", e.getMessage());
        }
    }
}

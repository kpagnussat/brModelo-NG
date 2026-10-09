package util;

import com.formdev.flatlaf.ui.FlatRootPaneUI;
import com.formdev.flatlaf.util.UIScale;
import java.util.Locale;
import javax.swing.*;
import javax.swing.plaf.ComponentUI;

/**
 * Window frame per platform. Windows uses FlatLaf's frame by default (backed by the
 * native window manager there: snap layouts, system shadow and rounded corners).
 * Linux and macOS keep the system frame: on Linux a custom frame cannot get the
 * compositor's shadow, and rounding it needs a translucent window, which Java repaints
 * transparent on every resize step. {@code -Dbrmodelo.decoracoes=modernas|nativas}
 * overrides the default. All additional state belongs to non-serializable UI helpers.
 */
public final class DecoracoesJanela {
    private DecoracoesJanela() {}

    public static boolean modernas() {
        return modernas(System.getProperty("brmodelo.decoracoes", ""), System.getProperty("os.name", ""));
    }

    static boolean modernas(String escolha, String os) {
        if ("modernas".equalsIgnoreCase(escolha)) return true;
        if ("nativas".equalsIgnoreCase(escolha)) return false;
        return os.toLowerCase(Locale.ROOT).contains("windows");
    }

    public static void iniciar() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("mac")) {
            System.setProperty("apple.laf.useScreenMenuBar", "true");
            if (modernas()) System.setProperty("apple.awt.application.appearance", "system");
        }
        boolean custom = modernas() && (os.contains("windows") || os.contains("linux"));
        System.setProperty("flatlaf.useWindowDecorations", Boolean.toString(custom));
        JFrame.setDefaultLookAndFeelDecorated(custom);
        JDialog.setDefaultLookAndFeelDecorated(custom);
    }

    static void defaults() {
        if (!modernas()) return;
        UIManager.put("TitlePane.menuBarEmbedded", true);
        for (String key : new String[]{"TitlePane.background", "TitlePane.inactiveBackground",
                "TitlePane.embeddedBackground", "TitlePane.buttonBackground",
                "TitlePane.buttonInactiveBackground"})
            UIManager.put(key, EstiloUI.cor("Panel.background"));
        for (String key : new String[]{"TitlePane.foreground", "TitlePane.inactiveForeground",
                "TitlePane.embeddedForeground"})
            UIManager.put(key, EstiloUI.cor("Label.foreground"));
        // A custom frame on Linux has no compositor shadow, so its outline is the only edge
        // between a dialog and the window behind it; FlatLaf's default is too faint in dark.
        UIManager.put("RootPane.activeBorderColor", EstiloUI.cor("Component.borderColor"));
        if (System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("mac"))
            UIManager.put("RootPaneUI", RaizMac.class.getName());
    }

    /** Native macOS frame; reserve the title row so traffic lights never cover content. */
    public static final class RaizMac extends FlatRootPaneUI {
        private JComponent content;
        private javax.swing.border.Border previousBorder;

        public static ComponentUI createUI(JComponent component) { return new RaizMac(); }

        @Override public void installUI(JComponent component) {
            JRootPane root = (JRootPane) component;
            root.putClientProperty("apple.awt.fullWindowContent", true);
            root.putClientProperty("apple.awt.transparentTitleBar", true);
            root.putClientProperty("apple.awt.windowTitleVisible", false);
            super.installUI(component);
            if (root.getContentPane() instanceof JComponent panel) {
                content = panel;
                previousBorder = panel.getBorder();
                panel.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createEmptyBorder(UIScale.scale(28), 0, 0, 0), previousBorder));
            }
        }

        @Override public void uninstallUI(JComponent component) {
            if (content != null) content.setBorder(previousBorder);
            super.uninstallUI(component);
        }
    }
}

package util;

import java.util.Locale;

/** Java2D pipeline choice, applied before AWT starts. */
public final class Renderizacao {
    private Renderizacao() {}
    static final String XRENDER = "sun.java2d.xrender", PIXMAPS = "sun.java2d.pmoffscreen";

    /**
     * Java 21 draws through X11 on Linux (XWayland on Wayland sessions). With XRender every
     * antialiased shape becomes a mask that the X server composites on the CPU, and FlatLaf
     * antialiases almost everything: resizing kept XWayland at a full core. Without XRender,
     * Swing's back buffer still lived in an X pixmap, so each translucent blend read pixels back
     * from the server. Rendering into a back buffer in Java memory and sending finished frames
     * fixes both (measured on GNOME). Explicit -D values still win.
     */
    public static void configurar() {
        String os = System.getProperty("os.name", "");
        for (String chave : new String[]{XRENDER, PIXMAPS}) {
            String valor = padrao(os, System.getProperty(chave));
            if (valor != null) System.setProperty(chave, valor);
        }
    }

    /** The value to set, or null to leave the property as it is. */
    static String padrao(String os, String atual) {
        if (atual != null) return null;
        return os.toLowerCase(Locale.ROOT).contains("linux") ? "false" : null;
    }
}

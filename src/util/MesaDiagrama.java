package util;

import java.awt.*;

/** Editor-only page surroundings; never used by Diagrama.ExternalPaint or exporters. */
public final class MesaDiagrama {
    private MesaDiagrama() {}

    /**
     * A thin outline separates the white page from the desk. There is no drop shadow: its
     * antialiased fills were the most expensive thing painted while resizing the window.
     */
    public static void pintar(Graphics2D g, Rectangle page) {
        Object antialias = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        // An axis-aligned one-pixel outline gains nothing from antialiasing.
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setColor(EstiloUI.separador());
        g.drawRect(page.x - 1, page.y - 1, page.width + 1, page.height + 1);
        if (antialias != null) g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, antialias);
    }
}

package util;

import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.Rectangle;
import java.awt.Window;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.text.JTextComponent;

/**
 * Sizes for widgets that were laid out with fixed pixel dimensions (designed for a 12pt
 * Metal font). Used where the code builds rows by hand instead of a layout manager.
 */
public final class Escala {

    /** Font size the fixed pixel sizes were designed for. */
    private static final float FONTE_PROJETO = 12f;

    private Escala() {
    }

    /** How much bigger the component's font is than the design font (never below 1). */
    public static float fator(JComponent c) {
        return c.getFont() == null ? 1f : Math.max(1f, c.getFont().getSize2D() / FONTE_PROJETO);
    }

    /**
     * Preferred size for a widget designed as largura x altura px: the width grows with the
     * font, and the height is never smaller than what the look-and-feel needs for the text
     * (the old fixed 20px clipped text fields under larger theme fonts).
     * Widgets showing the row's own text (field name box, field checkbox) keep the scaled width
     * even when the text is longer, so every row has the same column widths; a long name is
     * clipped as in the original design instead of shifting that row's columns. Other widgets
     * (combos, buttons) may still grow to what the look-and-feel needs.
     */
    public static Dimension ajuste(JComponent c, int largura, int altura) {
        c.setPreferredSize(null);
        Dimension natural = c.getPreferredSize();
        int w = Math.round(largura * fator(c));
        if (!(c instanceof JTextComponent || c instanceof JToggleButton)) {
            w = Math.max(w, natural.width);
        }
        return new Dimension(w, Math.max(altura, natural.height));
    }

    /**
     * Widens a window so the scroll pane shows its content's full width (rows built after the
     * window was packed, wider under larger fonts), capped at 95% of the screen. Never shrinks.
     */
    public static void alargue(Window w, JScrollPane sp) {
        if (sp.getViewport().getView() == null) {
            return;
        }
        w.validate();
        int precisa = sp.getViewport().getView().getPreferredSize().width
                + sp.getVerticalScrollBar().getPreferredSize().width + 4;
        int falta = precisa - sp.getViewport().getWidth();
        if (falta <= 0) {
            return;
        }
        cresca(w, w.getWidth() + falta, w.getHeight());
    }

    /**
     * Grows a window to its current preferred size, for windows whose texts are filled in after
     * they were packed (the print dialog's page count). Capped at 95% of the screen; never
     * shrinks.
     */
    public static void cresca(Window w) {
        Dimension pref = w.getPreferredSize();
        cresca(w, pref.width, pref.height);
    }

    private static void cresca(Window w, int largura, int altura) {
        GraphicsConfiguration gc = w.getGraphicsConfiguration();
        Rectangle tela = gc != null ? gc.getBounds() : new Rectangle(0, 0, Integer.MAX_VALUE, Integer.MAX_VALUE);
        largura = Math.max(w.getWidth(), Math.min(largura, (int) (tela.width * 0.95)));
        altura = Math.max(w.getHeight(), Math.min(altura, (int) (tela.height * 0.95)));
        if (largura > w.getWidth() || altura > w.getHeight()) {
            w.setSize(largura, altura);
            w.setLocationRelativeTo(w.getOwner());
            w.validate();
        }
    }
}

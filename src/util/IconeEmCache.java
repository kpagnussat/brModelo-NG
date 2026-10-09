package util;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import javax.swing.Icon;
import javax.swing.UIManager;

/**
 * An SVG icon rasterized once per device scale and theme, then only copied. FlatSVGIcon
 * re-renders the vector on every paint, and a window resize repaints every palette and toolbar
 * button on every frame; the original brModelo stays fast because its icons are bitmaps.
 */
final class IconeEmCache implements Icon, FlatLaf.DisabledIconProvider {
    private final FlatSVGIcon svg;
    private BufferedImage imagem;
    private double escala;
    private Object tema;
    private Icon desabilitado;

    IconeEmCache(FlatSVGIcon svg) {
        this.svg = svg;
    }

    /** The SVG behind the raster (its name and source), for code that inspects icons. */
    FlatSVGIcon vetor() { return svg; }

    @Override public int getIconWidth() { return svg.getIconWidth(); }
    @Override public int getIconHeight() { return svg.getIconHeight(); }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        if (!(g instanceof Graphics2D g2)) {
            svg.paintIcon(c, g, x, y);
            return;
        }
        AffineTransform t = g2.getTransform();
        double s = Math.max(.25, Math.round(Math.hypot(t.getScaleX(), t.getShearY()) * 100) / 100.0);
        // Theme colors reach the SVG through FlatSVGIcon's global color filter, which follows
        // the look and feel; every theme switch installs a new look-and-feel instance.
        Object laf = UIManager.getLookAndFeel();
        int w = getIconWidth(), h = getIconHeight();
        if (imagem == null || s != escala || laf != tema
                || imagem.getWidth() != (int) Math.ceil(w * s) || imagem.getHeight() != (int) Math.ceil(h * s)) {
            BufferedImage nova = new BufferedImage(Math.max(1, (int) Math.ceil(w * s)),
                    Math.max(1, (int) Math.ceil(h * s)), BufferedImage.TYPE_INT_ARGB);
            Graphics2D ig = nova.createGraphics();
            try {
                ig.scale(s, s);
                svg.paintIcon(c, ig, 0, 0);
            } finally {
                ig.dispose();
            }
            imagem = nova;
            escala = s;
            tema = laf;
        }
        // Device pixels map one to one onto the cached raster, so it stays as sharp as the SVG.
        AffineTransform posicao = AffineTransform.getTranslateInstance(x, y);
        posicao.scale(1 / s, 1 / s);
        g2.drawImage(imagem, posicao, null);
    }

    @Override
    public Icon getDisabledIcon() {
        if (desabilitado == null && svg.getDisabledIcon() instanceof FlatSVGIcon gray)
            desabilitado = new IconeEmCache(gray);
        return desabilitado != null ? desabilitado : svg.getDisabledIcon();
    }
}

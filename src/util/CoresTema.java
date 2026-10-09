package util;

import java.awt.Color;
import javax.swing.UIManager;

/**
 * Theme-aware colors for the hand-painted parts of the interface (Inspector rows, status
 * strips, highlight panels). The diagram itself is a document and stays white on purpose.
 *
 * On a light theme every method returns the original color untouched, so the classic look
 * is preserved pixel for pixel. On a dark theme (panel background darker than mid-gray)
 * the original is translated into the theme's own palette:
 *  - background colors keep their distance from white as a distance from the theme
 *    background, plus a softened version of their tint (lavender stays bluish);
 *  - grays used as ink (black, gray, light gray) become blends of the theme's text and
 *    background colors with the same contrast ratio;
 *  - dark saturated ink (red errors, blue links) is lifted toward white, same hue.
 */
public final class CoresTema {

    private CoresTema() {
    }

    /** Theme panel background (falls back to Metal's when the look-and-feel has none). */
    public static Color fundoTema() {
        Color c = UIManager.getColor("Panel.background");
        return c != null ? c : new Color(238, 238, 238);
    }

    /** Theme text color. */
    public static Color textoTema() {
        Color c = UIManager.getColor("Label.foreground");
        return c != null ? c : Color.BLACK;
    }

    /** True when the current look-and-feel paints dark panels. */
    public static boolean escuro() {
        return luminancia(fundoTema()) < 0.5;
    }

    /** Background color designed for a light theme, translated to the current theme. */
    public static Color fundo(Color original) {
        if (!escuro()) {
            return original;
        }
        Color base = fundoTema();
        // How far the original sits below white becomes how far above the dark base we sit.
        int claro = (int) Math.round((1.0 - luminancia(original)) * 255);
        int media = (original.getRed() + original.getGreen() + original.getBlue()) / 3;
        return new Color(
                limite(base.getRed() + claro + (original.getRed() - media) / 2),
                limite(base.getGreen() + claro + (original.getGreen() - media) / 2),
                limite(base.getBlue() + claro + (original.getBlue() - media) / 2),
                original.getAlpha());
    }

    /** Ink (text/line) color designed for a light theme, translated to the current theme. */
    public static Color traco(Color original) {
        if (!escuro()) {
            return original;
        }
        if (cinza(original)) {
            // Black = full text color, white = background; grays keep their relative contrast.
            double peso = 1.0 - original.getRed() / 255.0;
            return mistura(textoTema(), fundoTema(), peso);
        }
        // Dark saturated ink (pure red, pure blue) is hard to read on a dark background:
        // lift it toward white, keeping the hue (red -> light red, blue -> periwinkle).
        return luminancia(original) < 0.4 ? mistura(original, Color.WHITE, 0.6) : original;
    }

    /** Linear blend: peso=1 gives a, peso=0 gives b. */
    public static Color mistura(Color a, Color b, double peso) {
        return new Color(
                limite((int) Math.round(a.getRed() * peso + b.getRed() * (1 - peso))),
                limite((int) Math.round(a.getGreen() * peso + b.getGreen() * (1 - peso))),
                limite((int) Math.round(a.getBlue() * peso + b.getBlue() * (1 - peso))));
    }

    /** Relative luminance (sRGB weights, no gamma), 0 = black, 1 = white. */
    public static double luminancia(Color c) {
        return (0.2126 * c.getRed() + 0.7152 * c.getGreen() + 0.0722 * c.getBlue()) / 255.0;
    }

    private static boolean cinza(Color c) {
        return c.getRed() == c.getGreen() && c.getGreen() == c.getBlue();
    }

    private static int limite(int v) {
        return Math.max(0, Math.min(255, v));
    }
}

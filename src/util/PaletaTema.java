package util;

import java.awt.Color;
import java.util.function.Function;

/** Semantic fallbacks shared by custom painting and previews, including contrast floors. */
public final class PaletaTema {
    private PaletaTema() {}
    public static Color cor(Function<String, Color> cores, String key) {
        Color bg = primeiro(cores, new Color(238, 238, 238), "Panel.background");
        Color fg = contraste(primeiro(cores, Color.BLACK, "Label.foreground"), bg, 4.5);
        Color raw = cores.apply(key);
        if (key.equals("Panel.background")) return bg;
        if (key.equals("Label.foreground")) return fg;
        if (key.equals("Label.disabledForeground"))
            return contraste(raw != null ? raw : CoresTema.mistura(fg, bg, .6), bg, 3);
        if (key.equals("Component.accentColor") || key.equals("Component.focusColor"))
            return contraste(primeiro(cores, new Color(50, 110, 190), key,
                    "Component.focusColor", "List.selectionBackground"), bg, 3);
        if (key.endsWith("selectionInactiveBackground"))
            return raw != null ? raw : primeiro(cores, CoresTema.mistura(fg, bg, .15),
                    key.replace("selectionInactive", "selection"));
        if (key.endsWith("selectionForeground") || key.endsWith("selectionInactiveForeground")) {
            Color selection = cor(cores, key.replace("Foreground", "Background"));
            return contraste(raw != null ? raw : fg, selection, 4.5);
        }
        if (key.equals("Component.error.focusedBorderColor") || key.equals("Actions.Red")) {
            Color error = primeiro(cores, new Color(190, 45, 55), key,
                    "Actions.Red", "Component.error.focusedBorderColor");
            // Some IntelliJ adaptations map Actions.Red to their green primary color.
            if (error.getGreen() > error.getRed())
                error = primeiro(cores, new Color(190, 45, 55), "Component.error.focusedBorderColor");
            if (error.getGreen() > error.getRed()) error = new Color(190, 45, 55);
            return contraste(error, bg, 4.5);
        }
        if (raw != null) return raw;
        if (key.endsWith("selectionBackground"))
            return primeiro(cores, CoresTema.mistura(fg, bg, .2), "List.selectionBackground");
        if (key.equals("Component.borderColor") || key.equals("Separator.foreground"))
            return primeiro(cores, CoresTema.mistura(fg, bg, .35), "Component.borderColor", "Separator.foreground");
        if (key.contains("hoverBackground")) return CoresTema.mistura(fg, bg, .08);
        if (key.contains("pressedBackground")) return CoresTema.mistura(fg, bg, .14);
        return key.endsWith("Background") || key.endsWith("background") ? bg : fg;
    }
    private static Color primeiro(Function<String, Color> cores, Color fallback, String... keys) {
        for (String key : keys) { Color c = cores.apply(key); if (c != null) return c; }
        return fallback;
    }
    /** WCAG sRGB contrast, also used by the catalog audit. */
    public static double razao(Color a, Color b) {
        double x = luz(a), y = luz(b);
        return (Math.max(x, y) + .05) / (Math.min(x, y) + .05);
    }
    private static double luz(Color c) {
        return .2126 * linear(c.getRed()) + .7152 * linear(c.getGreen()) + .0722 * linear(c.getBlue());
    }
    private static double linear(int v) { double s = v / 255.; return s <= .04045 ? s / 12.92 : Math.pow((s + .055) / 1.055, 2.4); }
    public static Color contraste(Color ink, Color bg, double min) {
        if (razao(ink, bg) >= min) return ink;
        Color end = razao(Color.WHITE, bg) > razao(Color.BLACK, bg) ? Color.WHITE : Color.BLACK;
        for (int step = 1; step <= 100; step++) {
            Color c = CoresTema.mistura(end, ink, step / 100.);
            if (razao(c, bg) >= min) return c;
        }
        return end;
    }
}

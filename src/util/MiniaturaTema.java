package util;

import com.formdev.flatlaf.util.UIScale;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import javax.swing.*;

/** Cached mini brModelo, painted from each LAF's defaults without installing that LAF. */
public final class MiniaturaTema {
    private static final Map<String, Icon> CACHE = new HashMap<>();
    private MiniaturaTema() {}
    public static Icon obter(CatalogoTemas.Tema tema) {
        float scale = UIScale.getUserScaleFactor();
        return CACHE.computeIfAbsent(tema.id() + ":" + scale, key -> renderizar(tema, scale));
    }
    private static Icon renderizar(CatalogoTemas.Tema tema, float scale) {
        UIDefaults defaults = tema.fabrica().get().getDefaults();
        Function<String, Color> cor = key -> PaletaTema.cor(defaults::getColor, key);
        BufferedImage image = new BufferedImage(Math.round(152 * scale), Math.round(80 * scale), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.scale(scale, scale);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color bg = cor.apply("Panel.background"), fg = cor.apply("Label.foreground");
            Color accent = cor.apply("Component.accentColor");
            g.setColor(bg); g.fillRect(0, 0, 152, 80);
            g.setColor(fg);
            for (int x = 7; x < 100; x += 13) g.drawRoundRect(x, 5, 6, 6, 2, 2);
            g.setColor(cor.apply("Component.borderColor")); g.drawLine(0, 16, 152, 16);
            g.drawLine(52, 16, 52, 80);
            g.setColor(cor.apply("Table.selectionBackground")); g.fillRect(0, 22, 51, 12);
            g.setFont(new Font(Font.DIALOG, Font.PLAIN, 6));
            g.setColor(cor.apply("Table.selectionForeground")); g.drawString("Nome    Cliente", 4, 30);
            g.setColor(fg); g.drawString("Cor       Branco", 4, 43);
            g.setColor(cor.apply("Label.disabledForeground")); g.drawString("Tipo      Entidade", 4, 55);
            g.setColor(cor.apply("Component.borderColor"));
            g.drawLine(0, 36, 51, 36); g.drawLine(0, 48, 51, 48); g.drawLine(0, 60, 51, 60);
            g.setColor(CoresTema.mistura(fg, bg, .08)); g.fillRect(53, 17, 86, 63);
            g.setColor(new Color(0, 0, 0, 40)); g.fillRect(63, 23, 69, 54);
            g.setColor(Color.WHITE); g.fillRect(61, 21, 69, 54);
            g.setColor(Color.BLACK); g.drawRect(76, 42, 39, 18); g.drawString("Cliente", 86, 53);
            g.drawLine(94, 42, 94, 34); g.fillOval(91, 29, 6, 6);
            g.drawLine(104, 42, 109, 34); g.drawOval(106, 29, 6, 6);
            g.setColor(accent); g.drawRect(73, 39, 45, 24);
            g.fillRect(139, 22, 2, 11);
            g.setColor(fg); for (int y = 24; y < 77; y += 12) g.drawRect(144, y, 5, 5);
        } finally { g.dispose(); }
        // Icon is deliberately not Serializable: it is runtime presentation state.
        return new Icon() {
            public int getIconWidth() { return image.getWidth(); }
            public int getIconHeight() { return image.getHeight(); }
            public void paintIcon(Component c, Graphics g, int x, int y) { g.drawImage(image, x, y, null); }
        };
    }
}

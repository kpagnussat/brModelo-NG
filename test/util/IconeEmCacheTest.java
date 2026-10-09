package util;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.swing.Icon;
import javax.swing.UIManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class IconeEmCacheTest {
    private final javax.swing.LookAndFeel old = UIManager.getLookAndFeel();

    @AfterEach void restore() throws Exception { UIManager.setLookAndFeel(old); }

    @Test void fixedColorIconsAreCachedAndLookLikeTheVector() {
        FlatLightLaf.setup();
        Icon icon = Icones.de("/imagens/menu_salvar.png");
        assertInstanceOf(IconeEmCache.class, icon);
        FlatSVGIcon vector = new FlatSVGIcon("imagens/svg/menu_salvar.svg", 16, 16);
        for (double scale : new double[]{1, 2})
            assertTrue(maxDiff(paint(vector, scale), paint(icon, scale)) <= 8, "scale " + scale);
    }

    @Test void themeSwitchRepaintsInTheNewColors() {
        FlatLightLaf.setup();
        Icon icon = Icones.de("/imagens/menu_salvar.png");
        BufferedImage light = paint(icon, 1);
        FlatDarkLaf.setup();
        BufferedImage dark = paint(icon, 1);
        assertTrue(maxDiff(light, dark) > 40, "the cache must follow the theme");
        assertTrue(maxDiff(paint(new FlatSVGIcon("imagens/svg/menu_salvar.svg", 16, 16), 1), dark) <= 8);
    }

    @Test void stateDependentIconsStayVectorAndDisabledIconsWork() {
        FlatLightLaf.setup();
        assertInstanceOf(FlatSVGIcon.class, Icones.de("/imagens/trash-2.png"), "rollover-colored icon");
        Icon icon = Icones.de("/imagens/menu_salvar.png");
        Icon disabled = UIManager.getLookAndFeel().getDisabledIcon(new javax.swing.JButton(), icon);
        assertTrue(maxDiff(paint(icon, 1), paint(disabled, 1)) > 20, "disabled icon is greyed");
    }

    private static BufferedImage paint(Icon icon, double scale) {
        int w = (int) Math.ceil(icon.getIconWidth() * scale), h = (int) Math.ceil(icon.getIconHeight() * scale);
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.scale(scale, scale);
        icon.paintIcon(null, g, 0, 0);
        g.dispose();
        return image;
    }

    private static int maxDiff(BufferedImage a, BufferedImage b) {
        int d = 0;
        for (int y = 0; y < a.getHeight(); y++)
            for (int x = 0; x < a.getWidth(); x++)
                for (int s = 0; s < 32; s += 8)
                    d = Math.max(d, Math.abs(((a.getRGB(x, y) >> s) & 255) - ((b.getRGB(x, y) >> s) & 255)));
        return d;
    }
}

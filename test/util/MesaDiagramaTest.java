package util;

import com.formdev.flatlaf.FlatDarkLaf;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.swing.UIManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MesaDiagramaTest {
    private final javax.swing.LookAndFeel old = UIManager.getLookAndFeel();

    @AfterEach void restore() throws Exception { UIManager.setLookAndFeel(old); }

    /** Only a crisp outline around the page: no shadow pixels, antialiasing left as it was. */
    @Test void pageHasAnOutlineAndNoShadow() {
        FlatDarkLaf.setup();
        Rectangle page = new Rectangle(30, 30, 200, 120);
        BufferedImage image = new BufferedImage(260, 180, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        int desk = EstiloUI.mesa().getRGB(), line = EstiloUI.separador().getRGB();
        g.setColor(EstiloUI.mesa());
        g.fillRect(0, 0, 260, 180);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        MesaDiagrama.pintar(g, page);
        assertEquals(RenderingHints.VALUE_ANTIALIAS_ON, g.getRenderingHint(RenderingHints.KEY_ANTIALIASING));
        g.dispose();
        Rectangle outline = new Rectangle(page.x - 1, page.y - 1, page.width + 2, page.height + 2);
        for (int y = 0; y < 180; y++)
            for (int x = 0; x < 260; x++) {
                boolean border = outline.contains(x, y) && !new Rectangle(page).contains(x, y);
                if (border) assertEquals(line, image.getRGB(x, y), "outline at " + x + "," + y);
                else if (!page.contains(x, y)) assertEquals(desk, image.getRGB(x, y), "desk at " + x + "," + y);
            }
    }
}

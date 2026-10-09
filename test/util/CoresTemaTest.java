package util;

import java.awt.Color;
import javax.swing.UIManager;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class CoresTemaTest {
    private Object background;
    private Object foreground;
    @BeforeEach void save() { background = UIManager.put("Panel.background", null); foreground = UIManager.put("Label.foreground", null); }
    @AfterEach void restore() { UIManager.put("Panel.background", background); UIManager.put("Label.foreground", foreground); }
    private void dark() { UIManager.put("Panel.background", new Color(40, 40, 40)); UIManager.put("Label.foreground", new Color(220, 220, 220)); }

    // Guards pixel-identical original colors on light themes, including alpha.
    @Test void lightReturnsOriginals() {
        UIManager.put("Panel.background", Color.WHITE);
        assertFalse(CoresTema.escuro());
        for (Color c : new Color[]{Color.RED, Color.BLACK, Color.GRAY, new Color(80, 40, 130, 70)}) {
            assertSame(c, CoresTema.fundo(c)); assertSame(c, CoresTema.traco(c));
        }
    }

    // Guards hue preservation and improved visibility of saturated dark ink.
    @Test void darkInkPreservesHue() {
        dark(); assertTrue(CoresTema.escuro());
        for (Color c : new Color[]{Color.RED, Color.BLUE, new Color(80, 20, 120)}) {
            Color result = CoresTema.traco(c);
            assertEquals(Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null)[0],
                    Color.RGBtoHSB(result.getRed(), result.getGreen(), result.getBlue(), null)[0], 0.005);
            assertTrue(CoresTema.luminancia(result) > CoresTema.luminancia(c));
        }
    }

    // Guards gray ink blending between theme text and background, including both endpoints.
    @Test void darkGrayBlends() {
        dark();
        assertEquals(new Color(220, 220, 220), CoresTema.traco(Color.BLACK));
        assertEquals(new Color(40, 40, 40), CoresTema.traco(Color.WHITE));
        assertEquals(new Color(130, 130, 130), CoresTema.traco(new Color(128, 128, 128)));
    }

    // Guards background tint and alpha while translating white to the dark panel color.
    @Test void darkBackgroundKeepsTint() {
        dark();
        assertEquals(new Color(40, 40, 40), CoresTema.fundo(Color.WHITE));
        Color tint = CoresTema.fundo(new Color(210, 190, 230, 80));
        assertEquals(80, tint.getAlpha());
        assertTrue(tint.getBlue() > tint.getRed()); assertTrue(tint.getRed() > tint.getGreen());
    }
}

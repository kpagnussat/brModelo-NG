package util;

import java.awt.Color;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CatalogoCoresTest {
    @Test void everyThemeHasReadableCustomSurfacesAndCachedPreviewsWithoutInstallingIt() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            var oldLaf = UIManager.getLookAndFeel();
            var oldPanel = UIManager.getColor("Panel.background");
            for (var theme : CatalogoTemas.TODOS) {
                UIDefaults defaults = theme.fabrica().get().getDefaults();
                java.util.function.Function<String, Color> c = key -> PaletaTema.cor(defaults::getColor, key);
                Color bg = c.apply("Panel.background");
                assertTrue(PaletaTema.razao(c.apply("Label.foreground"), bg) >= 4.5, theme.id());
                assertTrue(PaletaTema.razao(c.apply("Label.disabledForeground"), bg) >= 3, theme.id());
                assertTrue(PaletaTema.razao(c.apply("Component.accentColor"), bg) >= 3, theme.id());
                assertTrue(PaletaTema.razao(c.apply("Actions.Red"), bg) >= 4.5, theme.id());
                assertTrue(c.apply("Actions.Red").getRed() >= c.apply("Actions.Red").getGreen(), theme.id());
                for (String prefix : new String[]{"Table.selection", "Table.selectionInactive", "List.selection"})
                    assertTrue(PaletaTema.razao(c.apply(prefix + "Foreground"), c.apply(prefix + "Background")) >= 4.5, theme.id() + prefix);
                for (String key : new String[]{"TextField.background", "Button.background", "Button.toolbar.hoverBackground",
                        "Button.toolbar.pressedBackground", "Component.borderColor", "Component.focusColor",
                        "Component.error.focusedBorderColor", "Actions.Red", "Separator.foreground"}) assertNotNull(c.apply(key), theme.id() + key);
                Icon icon = MiniaturaTema.obter(theme);
                assertSame(icon, MiniaturaTema.obter(theme));
                assertTrue(icon.getIconWidth() >= 152);
                assertSame(oldLaf, UIManager.getLookAndFeel(), "Previews must not install their LAF");
                assertEquals(oldPanel, UIManager.getColor("Panel.background"));
            }
        });
    }
    @Test void missingKeysStillProvideSemanticColorsAndContrast() {
        java.util.function.Function<String, Color> empty = key -> null;
        Color bg = PaletaTema.cor(empty, "Panel.background");
        assertTrue(PaletaTema.razao(PaletaTema.cor(empty, "Label.foreground"), bg) >= 4.5);
        assertTrue(PaletaTema.razao(PaletaTema.cor(empty, "Label.disabledForeground"), bg) >= 3);
        assertTrue(PaletaTema.razao(PaletaTema.cor(empty, "Component.accentColor"), bg) >= 3);
        assertEquals(bg, PaletaTema.cor(empty, "TextField.background"));
    }
}

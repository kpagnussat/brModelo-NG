package util;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import java.io.InputStream;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.*;

class IconesTest {
    static Stream<String> svgNames() throws Exception {
        try (Stream<Path> paths = Files.list(Path.of("src/imagens/svg"))) {
            return paths.map(p -> p.getFileName().toString()).filter(n -> n.endsWith(".svg")).sorted().toList().stream();
        }
    }

    // Guards parser validity and dimensions of every shipped SVG, including actual rasterization.
    @ParameterizedTest(name = "{0}") @MethodSource("svgNames")
    void everySvgParses(String name) {
        FlatSVGIcon icon = new FlatSVGIcon("imagens/svg/" + name);
        assertTrue(icon.hasFound(), name);
        assertTrue(icon.getIconWidth() > 0 && icon.getIconHeight() > 0, name);
        var image = new java.awt.image.BufferedImage(icon.getIconWidth(), icon.getIconHeight(), java.awt.image.BufferedImage.TYPE_INT_ARGB);
        var graphics = image.createGraphics();
        try { icon.paintIcon(null, graphics, 0, 0); } finally { graphics.dispose(); }
        assertTrue(icon.hasFound(), "Rendering must not invalidate " + name);
    }

    // Every image-valued resource key has a vector source, including paper/IR aliases.
    @Test void allInterfaceImageKeysHaveSvg() throws Exception {
        String generator = Files.readString(Path.of("dev/icones/gerar_svg.py"));
        assertFalse(generator.contains("PAINTED_IN_DIAGRAM"), "No bitmap-only paper exemptions remain");
        Properties properties = new Properties();
        try (InputStream in = getClass().getResourceAsStream("/principal/Propriedades_pt_BR.properties")) { properties.load(in); }
        List<String> missing = new ArrayList<>();
        for (String key : properties.stringPropertyNames()) {
            String value = properties.getProperty(key).trim();
            if (!value.toLowerCase(Locale.ROOT).matches(".*\\.(png|gif|jpg|jpeg)$")) continue;
            if (Icones.daChave(key, 16) == null) missing.add(key + "=" + value);
        }
        assertEquals(List.of(), missing, "Interface icons missing SVG");
    }

    // Guards resource name normalization, bitmap fallback and missing-resource behavior.
    @Test void resourceLookup() {
        assertNotNull(svg(Icones.de("/imagens/menu_salvar.png")), "SVG-backed");
        assertEquals(16, Icones.de("/imagens/menu_salvar.png").getIconWidth());
        assertNotNull(svg(Icones.de("/imagens/ir_pk.png")), "SVG-backed");
        assertInstanceOf(javax.swing.ImageIcon.class, Icones.de("/imagens/Desenho.png"));
        assertNull(Icones.de("/imagens/invented_missing_icon.png"));
        assertNull(Icones.daChave("invented.missing.key", 16));
        assertNull(Icones.noPapel("invented.missing.key", 16));
    }

    /** The SVG behind an interface icon, whether cached as a raster or painted from the vector. */
    private static FlatSVGIcon svg(javax.swing.Icon icon) {
        return icon instanceof IconeEmCache cached ? cached.vetor() : icon instanceof FlatSVGIcon svg ? svg : null;
    }

    @Test void constraintsSharePaletteSources() {
        assertEquals(svg(Icones.daChave("diagrama.Campo_Key.img", 16)).getName(),
                svg(Icones.daChave("diagrama.Constraint_PK.img", 16)).getName());
        assertEquals(svg(Icones.daChave("diagrama.Campo_KeyFkey.img", 16)).getName(),
                svg(Icones.daChave("diagrama.Constraint_FK.img", 16)).getName());
        assertArrayEquals(pixels("diagrama.Campo_Key.img", 4, false), pixels("diagrama.Constraint_PK.img", 4, false));
        assertArrayEquals(pixels("diagrama.Campo_KeyFkey.img", 4, false), pixels("diagrama.Constraint_FK.img", 4, false));
    }

    // Paper ink must ignore both theme and FlatSVGIcon's global recoloring, even at 8x.
    @Test void paperIsVectorAndIgnoresThemeFilter() throws Exception {
        var old = javax.swing.UIManager.getLookAndFeel();
        var filter = FlatSVGIcon.ColorFilter.getInstance();
        var mapper = filter.getMapper();
        try {
            for (String key : List.of("diagrama.Campo_Key.img", "diagrama.Campo_Fkey.img",
                    "diagrama.Campo_KeyFkey.img", "diagrama.Constraint_UN.img", "diagrama.Constraint_UNFK.img",
                    "diagrama.ancordor.0.img", "diagrama.ancordor.0.0.img", "diagrama.ancordor.1.img",
                    "diagrama.ancordor.2.img", "diagrama.ancordor.3.img", "diagrama.ancordor.5.img",
                    "diagrama.ancordor.6.img", "diagrama.ancordor.7.img", "diagrama.ancordor.8.img",
                    "diagrama.ancordor.9.img", "diagrama.ancordor.7.0.img", "diagrama.ancordor.8.0.img",
                    "diagrama.ancordor.9.0.img", "diagrama.Constraint_see.img")) {
                javax.swing.Icon icon = Icones.noPapel(key, 16);
                assertNotNull(icon, key);
                assertEquals(16, icon.getIconWidth(), key);
                for (int zoom : new int[]{1, 2, 4}) for (int screenScale : new int[]{1, 2}) {
                    int scale = zoom * screenScale;
                    com.formdev.flatlaf.FlatLightLaf.setup();
                    filter.setMapper(mapper);
                    int[] light = pixels(key, scale, false);
                    com.formdev.flatlaf.FlatDarkLaf.setup();
                    filter.setMapper(color -> java.awt.Color.MAGENTA);
                    assertArrayEquals(light, pixels(key, scale, false), key + " scale " + scale);
                    assertTrue(Arrays.stream(light).anyMatch(p -> p != 0), key);
                }
                // Vector geometry at 8x must differ from enlarging the 16px raster.
                var low = image(key, 1, false);
                var enlarged = new java.awt.image.BufferedImage(128, 128, java.awt.image.BufferedImage.TYPE_INT_ARGB);
                var g = enlarged.createGraphics();
                try { g.drawImage(low, 0, 0, 128, 128, null); } finally { g.dispose(); }
                assertFalse(Arrays.equals(enlarged.getRGB(0, 0, 128, 128, null, 0, 128), pixels(key, 8, false)), key);
            }
            int[] tinted = pixels("diagrama.Campo_KeyFkey.img", 4, true);
            assertTrue(Arrays.stream(tinted).anyMatch(p -> (p >>> 24) != 0));
            // Edge pixels round through premultiplied alpha; opaque ink keeps the exact color.
            assertTrue(Arrays.stream(tinted).anyMatch(p -> (p >>> 24) == 255));
            for (int pixel : tinted) if ((pixel >>> 24) == 255) assertEquals(0x123456, pixel & 0xffffff);
        } finally {
            filter.setMapper(mapper);
            javax.swing.UIManager.setLookAndFeel(old);
        }
    }

    private static java.awt.image.BufferedImage image(String key, int scale, boolean tint) {
        var im = new java.awt.image.BufferedImage(16 * scale, 16 * scale, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        var g = im.createGraphics();
        try {
            g.scale(scale, scale);
            if (tint) Icones.pinteNoPapel(key, g, 0, 0, 16, new java.awt.Color(0x123456));
            else Icones.noPapel(key, 16).paintIcon(null, g, 0, 0);
        } finally { g.dispose(); }
        return im;
    }

    private static int[] pixels(String key, int scale, boolean tint) {
        var im = image(key, scale, tint);
        return im.getRGB(0, 0, im.getWidth(), im.getHeight(), null, 0, im.getWidth());
    }
}

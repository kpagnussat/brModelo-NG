package brmodelo;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.*;

@Tag("gui")
class PrintPreviewTest {
    private static principal.FramePrincipal frame;

    @BeforeAll static void start() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            frame = new principal.FramePrincipal();
            principal.Aplicacao.fmPrincipal = frame;
        });
    }

    @AfterAll static void stop() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            for (java.awt.Window window : java.awt.Window.getWindows()) window.dispose();
            principal.Aplicacao.fmPrincipal = null;
        });
    }

    @ParameterizedTest @org.junit.jupiter.params.provider.ValueSource(strings = {"conceitual", "logico"})
    void externalOutputAndModelAreIdenticalAcrossThemes(String name) throws Exception {
        var diagram = Fixtures.load(name);
        var old = javax.swing.UIManager.getLookAndFeel();
        SwingUtilities.invokeAndWait(() -> {
            try {
                diagram.setMaster(frame.getEditor());
                // Loaded diagrams lazily cache text layout on their first paint.
                Graphics2D warmup = new BufferedImage(1400, 1050, BufferedImage.TYPE_INT_RGB).createGraphics();
                try { diagram.ExternalPaint(warmup); } finally { warmup.dispose(); }
                String before = CanonicalDump.dump(diagram);
                byte[] stored = serializedBytes(diagram);
                int[] light = null;
                for (boolean dark : new boolean[]{false, true}) {
                    if (dark) com.formdev.flatlaf.FlatDarkLaf.setup();
                    else com.formdev.flatlaf.FlatLightLaf.setup();
                    com.formdev.flatlaf.FlatLaf.updateUI();
                    BufferedImage output = new BufferedImage(1400, 1050, BufferedImage.TYPE_INT_RGB);
                    Graphics2D graphics = output.createGraphics();
                    graphics.setColor(java.awt.Color.WHITE);
                    graphics.fillRect(0, 0, output.getWidth(), output.getHeight());
                    try { diagram.ExternalPaint(graphics); }
                    finally { graphics.dispose(); }
                    int[] pixels = output.getRGB(0, 0, output.getWidth(), output.getHeight(), null, 0, output.getWidth());
                    if (light == null) light = pixels;
                    else assertArrayEquals(light, pixels, "Theme/desk/shadow must not enter external output");
                    assertEquals(before, CanonicalDump.dump(diagram));
                    if (name.equals("logico"))
                        assertArrayEquals(stored, serializedBytes(diagram), "Logical markers must preserve serialized bytes");
                }
            } catch (Exception e) { throw new RuntimeException(e); }
            finally {
                try { javax.swing.UIManager.setLookAndFeel(old); }
                catch (Exception e) { throw new RuntimeException(e); }
            }
        });
    }

    // Saving through GuardaPadraoBrM increments Diagrama._tick; inspect the stream directly.
    private static byte[] serializedBytes(controlador.Diagrama diagram) throws java.io.IOException {
        var bytes = new java.io.ByteArrayOutputStream();
        try (var stream = new java.io.ObjectOutputStream(bytes)) { stream.writeObject(diagram); }
        return bytes.toByteArray();
    }

    // Guards document text, geometry, flags and endpoints against scaled print-preview painting.
    @ParameterizedTest(name = "{0}")
    @MethodSource("brmodelo.Fixtures#names")
    void scaledPaintingDoesNotMutateModel(String name) throws Exception {
        var diagram = Fixtures.load(name);
        SwingUtilities.invokeAndWait(() -> {
            diagram.setMaster(frame.getEditor());
            try {
                String before = CanonicalDump.dump(diagram);
                for (double scale : new double[]{0.25, 0.5, 0.75}) {
                    Graphics2D graphics = new BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB).createGraphics();
                    try {
                        graphics.scale(scale, scale);
                        diagram.ExternalPaint(graphics);
                    } finally { graphics.dispose(); }
                    assertFalse(diagram.isPinturaExterna(), "Paint mode is restored");
                    assertEquals(before, CanonicalDump.dump(diagram), "Preview at scale " + scale);
                }
            } catch (Exception e) { throw new RuntimeException(e); }
        });
    }
}

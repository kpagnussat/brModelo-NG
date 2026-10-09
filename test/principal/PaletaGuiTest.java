package principal;

import com.formdev.flatlaf.*;
import com.formdev.flatlaf.util.UIScale;
import controlador.Diagrama.TipoDeDiagrama;
import controlador.Editor;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.swing.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

@Tag("gui")
class PaletaGuiTest {
    private FramePrincipal frame;
    private JPanel palette;
    private JScrollPane scroller;
    private final Map<AbstractButton, Dimension> icons = new IdentityHashMap<>();

    @Test void fitsNormalWindowsAndTracksResizeDiagramThemeFontAndScale() throws Exception {
        var oldLaf = UIManager.getLookAndFeel();
        String oldScale = System.getProperty("flatlaf.uiScale");
        Object oldFont = UIManager.get("defaultFont");
        var config = new HashMap<>(Editor.fromConfiguracao.getConfiguracao());
        try {
            SwingUtilities.invokeAndWait(() -> {
                System.setProperty("flatlaf.uiScale", "1x");
                FlatLightLaf.setup();
                frame = new FramePrincipal();
                frame.getEditor().setAutoSaveInterval(0);
                assertEquals(TipoDeDiagrama.tpConceitual, frame.getEditor().diagramaAtual.getTipo());
                palette = field("BarraDeBotoes", JPanel.class);
                scroller = field("ScrollerBarraDeBotoes", JScrollPane.class);
                for (Component component : palette.getComponents()) {
                    var button = (AbstractButton) component;
                    icons.put(button, new Dimension(button.getIcon().getIconWidth(), button.getIcon().getIconHeight()));
                }
                frame.setSize(1366, 768);
                frame.addNotify();
                frame.validate();
            });
            settle();
            for (boolean dark : new boolean[]{false, true}) {
                SwingUtilities.invokeAndWait(() -> {
                    if (dark) FlatDarkLaf.setup(); else FlatLightLaf.setup();
                    FlatLaf.updateUI();
                });
                settle();
                // Outer sizes: without a window manager (Xvfb in CI) the frame has no title bar, so the
                // palette gets ~38px more than on a desktop; 500 overflows either way, 600 only on a desktop.
                for (int height : new int[]{1050, 768, 700, 600, 500, 768, 1050}) {
                    SwingUtilities.invokeAndWait(() -> frame.setSize(1366, height));
                    settle();
                    SwingUtilities.invokeAndWait(() -> {
                        verify(18);
                        if (height == 768) assertFalse(scroller.getVerticalScrollBar().isVisible());
                        if (height == 500) assertTrue(scroller.getVerticalScrollBar().isVisible());
                        capture((dark ? "flatescuro" : "flatclaro") + "-1-1366x" + height);
                    });
                }
            }
            // GNOME-like maximized 1080p: reserve 30px for the desktop top bar.
            // The actual frame still contains its menu, toolbar, diagram tabs and status.
            for (String scale : new String[]{"1", "1.25"}) {
                SwingUtilities.invokeAndWait(() -> {
                    System.setProperty("flatlaf.uiScale", scale + "x");
                    FlatDarkLaf.setup();
                    FlatLaf.updateUI();
                    frame.setSize(1920, 1080 - 30);
                });
                settle();
                SwingUtilities.invokeAndWait(() -> {
                    assertEquals(Float.parseFloat(scale), UIScale.getUserScaleFactor());
                    verify(18);
                    assertFalse(scroller.getVerticalScrollBar().isVisible());
                    capture("flatescuro-" + scale + "-1920x1080-gnome");
                });
            }
            // Fewer tools must grow at the same height; returning to conceptual shrinks again.
            SwingUtilities.invokeAndWait(() -> {
                System.setProperty("flatlaf.uiScale", "1x");
                FlatLightLaf.setup();
                FlatLaf.updateUI();
                frame.setSize(1366, 700);
            });
            settle();
            for (TipoDeDiagrama type : TipoDeDiagrama.values()) {
                SwingUtilities.invokeAndWait(() -> {
                    var diagram = frame.getEditor().Novo(type);
                    frame.getEditor().getDiagramas().add(diagram);
                    frame.getEditor().setSelected(diagram);
                });
                settle();
                SwingUtilities.invokeAndWait(() -> {
                    verify(palette.getComponentCount());
                    if (type == TipoDeDiagrama.tpLogico) {
                        assertTrue(palette.getComponentCount() < 18);
                        assertEquals(UIScale.scale(40), palette.getComponent(0).getWidth());
                    }
                });
            }
            SwingUtilities.invokeAndWait(() -> frame.getEditor().setSelected(frame.getEditor().getDiagramas().get(0)));
            settle();
            SwingUtilities.invokeAndWait(() -> {
                verify(18);
                var button = (AbstractButton) palette.getComponent(0);
                assertTrue(button.isSelected());
                assertTrue(button.isRolloverEnabled());
                assertEquals("toolBarButton", button.getClientProperty("JButton.buttonType"));
                button.getModel().setRollover(true);
                capture("flatclaro-1-hover");
                button.getModel().setArmed(true);
                button.getModel().setPressed(true);
                capture("flatclaro-1-pressed");
                button.getModel().setArmed(false);
                button.getModel().setPressed(false);
            });
            // A larger default font changes UIScale even with no explicit scale option.
            SwingUtilities.invokeAndWait(() -> {
                System.clearProperty("flatlaf.uiScale");
                UIManager.put("defaultFont", new javax.swing.plaf.FontUIResource(Font.DIALOG, Font.PLAIN, 30));
                FlatLaf.updateUI();
            });
            settle();
            SwingUtilities.invokeAndWait(() -> {
                assertTrue(UIScale.getUserScaleFactor() > 1);
                verify(18);
                assertTrue(scroller.getVerticalScrollBar().isVisible());
            });
        } finally {
            SwingUtilities.invokeAndWait(() -> {
                if (frame != null) { frame.getEditor().EndAutoSave(); frame.dispose(); }
                Editor.fromConfiguracao.setConfiguracao(config);
                UIManager.put("defaultFont", oldFont);
                if (oldScale == null) System.clearProperty("flatlaf.uiScale");
                else System.setProperty("flatlaf.uiScale", oldScale);
                try { UIManager.setLookAndFeel(oldLaf); }
                catch (Exception exception) { throw new RuntimeException(exception); }
            });
        }
    }

    private void settle() throws Exception {
        // Each pass drains deferred viewport/divider events before validating the frame. A loaded
        // CI runner can need more passes after a font or scale change, so wait until the palette
        // geometry holds still for three passes in a row (six passes minimum, as before).
        String previous = null;
        int stable = 0;
        for (int pass = 0; pass < 60 && (pass < 6 || stable < 3); pass++) {
            String[] now = new String[1];
            SwingUtilities.invokeAndWait(() -> {
                frame.validate();
                now[0] = scroller.getSize() + " " + scroller.getViewport().getExtentSize() + " "
                        + palette.getSize() + " " + palette.getPreferredSize() + " "
                        + scroller.getVerticalScrollBar().isVisible();
            });
            stable = now[0].equals(previous) ? stable + 1 : 0;
            previous = now[0];
        }
    }

    private void verify(int count) {
        assertEquals(count, palette.getComponentCount());
        Insets in = palette.getInsets();
        int available = scroller.getViewport().getHeight() - in.top - in.bottom;
        int size = Math.max(UIScale.scale(28), Math.min(UIScale.scale(40), available / count));
        System.out.println("window=" + frame.getSize() + " scale=" + UIScale.getUserScaleFactor()
                + " viewport=" + scroller.getViewport().getExtentSize() + " tools=" + count
                + " size=" + palette.getComponent(0).getSize() + " scrollbar=" + scroller.getVerticalScrollBar().isVisible());
        for (Component child : palette.getComponents()) {
            assertEquals(new Dimension(size, size), child.getSize());
            assertNotNull(((AbstractButton) child).getToolTipText());
            var button = (AbstractButton) child;
            assertTrue(button.getIcon().getIconWidth() <= size);
            Dimension original = icons.get(button);
            if (original != null) {
                assertEquals(UIScale.scale(original.width), button.getIcon().getIconWidth());
                assertEquals(UIScale.scale(original.height), button.getIcon().getIconHeight());
            }
            Insets padding = button.getInsets();
            assertTrue(button.getIcon().getIconWidth() + padding.left + padding.right <= size);
        }
        boolean overflow = size * count > available;
        assertEquals(overflow, scroller.getVerticalScrollBar().isVisible());
        assertTrue(palette.getPreferredSize().width <= scroller.getViewport().getWidth());
        // Fixed-width column glued to the right edge: no divider can squeeze it while resizing.
        JTabbedPane column = field("jTabbedPane1", JTabbedPane.class);
        assertEquals(column.getPreferredSize().width, column.getWidth());
        assertTrue(column.getWidth() >= palette.getPreferredSize().width
                + (overflow ? scroller.getVerticalScrollBar().getPreferredSize().width : 0));
        assertEquals(column.getParent().getWidth(), column.getX() + column.getWidth());
        if (!overflow) {
            Component last = palette.getComponent(count - 1);
            assertTrue(last.getY() + last.getHeight() + in.bottom <= scroller.getViewport().getHeight());
            assertEquals(0, scroller.getViewport().getViewPosition().y);
        } else {
            scroller.getVerticalScrollBar().setValue(Integer.MAX_VALUE);
            Rectangle visible = scroller.getViewport().getViewRect();
            assertTrue(visible.contains(palette.getComponent(count - 1).getBounds()));
            scroller.getVerticalScrollBar().setValue(0);
        }
    }

    private <T> T field(String name, Class<T> type) {
        try {
            var field = FramePrincipal.class.getDeclaredField(name);
            field.setAccessible(true);
            return type.cast(field.get(frame));
        } catch (ReflectiveOperationException exception) { throw new RuntimeException(exception); }
    }

    private void capture(String name) {
        String output = System.getProperty("brmodelo.snap.output");
        if (output == null) return;
        try {
            Path dir = Path.of(output);
            Files.createDirectories(dir);
            paint(scroller, dir.resolve(name + "-palette.png"));
            paint(frame.getRootPane(), dir.resolve(name + "-window.png"));
        } catch (Exception exception) { throw new RuntimeException(exception); }
    }

    private static void paint(Component component, Path output) throws Exception {
        var image = new BufferedImage(component.getWidth(), component.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        component.printAll(graphics);
        graphics.dispose();
        ImageIO.write(image, "png", output.toFile());
    }
}

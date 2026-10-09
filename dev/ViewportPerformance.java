import controlador.Editor;
import desenho.formas.Forma;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Method;
import java.util.Arrays;
import javax.swing.SwingUtilities;

/** Companion benchmark: real viewport clipping and verified selection/movement. */
public final class ViewportPerformance {
    public static void main(String[] args) {
        try {
            Method laf = principal.Aplicacao.class.getDeclaredMethod("initLookAndFeel");
            laf.setAccessible(true);
            laf.invoke(null);
            SwingUtilities.invokeAndWait(() -> {
                principal.Aplicacao.fmPrincipal = new principal.FramePrincipal();
                principal.Aplicacao.fmPrincipal.setVisible(true);
                principal.Aplicacao.fmPrincipal.getEditor().setAutoSaveInterval(0);
            });
            System.out.println("input,viewport,paint_ms,select_ms,drag_ms");
            for (String path : args) SwingUtilities.invokeAndWait(() -> measure(path));
            System.exit(0);
        } catch (Throwable failure) {
            failure.printStackTrace();
            System.exit(1);
        }
    }

    private static void measure(String path) {
        try {
            Editor editor = principal.Aplicacao.fmPrincipal.getEditor();
            editor.FecharTudo();
            var diagram = SnapDialogs.open(new File(path));
            principal.Aplicacao.fmPrincipal.validate();
            var box = editor.getBox();
            Rectangle viewport = box.getVisibleRect();
            if (viewport.isEmpty()) throw new IllegalStateException("Empty viewport");
            BufferedImage image = new BufferedImage(viewport.width, viewport.height, BufferedImage.TYPE_INT_RGB);
            double[] paint = new double[100], select = new double[100], drag = new double[100];
            for (int i = -10; i < paint.length; i++) {
                var graphics = image.createGraphics();
                graphics.translate(-viewport.x, -viewport.y);
                graphics.setClip(viewport);
                long start = System.nanoTime();
                box.paint(graphics);
                if (i >= 0) paint[i] = (System.nanoTime() - start) / 1e6;
                graphics.dispose();
            }
            // InfoDiagrama is a hidden, unselectable Forma. Pick an actual selectable shape
            // and verify hit testing, selection and movement rather than timing empty clicks.
            var shape = diagram.getListaDeItens().stream().filter(item -> item instanceof Forma
                    && item.isSelecionavel() && item.isVisible() && !item.isAncorado()
                    && diagram.CaptureFromPoint(new Point(item.getLeft() + item.getWidth() / 2,
                            item.getTop() + item.getHeight() / 2)) == item).findFirst().orElse(null);
            if (shape == null) {
                Arrays.fill(select, Double.NaN);
                Arrays.fill(drag, Double.NaN);
            } else {
                for (int i = 0; i < select.length; i++) {
                    int x = (int) ((shape.getLeft() + shape.getWidth() / 2) * box.getZoom());
                    int y = (int) ((shape.getTop() + shape.getHeight() / 2) * box.getZoom());
                    diagram.ClearSelect(false);
                    long start = System.nanoTime();
                    mouse(box, MouseEvent.MOUSE_PRESSED, x, y);
                    mouse(box, MouseEvent.MOUSE_RELEASED, x, y);
                    select[i] = (System.nanoTime() - start) / 1e6;
                    if (!shape.isSelecionado()) throw new IllegalStateException("Selection failed: " + path);
                    int left = shape.getLeft(), top = shape.getTop();
                    start = System.nanoTime();
                    mouse(box, MouseEvent.MOUSE_PRESSED, x, y);
                    mouse(box, MouseEvent.MOUSE_DRAGGED, x + 40, y + 40);
                    if (shape.getLeft() == left && shape.getTop() == top)
                        throw new IllegalStateException("Drag failed: " + path);
                    mouse(box, MouseEvent.MOUSE_RELEASED, x + 40, y + 40);
                    drag[i] = (System.nanoTime() - start) / 1e6;
                    // Restore only the in-memory model through a second real gesture.
                    mouse(box, MouseEvent.MOUSE_PRESSED, x + 40, y + 40);
                    mouse(box, MouseEvent.MOUSE_DRAGGED, x, y);
                    mouse(box, MouseEvent.MOUSE_RELEASED, x, y);
                }
            }
            System.out.printf(java.util.Locale.ROOT, "%s,%dx%d,%.3f,%.3f,%.3f%n", path,
                    viewport.width, viewport.height, median(paint), median(select), median(drag));
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    private static double median(double[] values) { Arrays.sort(values); return values[values.length / 2]; }

    private static void mouse(java.awt.Component target, int id, int x, int y) {
        target.dispatchEvent(new MouseEvent(target, id, System.currentTimeMillis(),
                java.awt.event.InputEvent.BUTTON1_DOWN_MASK, x, y, 1, false,
                id == MouseEvent.MOUSE_DRAGGED ? 0 : 1));
    }
}

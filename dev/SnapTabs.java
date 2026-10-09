import controlador.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import javax.swing.*;

/** Isolated tab/decorations proof: theme, UIScale, output directory, native|modernas. */
public final class SnapTabs {
    private SnapTabs() {}

    private static void paint(Component component, File output) throws Exception {
        BufferedImage image = new BufferedImage(component.getWidth(), component.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        RepaintManager manager = RepaintManager.currentManager(component);
        boolean buffered = manager.isDoubleBufferingEnabled();
        manager.setDoubleBufferingEnabled(false);
        try { component.paint(graphics); }
        finally { manager.setDoubleBufferingEnabled(buffered); }
        graphics.dispose();
        ImageIO.write(image, "png", output);
    }

    public static void main(String[] args) throws Exception {
        System.setProperty("flatlaf.uiScale", args[1] + "x");
        System.setProperty("brmodelo.decoracoes", args[3]);
        System.setProperty("brmodelo.tema", args[0]);
        File output = new File(args[2]);
        output.mkdirs();
        float scale = Float.parseFloat(args[1]);
        SwingUtilities.invokeAndWait(() -> {
            util.TemaAplicacao.decoracoes();
            util.TemaAplicacao.iniciar();
            principal.FramePrincipal frame = new principal.FramePrincipal();
            principal.Aplicacao.fmPrincipal = frame;
            frame.getEditor().setAutoSaveInterval(0);
            frame.setSize(Math.round(1400 * scale), Math.round(950 * scale));
            frame.setLocation(100, 100);
            frame.addNotify();
            frame.validate();
        });
        // Drain layout finishing and LAF defaults before opening fixtures.
        SwingUtilities.invokeAndWait(() -> {});
        SwingUtilities.invokeAndWait(() -> {
            principal.FramePrincipal frame = principal.Aplicacao.fmPrincipal;
            Editor editor = frame.getEditor();
            try {
                SnapDialogs.open(new File("test-resources/fixtures/conceitual.brM3"));
                frame.validate();
                Mostrador host = editor.getShowDiagramas();
                JTabbedPane tabs = (JTabbedPane) host.getComponent(0);
                String prefix = args[0] + "-" + args[1] + "-" + args[3];
                paint(host, new File(output, prefix + "-one.png"));
                Rectangle first = tabs.getBoundsAt(0);
                tabs.dispatchEvent(new MouseEvent(tabs, MouseEvent.MOUSE_MOVED, System.currentTimeMillis(),
                        0, first.x + first.width / 2, first.y + first.height / 2, 0, false));
                paint(host, new File(output, prefix + "-hover.png"));
                tabs.dispatchEvent(new MouseEvent(tabs, MouseEvent.MOUSE_EXITED, System.currentTimeMillis(),
                        0, -1, -1, 0, false));
                paint(frame.getRootPane(), new File(output, prefix + "-window.png"));
                for (int i = 0; i < 12; i++) {
                    var diagram = new diagramas.conceitual.DiagramaConceitual(editor);
                    diagram.setNome("Modelo de exemplo " + (i + 1));
                    diagram.setArquivo(new File(output, "modelo-" + i + ".brM3").getAbsolutePath());
                    editor.getDiagramas().add(diagram);
                }
                host.Reset(12);
                frame.validate();
                paint(host, new File(output, prefix + "-overflow.png"));
                var field = principal.FramePrincipal.class.getDeclaredField("TabInspector");
                field.setAccessible(true);
                JTabbedPane inspector = (JTabbedPane) field.get(frame);
                System.out.println(prefix + ": scale=" + com.formdev.flatlaf.util.UIScale.getUserScaleFactor()
                        + " tab row=" + host.getHeight() + " inspector tab=" + inspector.getBoundsAt(0)
                        + " diagram tab=" + tabs.getBoundsAt(0) + " undecorated=" + frame.isUndecorated()
                        + " alpha=" + frame.getBackground().getAlpha() + " root UI=" + frame.getRootPane().getUI());
                System.out.println("window=" + frame.getBounds() + " screen=" + frame.getGraphicsConfiguration().getBounds()
                        + " glass=" + frame.getGlassPane().getBounds() + " visible=" + frame.getGlassPane().isVisible()
                        + " glassUI=" + ((JComponent) frame.getGlassPane()).getUI());
                principal.FrameSobre dialog = new principal.FrameSobre(frame, false);
                dialog.setLocation(150, 150);
                dialog.addNotify();
                dialog.validate();
                paint(dialog.getRootPane(), new File(output, prefix + "-dialog.png"));
                dialog.dispose();
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            } finally {
                editor.EndAutoSave();
                frame.dispose();
            }
        });
        System.exit(0);
    }
}

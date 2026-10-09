import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;

/**
 * Dev-only screenshot harness (not part of the app): builds the main window with a given
 * look-and-feel and font scale, renders it off-screen to a PNG and exits.
 *
 * The window is realized (addNotify) but never mapped, so nothing flashes on the desktop.
 * Font scaling simulates "the theme's font is bigger than the layout was designed for",
 * which is exactly what GTK at high DPI does to brModelo.
 *
 * Usage: ./gradlew snap -Pargs="<metal|gtk|system> <fontScale> <out.png> [w h [interact]]"
 */
public class Snap {

    static controlador.inspector.Inspector maiorInspector(java.awt.Container c) {
        controlador.inspector.Inspector best = null;
        for (java.awt.Component k : c.getComponents()) {
            controlador.inspector.Inspector cand = null;
            if (k instanceof controlador.inspector.Inspector) {
                cand = (controlador.inspector.Inspector) k;
            } else if (k instanceof java.awt.Container) {
                cand = maiorInspector((java.awt.Container) k);
            }
            if (cand != null && (best == null || cand.getItens().size() > best.getItens().size())) {
                best = cand;
            }
        }
        return best;
    }

    static void press(java.awt.Component c, int x, int y) {
        long t = System.currentTimeMillis();
        c.dispatchEvent(new java.awt.event.MouseEvent(c, java.awt.event.MouseEvent.MOUSE_PRESSED, t, 0, x, y, 1, false, java.awt.event.MouseEvent.BUTTON1));
        c.dispatchEvent(new java.awt.event.MouseEvent(c, java.awt.event.MouseEvent.MOUSE_RELEASED, t, 0, x, y, 1, false, java.awt.event.MouseEvent.BUTTON1));
    }

    static void snap(String path) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JFrame f = principal.Aplicacao.fmPrincipal;
            f.validate();
            BufferedImage img = new BufferedImage(f.getWidth(), f.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            f.getRootPane().paint(g);
            g.dispose();
            try {
                ImageIO.write(img, "png", new File(path));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        System.out.println("wrote " + path);
    }

    public static void main(String[] a) throws Exception {
        String laf = a[0];
        float scale = Float.parseFloat(a[1]);
        File out = new File(a[2]);
        int w = a.length > 3 ? Integer.parseInt(a[3]) : 1200;
        int h = a.length > 4 ? Integer.parseInt(a[4]) : 800;

        SwingUtilities.invokeAndWait(() -> {
            try {
                String cls = laf.equals("metal") ? "javax.swing.plaf.metal.MetalLookAndFeel"
                        : laf.equals("gtk") ? "com.sun.java.swing.plaf.gtk.GTKLookAndFeel"
                        : UIManager.getSystemLookAndFeelClassName();
                UIManager.setLookAndFeel(cls);
                if (scale != 1f) {
                    // Metal keeps its fonts in the defaults table; scaling them mimics a larger theme font.
                    UIDefaults d = UIManager.getLookAndFeelDefaults();
                    for (Object k : d.keySet().toArray()) {
                        Object v = d.get(k);
                        if (v instanceof Font) {
                            Font f = (Font) v;
                            d.put(k, new FontUIResource(f.deriveFont(f.getSize2D() * scale)));
                        }
                    }
                }
                principal.FramePrincipal f = new principal.FramePrincipal();
                principal.Aplicacao.fmPrincipal = f;
                f.setSize(w, h);
                f.addNotify();
                f.validate();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        // Let deferred invokeLater work from the frame's constructor settle.
        Thread.sleep(1500);

        SwingUtilities.invokeAndWait(() -> {
            JFrame f = principal.Aplicacao.fmPrincipal;
            f.validate();
            BufferedImage img = new BufferedImage(f.getWidth(), f.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            f.getRootPane().paint(g);
            g.dispose();
            try {
                ImageIO.write(img, "png", out);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        System.out.println("wrote " + out);

        // Optional interaction pass: collapse a group and select a row, snapshot each step.
        if (a.length > 5 && a[5].equals("interact")) {
            String base = out.getPath().replaceAll("\\.png$", "");
            SwingUtilities.invokeAndWait(() -> {
                controlador.inspector.Inspector ins = maiorInspector(principal.Aplicacao.fmPrincipal);
                for (controlador.inspector.InspectorItemBase it : ins.getItens()) {
                    if (it instanceof controlador.inspector.InspectorItemSeparador && it.getTexto().startsWith("Dimens")) {
                        press(it, 8, it.getHeight() / 2);   // the +/- box at the row's left edge
                        break;
                    }
                }
            });
            snap(base + "_collapse.png");
            SwingUtilities.invokeAndWait(() -> {
                controlador.inspector.Inspector ins = maiorInspector(principal.Aplicacao.fmPrincipal);
                controlador.inspector.InspectorItemBase nome = null;
                for (controlador.inspector.InspectorItemBase it : ins.getItens()) {
                    if (it.getTexto().equals("Nome")) { nome = it; break; }
                }
                press(nome, nome.getWidth() * 3 / 4, nome.getHeight() / 2);
            });
            Thread.sleep(500);
            snap(base + "_select.png");
            SwingUtilities.invokeAndWait(() -> {
                controlador.inspector.Inspector ins = maiorInspector(principal.Aplicacao.fmPrincipal);
                int visiveis = 0;
                for (controlador.inspector.InspectorItemBase it : ins.getItens()) {
                    if (it.isVisible() && it.getParent() != null) visiveis++;
                    javax.swing.JComponent ed = it.getOndeEditar();
                    if (ed != null && ed.isVisible() && ed.getParent() == it) {
                        System.out.println("editor in '" + it.getTexto() + "': row=" + it.getBounds() + " editor=" + ed.getBounds());
                    }
                }
                System.out.println("items=" + ins.getItens().size() + " laid-out=" + visiveis);
            });
        }
        System.exit(0);
    }
}

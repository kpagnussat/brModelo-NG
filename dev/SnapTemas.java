import controlador.Editor;
import controlador.editores.EditorDeAtributos;
import diagramas.conceitual.DiagramaConceitual;
import diagramas.conceitual.Entidade;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import javax.swing.*;
import util.*;

/** Isolated live-theme gallery: one main-window + attributes-editor image per theme/scale. */
public final class SnapTemas {
    private static principal.FramePrincipal frame;
    private static EditorDeAtributos attributes;
    private SnapTemas() {}
    public static void main(String[] args) throws Exception {
        float scale = Float.parseFloat(args[0]);
        File output = new File(args[1]); output.mkdirs();
        System.setProperty("flatlaf.uiScale", args[0] + "x");
        System.setProperty("brmodelo.tema", "claro");
        System.setProperty("brmodelo.decoracoes", "modernas");
        // Never animate off-screen captures; the actual picker keeps animation enabled.
        System.setProperty("flatlaf.animatedLafChange", "false");
        SwingUtilities.invokeAndWait(() -> {
            TemaAplicacao.decoracoes(); TemaAplicacao.iniciar();
            frame = new principal.FramePrincipal(); principal.Aplicacao.fmPrincipal = frame;
            frame.getEditor().setAutoSaveInterval(0);
            frame.setSize(Math.round(1400 * scale), Math.round(950 * scale));
            frame.addNotify(); frame.validate();
        });
        settle();
        SwingUtilities.invokeAndWait(() -> {
            try {
                DiagramaConceitual diagram = (DiagramaConceitual) SnapDialogs.open(new File("test-resources/fixtures/conceitual.brM3"));
                for (var item : diagram.getListaDeItens()) if (item instanceof Entidade) {
                    diagram.setSelecionado(item); frame.getEditor().PerformInspectorFor(item); break;
                }
                for (var row : frame.getEditor().getInspectorEditor().getItens())
                    if ("Nome".equals(row.getPropriedade().caption)) {
                        frame.getEditor().getInspectorEditor().PerformSelect(row); break;
                    }
                attributes = new EditorDeAtributos(frame, true); attributes.Inicie(diagram);
                attributes.addNotify(); attributes.setSize(attributes.getPreferredSize()); attributes.validate();
                BrLogger.Clean();
                BrLogger.Logger("ERROR_DIAGRAMA_LOAD_FILE_BRM", "Exemplo de indicador de log");
                MensagensStatus.mostrar(frame.getEditor().getLblStatus(), "Exemplo de mensagem de erro", true);
            } catch (Exception e) { throw new RuntimeException(e); }
        });
        for (var tema : CatalogoTemas.TODOS) {
            SwingUtilities.invokeAndWait(() -> TemaAplicacao.aplicar(tema.id()));
            settle();
            SwingUtilities.invokeAndWait(() -> {
                try {
                    MensagensStatus.mostrar(frame.getEditor().getLblStatus(), "Exemplo de mensagem de erro", true);
                    BufferedImage main = pintar(frame.getRootPane()), editor = pintar(attributes.getRootPane());
                    int gap = Math.round(20 * scale), header = Math.round(40 * scale);
                    BufferedImage combined = new BufferedImage(main.getWidth() + editor.getWidth() + gap,
                            Math.max(main.getHeight(), editor.getHeight()) + header, BufferedImage.TYPE_INT_RGB);
                    Graphics2D g = combined.createGraphics();
                    g.setColor(EstiloUI.cor("Panel.background")); g.fillRect(0, 0, combined.getWidth(), combined.getHeight());
                    g.setColor(EstiloUI.cor("Label.foreground")); g.setFont(new Font(Font.DIALOG, Font.BOLD, Math.round(16 * scale)));
                    g.drawString(tema.nome() + " · escala " + args[0] + " · troca ao vivo", gap, Math.round(26 * scale));
                    g.drawImage(main, 0, header, null); g.drawImage(editor, main.getWidth() + gap, header, null); g.dispose();
                    ImageIO.write(combined, "png", new File(output, tema.id() + "-" + args[0] + ".png"));
                    System.out.println(tema.id() + ": " + combined.getWidth() + "x" + combined.getHeight()
                            + " panel=" + EstiloUI.cor("Panel.background") + " accent=" + EstiloUI.cor("Component.accentColor"));
                } catch (Exception e) { throw new RuntimeException(e); }
            });
        }
        for (String theme : new String[]{"claro", "escuro"}) {
            SwingUtilities.invokeAndWait(() -> TemaAplicacao.aplicar(theme.equals("claro") ? "auto" : theme)); settle();
            JDialog[] dialog = new JDialog[1];
            SwingUtilities.invokeAndWait(() -> {
                dialog[0] = Aparencia.criar(null); dialog[0].addNotify(); dialog[0].validate();
            });
            settle();
            SwingUtilities.invokeAndWait(() -> {
                try {
                    if (theme.equals("escuro")) rolar(dialog[0].getContentPane());
                    ImageIO.write(pintar(dialog[0].getRootPane()), "png", new File(output, "aparencia-" + theme + "-" + args[0] + ".png"));
                } catch (Exception e) { throw new RuntimeException(e); }
                finally { dialog[0].dispose(); }
            });
        }
        SwingUtilities.invokeAndWait(() -> { frame.getEditor().EndAutoSave(); for (Window w : Window.getWindows()) w.dispose(); });
        System.exit(0);
    }
    private static void rolar(Container container) {
        for (Component c : container.getComponents()) {
            if (c instanceof JScrollPane pane) pane.getVerticalScrollBar().setValue(Integer.MAX_VALUE);
            else if (c instanceof Container nested) rolar(nested);
        }
    }
    private static void settle() throws Exception {
        for (int i = 0; i < 8; i++) SwingUtilities.invokeAndWait(() -> { frame.validate(); if (attributes != null) attributes.validate(); });
    }
    private static BufferedImage pintar(Component component) {
        BufferedImage image = new BufferedImage(component.getWidth(), component.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        RepaintManager manager = RepaintManager.currentManager(component);
        boolean old = manager.isDoubleBufferingEnabled(); manager.setDoubleBufferingEnabled(false);
        try { component.paint(g); } finally { manager.setDoubleBufferingEnabled(old); g.dispose(); }
        return image;
    }
}

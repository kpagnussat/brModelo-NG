import controlador.inspector.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.plaf.FontUIResource;

/** Deterministic property-type/state atlas and selected fixture screenshots. */
public final class SnapInspector {
    private static principal.FramePrincipal main;
    private static JFrame host;
    private static Inspector inspector;
    private static InspectorItemBase row;
    private static final String[] STATES = {"normal", "hover", "selected (inactive)", "editing / action (focused)"};
    private static final java.util.List<InspectorProperty.TipoDeProperty> TYPES = Arrays.stream(InspectorProperty.TipoDeProperty.values())
            .filter(t -> t != InspectorProperty.TipoDeProperty.tpNothing).toList();

    static InspectorProperty property(InspectorProperty.TipoDeProperty type) {
        InspectorProperty p = new InspectorProperty();
        p.tipo = type;
        p.caption = switch (type) {
            case tpSeparador -> "Seção";
            case tpTextoNormal -> "Nome";
            case tpNumero -> "Tamanho";
            case tpTextoLongo -> "Descrição";
            case tpApenasLeituraTexto -> "Arquivo";
            case tpApenasLeituraCor -> "Cor herdada";
            case tpBooleano -> "Visível";
            case tpMenu -> "Estilo";
            case tpCor -> "Cor";
            case tpSelecObject -> "Destino";
            case tpCommand -> "Converter";
            default -> type.name();
        };
        p.property = "dev." + type.name();
        p.valor_string = switch (type) {
            case tpNumero -> "42";
            case tpBooleano -> "true";
            case tpMenu -> "0";
            case tpCor, tpApenasLeituraCor -> util.Utilidades.ColorToString(new Color(80, 150, 210));
            case tpCommand -> "...";
            case tpTextoLongo -> "Texto longo\nSegunda linha";
            case tpApenasLeituraTexto -> "biblioteca.brM3";
            default -> "Biblioteca Aurora";
        };
        p.opcoesMenu = java.util.List.of("Normal", "Tracejado", "Pontilhado");
        p.dica = "Descrição da propriedade de exemplo.";
        return p;
    }
    static void setup(String theme, float scale) throws Exception {
        if (theme.equals("flatclaro")) com.formdev.flatlaf.FlatLightLaf.setup();
        else com.formdev.flatlaf.FlatDarkLaf.setup();
        util.EstiloUI.iniciar();
        UIDefaults d = UIManager.getLookAndFeelDefaults();
        Map<Object, Font> fonts = new LinkedHashMap<>();
        for (Object key : d.keySet().toArray()) if (d.get(key) instanceof Font f) fonts.put(key, f);
        for (var entry : fonts.entrySet()) d.put(entry.getKey(), new FontUIResource(entry.getValue().deriveFont(entry.getValue().getSize2D() * scale)));
        main = new principal.FramePrincipal();
        principal.Aplicacao.fmPrincipal = main;
        main.getEditor().setAutoSaveInterval(0);
        main.setSize(Math.round(1400 * scale), Math.round(1050 * scale));
        main.addNotify();
        main.validate();
        host = new JFrame("Inspector dev renderer");
        host.setSize(Math.round(430 * scale), Math.round(200 * scale));
        host.setLocation(0, 0);
        host.setVisible(true);
    }
    static void prepare(InspectorProperty.TipoDeProperty type, int state) {
        inspector = new Inspector();
        inspector.setEditor(main.getEditor());
        inspector.setDicas(new InspectorDicas());
        row = inspector.Add(property(type));
        host.setContentPane(inspector);
        host.validate();
        if (state >= 2) {
            inspector.PerformSelect(row);
            if (state == 2) {
                if (row.getOndeEditar() != null) row.remove(row.getOndeEditar());
                host.getRootPane().requestFocusInWindow();
            } else {
                if (row.getOndeEditar() != null && row.getOndeEditar().getParent() == row) row.getOndeEditar().requestFocusInWindow();
                else row.requestFocusInWindow();
                if (type == InspectorProperty.TipoDeProperty.tpCommand) row.putClientProperty("brmodelo.inspector.pressed", true);
                if (type == InspectorProperty.TipoDeProperty.tpBooleano) row.setValor("false");
            }
        }
        if (state == 1) {
            row.dispatchEvent(new MouseEvent(row, MouseEvent.MOUSE_ENTERED, 0, 0, row.getWidth() - 12, row.getHeight() / 2, 0, false));
            row.dispatchEvent(new MouseEvent(row, MouseEvent.MOUSE_MOVED, 0, 0, row.getWidth() - 12, row.getHeight() / 2, 0, false));
        }
    }
    static BufferedImage paint(Component c) {
        BufferedImage image = new BufferedImage(c.getWidth(), c.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        c.paint(g);
        g.dispose();
        return image;
    }
    static void backgrounds(File dir, String prefix, float scale) throws Exception {
        InspectorDicas hints = new InspectorDicas();
        inspector = new Inspector();
        inspector.setEditor(main.getEditor());
        inspector.setDicas(hints);
        inspector.Add(property(InspectorProperty.TipoDeProperty.tpTextoNormal));
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(inspector);
        hints.setPreferredSize(new Dimension(0, Math.round(90 * scale)));
        panel.add(hints, BorderLayout.SOUTH);
        host.setContentPane(panel);
        host.setSize(Math.round(430 * scale), Math.round(300 * scale));
        host.validate();
        BufferedImage img = paint(panel);
        ImageIO.write(img, "png", new File(dir, prefix + "_backgrounds.png"));
        InspectorItemBase r = inspector.getItens().get(0);
        Point start = SwingUtilities.convertPoint(r, 0, 0, panel);
        int d = (int)(r.getWidth() * inspector.getDivisor());
        Map<String, Point> samples = new LinkedHashMap<>();
        samples.put("name", new Point(start.x + 3, start.y + r.getHeight()/2));
        samples.put("value", new Point(start.x + d + 3, start.y + r.getHeight()/2));
        samples.put("below", SwingUtilities.convertPoint(inspector.getViewport(), 20, inspector.getViewport().getHeight() - 15, panel));
        samples.put("gutter", SwingUtilities.convertPoint(inspector.getVerticalScrollBar(), 1, inspector.getVerticalScrollBar().getHeight()/2, panel));
        samples.put("hints", SwingUtilities.convertPoint(hints, 2, hints.getHeight() - 3, panel));
        for (var sample : samples.entrySet()) {
            Point p = sample.getValue();
            Color c = new Color(img.getRGB(p.x, p.y));
            System.out.printf("PIXEL %s %s (%d,%d) RGB=%d,%d,%d%n", prefix, sample.getKey(), p.x, p.y, c.getRed(), c.getGreen(), c.getBlue());
            if (!c.equals(UIManager.getColor("Panel.background"))) throw new AssertionError("Background mismatch: " + sample.getKey());
        }
    }
    static void dialog(File dir, String prefix, String name, Runnable open) {
        javax.swing.Timer timer = new javax.swing.Timer(150, e -> {
            for (Window window : Window.getWindows()) if (window instanceof JDialog && window.isShowing()) {
                try { SnapDialogs.render(window, new File(dir, prefix + "_editing_" + name + ".png")); }
                catch (Exception failure) { throw new RuntimeException(failure); }
                window.dispose();
            }
        });
        timer.setRepeats(false);
        timer.start();
        open.run();
    }
    public static void main(String[] args) throws Exception {
        String theme = args[0];
        float scale = Float.parseFloat(args[1]);
        File dir = new File(args[2]);
        dir.mkdirs();
        String prefix = theme + "_" + args[1];
        SwingUtilities.invokeAndWait(() -> { try { setup(theme, scale); } catch (Exception e) { throw new RuntimeException(e); } });
        int labelWidth = Math.round(210 * scale), cellWidth = Math.round(420 * scale), step = Math.round(42 * scale);
        BufferedImage atlas = new BufferedImage(labelWidth + 4 * cellWidth, (TYPES.size() + 2) * step, BufferedImage.TYPE_INT_RGB);
        Graphics2D ag = atlas.createGraphics();
        ag.setColor(UIManager.getColor("Panel.background"));
        ag.fillRect(0, 0, atlas.getWidth(), atlas.getHeight());
        ag.setFont(UIManager.getFont("Label.font"));
        ag.setColor(UIManager.getColor("Label.foreground"));
        for (int state = 0; state < 4; state++) ag.drawString(STATES[state], labelWidth + state * cellWidth + 8, step - 12);
        for (int i = 0; i < TYPES.size(); i++) {
            var type = TYPES.get(i);
            ag.setColor(UIManager.getColor("Label.foreground"));
            ag.drawString(type.name(), 8, (i + 2) * step - 12);
            for (int state = 0; state < 4; state++) {
                final int s = state;
                SwingUtilities.invokeAndWait(() -> prepare(type, s));
                // Allow real focus transitions to settle before painting, as with live theme switching.
                Thread.sleep(30);
                final BufferedImage[] cell = new BufferedImage[1];
                SwingUtilities.invokeAndWait(() -> cell[0] = paint(row));
                ag.drawImage(cell[0], labelWidth + state * cellWidth, (i + 1) * step, null);
            }
        }
        ag.setColor(UIManager.getColor("Label.disabledForeground"));
        ag.drawString("tpNothing: sem linha. Somente leitura: sem editor. Ações e diálogos: estado de ativação, sem editor de texto embutido.", 8, atlas.getHeight() - 10);
        ag.dispose();
        ImageIO.write(atlas, "png", new File(dir, prefix + "_states.png"));
        SwingUtilities.invokeAndWait(() -> {
            try {
                backgrounds(dir, prefix, scale);
                var dc = (diagramas.conceitual.DiagramaConceitual)SnapDialogs.open(new File(args[3]));
                prepare(InspectorProperty.TipoDeProperty.tpMenu, 3);
                inspector.PerformSelect(row);
                ((JComboBox<?>)row.getOndeEditar()).showPopup();
                JComponent popup = (JComponent)((JComboBox<?>)row.getOndeEditar()).getUI().getAccessibleChild(row.getOndeEditar(), 0);
                popup.validate();
                BufferedImage menu = paint(host.getRootPane());
                Graphics2D mg = menu.createGraphics();
                Point mp = SwingUtilities.convertPoint(row.getOndeEditar(), 0, row.getHeight(), host.getRootPane());
                mg.drawImage(paint(popup), mp.x, mp.y, null);
                mg.dispose();
                ImageIO.write(menu, "png", new File(dir, prefix + "_editing_menu.png"));
                ((JComboBox<?>)row.getOndeEditar()).hidePopup();
                prepare(InspectorProperty.TipoDeProperty.tpTextoLongo, 3);
                dialog(dir, prefix, "long_text", () -> ((InspectorItemExtender)row).ExternalRun());
                prepare(InspectorProperty.TipoDeProperty.tpCor, 3);
                dialog(dir, prefix, "color", () -> ((InspectorItemExtender)row).ExternalRun());
                main.validate();
                SnapDialogs.render(main, new File(dir, prefix + "_main_diagram.png"));
                for (var item : dc.getListaDeItens()) if (item instanceof diagramas.conceitual.Entidade) {
                    dc.setSelecionado(item);
                    main.getEditor().PerformInspectorFor(item);
                    SnapDialogs.render(main, new File(dir, prefix + "_main_entity.png"));
                    break;
                }
                for (var item : dc.getListaDeItens()) if (item instanceof diagramas.conceitual.Relacionamento) {
                    dc.setSelecionado(item);
                    main.getEditor().PerformInspectorFor(item);
                    SnapDialogs.render(main, new File(dir, prefix + "_main_relationship.png"));
                    break;
                }
                var dl = (diagramas.logico.DiagramaLogico)SnapDialogs.open(new File(args[4]));
                var table = dl.getListaDeTabelas().get(0);
                table.setCampoSelecionado(null);
                table.setConstraintSelecionado(null);
                dl.setSelecionado(table);
                main.getEditor().PerformInspectorFor(table);
                SnapDialogs.render(main, new File(dir, prefix + "_main_table.png"));
                host.dispose();
                main.dispose();
            } catch (Exception e) { throw new RuntimeException(e); }
        });
        System.exit(0);
    }
}

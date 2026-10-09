package brmodelo;

import controlador.Diagrama;
import controlador.Editor;
import desenho.Ancorador;
import desenho.PosicionamentoAncorador;
import desenho.formas.Forma;
import desenho.linhas.PontoDeLinha;
import desenho.linhas.Linha;
import diagramas.conceitual.Atributo;
import diagramas.conceitual.Entidade;
import diagramas.conceitual.Ligacao;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

@Tag("gui")
class AncoradorGuiTest {
    private principal.FramePrincipal frame;
    private Editor editor;
    private Diagrama diagram;
    private javax.swing.LookAndFeel previousLaf;

    @BeforeEach void start() throws Exception {
        previousLaf = UIManager.getLookAndFeel();
        onEdt(() -> {
            frame = new principal.FramePrincipal();
            editor = frame.getEditor();
            editor.setAutoSaveInterval(0);
            editor.setAncorador(true);
            frame.setSize(1300, 900);
            frame.addNotify();
            frame.validate();
            diagram = editor.AddAsAtual("tpConceitual");
        });
    }

    @AfterEach void stop() throws Exception {
        onEdt(() -> {
            if (editor != null) editor.EndAutoSave();
            if (frame != null) frame.dispose();
            UIManager.setLookAndFeel(previousLaf);
        });
    }

    @Test void gallery() throws Exception {
        Assumptions.assumeTrue(System.getProperty("brmodelo.snap.output") != null);
        for (String theme : new String[]{"flatclaro", "flatescuro"}) {
            onEdt(() -> {
                if (theme.equals("flatclaro")) com.formdev.flatlaf.FlatLightLaf.setup();
                else com.formdev.flatlaf.FlatDarkLaf.setup();
                SwingUtilities.updateComponentTreeUI(frame);
                diagram = editor.AddAsAtual("tpConceitual");
                leftAttribute(false);
                capture(theme + "-attribute-left.png");
                diagram = editor.AddAsAtual("tpConceitual");
                leftAttribute(true);
                capture(theme + "-attribute-corner.png");
                diagram = editor.AddAsAtual("tpConceitual");
                fourSides();
                capture(theme + "-four-sides.png");
            });
        }
    }

    @Test void leftLinkedAttributeAndEntityCornerStayClear() throws Exception {
        onEdt(() -> {
            for (boolean corner : new boolean[]{false, true}) {
                diagram = editor.AddAsAtual("tpConceitual");
                Atributo attribute = leftAttribute(corner);
                paintDiagram();
                Rectangle bounds = strip().getBounds();
                assertEquals(attribute.getLeftWidth() + 8, bounds.x, "Free right side wins");
                assertClearOfEndpoints(bounds);
                for (var item : diagram.getListaDeItens()) {
                    if (item instanceof Forma && item != attribute) {
                        assertFalse(bounds.intersects(item.getBounds()), "Avoid the entity's corner");
                    }
                }
                assertClearOfLines(attribute, bounds);
            }
        });
    }

    @Test void allFourSidesUseOnlyTheClearanceNeededBeyondHandles() throws Exception {
        onEdt(() -> {
            Entidade entity = fourSides();
            paintDiagram();
            assertClearOfEndpoints(strip().getBounds());
            assertEquals(21, entity.getLeft() - strip().getLeftWidth(), "Adjacent, just past the expanded hit area");
        });
    }

    @Test void horizontalIconsKeepTheirAnchorAndDeleteActions() throws Exception {
        onEdt(() -> {
            Entidade entity = add(new Entidade(diagram), "Entidade", 400, 300, 160, 80);
            link(entity, 0, new Point(400, 340), null, 0, new Point(150, 340));
            link(entity, 2, new Point(560, 340), null, 0, new Point(800, 340));
            diagram.setSelecionado(entity);
            paintDiagram();
            Point first = new Point(strip().getLeft() + 9, strip().getTop() + 9);
            diagram.mousePressed(mouse(MouseEvent.MOUSE_PRESSED, first, MouseEvent.BUTTON1));
            diagram.mouseReleased(mouse(MouseEvent.MOUSE_RELEASED, first, MouseEvent.BUTTON1));
            assertTrue(entity.isAncorado());
            assertSame(entity, diagram.getSelecionado());
            assertEquals(20, strip().getHeight(), "The strip stays horizontal after anchoring");
            assertEquals(Ancorador.CODE_DEL, strip().getAncorasCode().get(1));
            Point second = new Point(strip().getLeft() + 29, strip().getTop() + 9);
            assertTrue(strip().IsMe(second), "The second icon is hit");
            diagram.mousePressed(mouse(MouseEvent.MOUSE_PRESSED, second, MouseEvent.BUTTON1));
            assertSame(strip(), diagram.getElementarSobMouse(), "The delete action receives the press");
            diagram.mouseReleased(mouse(MouseEvent.MOUSE_RELEASED, second, MouseEvent.BUTTON1));
            assertFalse(diagram.getListaDeItens().contains(entity));
        });
    }

    @Test void linksAndSelectionRecomputeWithoutWaitingForPaint() throws Exception {
        onEdt(() -> {
            Entidade entity = add(new Entidade(diagram), "Entidade", 400, 300, 160, 80);
            diagram.setSelecionado(entity);
            assertEquals(entity.getLeft() - 26, strip().getLeft());
            Linha line = link(entity, 0, new Point(400, 340), null, 0, new Point(150, 340));
            strip().IsMe(new Point(0, 0));
            assertEquals(entity.getLeftWidth() + 8, strip().getLeft());
            line.getPontaA().setEm(null);
            strip().IsMe(new Point(0, 0));
            assertEquals(entity.getLeft() - 26, strip().getLeft());
            Atributo attribute = add(new Atributo(diagram), "Atributo", 650, 220, 120, 20);
            diagram.setSelecionado(attribute);
            assertEquals(attribute.getLeft() - 26, strip().getLeft());
        });
    }

    @Test void topAndBottomAreHorizontalAndWorkForOtherShapeTypes() throws Exception {
        onEdt(() -> {
            for (String type : new String[]{"entity", "relationship", "attribute", "table", "note"}) {
                diagram = editor.AddAsAtual(type.equals("table") ? "tpLogico" : type.equals("note") ? "tpLivre" : "tpConceitual");
                Forma shape = switch (type) {
                    case "entity" -> new Entidade(diagram);
                    case "relationship" -> new diagramas.conceitual.Relacionamento(diagram);
                    case "attribute" -> new Atributo(diagram);
                    case "table" -> new diagramas.logico.Tabela(diagram);
                    default -> new diagramas.livre.LivreNota(diagram);
                };
                add(shape, "Forma", 400, 300, 160, 80);
                link(shape, 0, new Point(400, 320), null, 0, new Point(150, 320));
                link(shape, 2, new Point(560, 320), null, 0, new Point(800, 320));
                diagram.setSelecionado(shape);
                Rectangle top = strip().getBounds();
                assertEquals(shape.getTop() - 28, top.y, shape.getClass().getSimpleName());
                assertEquals(20, top.height);
                assertEquals((shape.getAncorasCode().size() - 1) * 20 + 18, top.width);
                link(shape, 1, new Point(440, 300), null, 0, new Point(440, 100));
                strip().IsMe(new Point(0, 0));
                assertEquals(shape.getTopHeight() + 8, strip().getTop());
                assertEquals(20, strip().getHeight());
            }
        });
    }

    @Test void ownRouteThenNearbyShapesDecideBetweenFreeSides() throws Exception {
        onEdt(() -> {
            Atributo attribute = leftAttribute(false);
            Linha line = attribute.getListaDeLigacoes().get(0);
            // A bent route crosses the free right side, but leaves the top clear.
            line.setPontosParaDesenho(new Point[]{line.getPontaA().getCentro(), new Point(780, 350),
                    new Point(780, 600), new Point(200, 600), line.getPontaB().getCentro()});
            strip().Posicione(attribute);
            assertEquals(attribute.getTop() - 28, strip().getTop(), "Own route has priority");
            assertEquals(20, strip().getHeight());
            add(new diagramas.livre.LivreNota(diagram), "Nota", attribute.getLeft(), attribute.getTop() - 65, 130, 50);
            strip().Posicione(attribute);
            assertEquals(attribute.getTopHeight() + 8, strip().getTop(), "Avoid note bounds too");
        });
    }

    @Test void endpointOverlappingAPaintedIconWinsEvenWithoutMouseMove() throws Exception {
        onEdt(() -> {
            Atributo attribute = leftAttribute(false);
            paintDiagram();
            Rectangle icon = strip().getBounds();
            Point press = new Point(icon.x + 9, icon.y + 9);
            Linha line = link(attribute, 0, attribute.getListaDePontosLigados().get(0).getCentro(),
                    null, 0, press);
            PontoDeLinha endpoint = line.getPontaB();
            assertFalse(endpoint.isVisible());
            assertTrue(icon.contains(endpoint.getCentro()), "Endpoint overlaps the last painted icon");
            diagram.setElementarSobMouse(strip());
            diagram.mousePressed(mouse(MouseEvent.MOUSE_PRESSED, press, MouseEvent.BUTTON1));
            assertSame(endpoint, diagram.getElementarSobMouse());
            assertTrue(line.isSelecionado());
            Point drag = new Point(press.x + 35, press.y + 30);
            diagram.mouseDragged(mouse(MouseEvent.MOUSE_DRAGGED, drag, MouseEvent.NOBUTTON));
            assertEquals(drag, endpoint.getCentro(), "The line end actually follows the drag");
            diagram.mouseReleased(mouse(MouseEvent.MOUSE_RELEASED, drag, MouseEvent.BUTTON1));
            assertEquals(drag, endpoint.getCentro());
        });
    }

    @Test void pressAtALineEndOnAnUnselectedShapeEdgeStillSelectsTheShape() throws Exception {
        onEdt(() -> {
            Atributo attribute = leftAttribute(false);
            paintDiagram();
            PontoDeLinha attached = attribute.getListaDePontosLigados().get(0);
            Linha line = attached.getDono();
            PontoDeLinha onEntity = line.getPontaA() == attached ? line.getPontaB() : line.getPontaA();
            Forma entity = onEntity.getEm();
            // Just inside the entity, within the endpoint's enlarged hit area, away from the strip.
            Point press = new Point(onEntity.getCentro().x - 3, onEntity.getCentro().y);
            assertTrue(PosicionamentoAncorador.endpointHitArea(onEntity).contains(press));
            assertFalse(strip().IsMe(press));
            diagram.mouseMoved(mouse(MouseEvent.MOUSE_MOVED, press, MouseEvent.NOBUTTON));
            diagram.mousePressed(mouse(MouseEvent.MOUSE_PRESSED, press, MouseEvent.BUTTON1));
            diagram.mouseReleased(mouse(MouseEvent.MOUSE_RELEASED, press, MouseEvent.BUTTON1));
            assertSame(entity, diagram.getSelecionado(), "The shape is selected, the line is left alone");
            assertFalse(line.isSelecionado());
        });
    }

    private MouseEvent mouse(int id, Point point, int button) {
        return new MouseEvent(editor.getBox(), id, System.currentTimeMillis(),
                id == MouseEvent.MOUSE_DRAGGED ? MouseEvent.BUTTON1_DOWN_MASK : 0,
                point.x, point.y, 1, false, button);
    }

    private void assertClearOfEndpoints(Rectangle bounds) {
        for (var item : diagram.getListaDeItens()) {
            if (item instanceof desenho.linhas.Linha line) {
                for (var endpoint : new PontoDeLinha[]{line.getPontaA(), line.getPontaB()}) {
                    assertFalse(bounds.intersects(PosicionamentoAncorador.endpointHitArea(endpoint)));
                }
            }
        }
    }

    private static void assertClearOfLines(Forma shape, Rectangle bounds) {
        for (var line : shape.getListaDeLigacoes()) {
            Point[] points = line.getPontosParaDesenho();
            for (int i = 1; i < points.length; i++) assertFalse(bounds.intersectsLine(
                    points[i - 1].x, points[i - 1].y, points[i].x, points[i].y));
        }
    }

    private void paintDiagram() {
        BufferedImage image = new BufferedImage(1100, 650, BufferedImage.TYPE_INT_RGB);
        var g = image.createGraphics();
        try { diagram.ProcessPaint(g); } finally { g.dispose(); }
    }

    private Atributo leftAttribute(boolean corner) {
        Entidade entity = add(new Entidade(diagram), "Entidade_1", 280, 310, 160, 80);
        Atributo attribute = add(new Atributo(diagram), "Atributo_2", corner ? 448 : 600,
                corner ? 290 : 340, 120, 20);
        link(attribute, 0, new Point(attribute.getLeft(), attribute.getTop() + 10),
                entity, 2, new Point(entity.getLeftWidth(), corner ? entity.getTop() : 350));
        diagram.setSelecionado(attribute);
        return attribute;
    }

    private Entidade fourSides() {
        Entidade entity = add(new Entidade(diagram), "Entidade_1", 400, 280, 160, 100);
        Point[] near = {new Point(400, 285), new Point(405, 280), new Point(560, 285), new Point(405, 380)};
        Point[] far = {new Point(190, 285), new Point(405, 130), new Point(770, 285), new Point(405, 530)};
        for (int side = 0; side < 4; side++) {
            Linha line = link(entity, side, near[side], null, 0, far[side]);
            line.getPontaA().SetBounds(near[side].x - 10, near[side].y - 10, 20, 20);
            line.reSetBounds();
        }
        diagram.setSelecionado(entity);
        return entity;
    }

    private Linha link(Forma shape, int side, Point near, Forma partner, int otherSide, Point far) {
        desenho.linhas.SuperLinha line = shape instanceof diagramas.logico.Tabela ? new diagramas.logico.LogicoLinha(diagram) : new Ligacao(diagram);
        line.Inicie(0, far, near);
        PontoDeLinha a = line.getPontaA(), b = line.getPontaB();
        a.setEm(shape);
        a.setLado(side);
        a.setCentro(near);
        if (partner != null) b.setEm(partner);
        b.setLado(otherSide);
        b.setCentro(far);
        line.OrganizeLinha();
        line.reSetBounds();
        return line;
    }

    private <T extends Forma> T add(T shape, String text, int x, int y, int w, int h) {
        shape.setTexto(text);
        shape.SetBounds(x, y, w, h);
        return shape;
    }

    private Ancorador strip() throws Exception {
        var field = Diagrama.class.getDeclaredField("superAncorador");
        field.setAccessible(true);
        return (Ancorador) field.get(diagram);
    }

    private void capture(String name) throws Exception {
        frame.validate();
        Path output = Path.of(System.getProperty("brmodelo.snap.output"));
        Files.createDirectories(output);
        var root = frame.getRootPane();
        BufferedImage image = new BufferedImage(root.getWidth(), root.getHeight(), BufferedImage.TYPE_INT_RGB);
        var g = image.createGraphics();
        try { root.printAll(g); } finally { g.dispose(); }
        ImageIO.write(image, "png", output.resolve(name).toFile());
    }

    @FunctionalInterface private interface Action { void run() throws Exception; }
    private static void onEdt(Action action) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try { action.run(); } catch (Exception e) { throw new RuntimeException(e); }
        });
    }
}

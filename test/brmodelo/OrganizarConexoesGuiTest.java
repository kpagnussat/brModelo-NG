package brmodelo;

import controlador.Diagrama;
import controlador.Editor;
import desenho.Ancorador;
import desenho.formas.Forma;
import desenho.linhas.Linha;
import desenho.linhas.PontoDeLinha;
import diagramas.conceitual.Atributo;
import diagramas.conceitual.Entidade;
import diagramas.conceitual.Ligacao;
import diagramas.conceitual.Relacionamento;
import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

@Tag("gui")
class OrganizarConexoesGuiTest {
    private principal.FramePrincipal frame;
    private Editor editor;
    private Diagrama diagram;

    @BeforeEach void start() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            frame = new principal.FramePrincipal();
            editor = frame.getEditor();
            editor.setAutoSaveInterval(0);
            frame.setSize(1300, 900);
            frame.addNotify();
            frame.validate();
            diagram = editor.AddAsAtual("tpConceitual");
        });
    }

    @AfterEach void stop() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            if (editor != null) editor.EndAutoSave();
            if (frame != null) frame.dispose();
        });
    }

    @Test void iconDistributesAttributesAndRelationshipsAndOneUndoRestoresEverything() throws Exception {
        onEdt(() -> {
            Entidade entity = crowdedEntity();
            var endpoints = entity.getListaDePontosLigados();
            var partners = endpoints.stream().map(p -> p.getDono().getOutraPonta(p).getEm()).toList();
            var bounds = partners.stream().map(Forma::getBounds).toList();
            var farEnds = endpoints.stream().map(p -> p.getDono().getOutraPonta(p).getCentro()).toList();
            Rectangle entityBefore = entity.getBounds();
            diagram.DoMuda(null);
            String before = CanonicalDump.dump(diagram);
            capture("organize-before.png");
            entity.runAncorasCode(Ancorador.CODE_ORG_AT);
            assertSide(entity, 0, List.of(partners.get(0), partners.get(1), partners.get(3)), List.of(325, 370, 415));
            assertSide(entity, 2, List.of(partners.get(4), partners.get(2)), List.of(340, 400));
            assertEquals(entityBefore, entity.getBounds());
            for (int i = 0; i < partners.size(); i++) {
                Forma partner = partners.get(i);
                if (partner instanceof Relacionamento) {
                    assertEquals(bounds.get(i), partner.getBounds());
                    assertEquals(farEnds.get(i), endpoints.get(i).getDono().getOutraPonta(endpoints.get(i)).getCentro());
                } else {
                    PontoDeLinha p = endpoints.get(i);
                    assertEquals(p.getCentro().y, center(partner).y, "Attribute follows its new point");
                    assertEquals(p.getLado() == 0 ? entity.getLeft() - 40 - partner.getWidth()
                            : entity.getLeftWidth() + 40, partner.getLeft());
                }
            }
            String after = CanonicalDump.dump(diagram);
            assertNotEquals(before, after);
            capture("organize-after.png");
            assertTrue(editor.desfazer());
            assertEquals(before, CanonicalDump.dump(editor.diagramaAtual), "One undo restores points, sides, routes and attributes");
            assertTrue(editor.refazer());
            assertEquals(after, CanonicalDump.dump(editor.diagramaAtual));
        });
    }

    @Test void organizedFixtureOpensAndRoundTripsInTheOfficialJar() throws Exception {
        String jar = System.getProperty("oficialJar");
        Assumptions.assumeTrue(jar != null && !jar.isBlank(), "Set -PoficialJar");
        onEdt(() -> {
            Entidade entity = crowdedEntity();
            entity.DoAnyThing(entity.CONST_DO_ORGATTR);
            byte[] saved = Fixtures.save(diagram);
            assertEquals(CanonicalDump.dump(diagram), CanonicalDump.dump(Fixtures.load(saved)));
            OfficialCompatibilityTest.verify(diagram, jar, saved);
            String directory = System.getProperty("brmodelo.snap.output");
            if (directory != null) Files.write(Path.of(directory, "organized.brM3"), saved);
        });
    }

    @Test void selectedPartnersKeepTheirSideAndSelectedAttributesRemainUntouched() throws Exception {
        onEdt(() -> {
            Entidade entity = add(new Entidade(diagram), "Pessoa", 400, 280, 240, 180);
            Atributo selected = add(new Atributo(diagram), "Selecionado", 850, 240, 110, 20);
            Atributo moved = add(new Atributo(diagram), "Móvel", 130, 330, 110, 20);
            Relacionamento relation = add(new Relacionamento(diagram), "Selecionado", 850, 440, 100, 60);
            PontoDeLinha a = link(entity, selected, 0, 282, false).getPontaA();
            PontoDeLinha b = link(entity, moved, 2, 283, true).getPontaB();
            PontoDeLinha r = link(entity, relation, 1, 403, false).getPontaA();
            diagram.ClearSelect();
            for (Forma shape : List.of(entity, selected, relation)) diagram.DiagramaDoSelecao(shape, true, true);
            Rectangle selectedBefore = selected.getBounds();
            Rectangle relationBefore = relation.getBounds();
            entity.DoAnyThing(entity.CONST_DO_ORGATTR);
            assertEquals(0, a.getLado());
            assertEquals(0, b.getLado(), "Unselected attribute changes from right to left");
            assertEquals(1, r.getLado());
            assertEquals(selectedBefore, selected.getBounds());
            assertEquals(relationBefore, relation.getBounds());
            assertEquals(entity.getLeft() - 40 - moved.getWidth(), moved.getLeft());
            assertEquals(List.of(340, 400), entity.getListaDePontosLigados().stream()
                    .filter(p -> p.getLado() == 0).map(p -> p.getCentro().y).sorted().toList());
        });
    }

    @Test void relationshipsUseManualDocksAndConnectionOnlyChangesAreOneUndoStep() throws Exception {
        onEdt(() -> {
            Relacionamento relation = add(new Relacionamento(diagram), "Participa", 420, 280, 240, 180);
            var entities = List.of(add(new Entidade(diagram), "A", 100, 230, 120, 60),
                    add(new Entidade(diagram), "B", 100, 450, 120, 60));
            var ends = new ArrayList<PontoDeLinha>();
            for (Forma entity : entities) ends.add(link(relation, entity, 2, 370, false).getPontaA());
            diagram.setSelecionado(relation);
            diagram.DoMuda(null);
            String before = CanonicalDump.dump(diagram);
            relation.DoAnyThing(relation.CONST_DO_ORGATTR);
            assertEquals(List.of(0, 0), ends.stream().map(PontoDeLinha::getLado).toList());
            Point[] docks = relation.getAllSubPoints();
            assertEquals(docks[4], ends.get(0).getCentro());
            assertEquals(docks[8], ends.get(1).getCentro());
            for (PontoDeLinha p : ends) {
                Point organized = p.getCentro();
                relation.PosicionePonto(p);
                assertEquals(organized, p.getCentro(), "Identical to releasing a manual drag on a dock");
            }
            String after = CanonicalDump.dump(diagram);
            assertTrue(editor.desfazer());
            assertEquals(before, CanonicalDump.dump(editor.diagramaAtual));
            assertTrue(editor.refazer());
            assertEquals(after, CanonicalDump.dump(editor.diagramaAtual));
            // Already organized, without attributes: do not append a spurious undo state.
            ((Relacionamento) editor.diagramaAtual.FindByID(relation.getID())).DoAnyThing(relation.CONST_DO_ORGATTR);
            assertTrue(editor.desfazer());
            assertEquals(before, CanonicalDump.dump(editor.diagramaAtual));
        });
    }

    @Test void selfLinkKeepsBothSidesAndDanglingLinksAreDistributedWithoutMovingTheirFarEnd() throws Exception {
        onEdt(() -> {
            Entidade entity = add(new Entidade(diagram), "Auto", 420, 280, 240, 180);
            Ligacao self = new Ligacao(diagram);
            self.Inicie(0, new Point(660, 310), new Point(450, 280));
            // SetEm also supports fixtures already stored with both ends on the same shape.
            self.getPontaA().SetEm(entity);
            self.getPontaB().SetEm(entity);
            entity.PosicionePonto(self.getPontaA());
            entity.PosicionePonto(self.getPontaB());
            diagram.Add(self);
            Ligacao dangling = new Ligacao(diagram);
            dangling.Inicie(0, new Point(900, 50), new Point(430, 280));
            dangling.getPontaA().LigarA(entity);
            diagram.Add(dangling);
            Point farEnd = dangling.getPontaB().getCentro();
            diagram.setSelecionado(entity);
            entity.DoAnyThing(entity.CONST_DO_ORGATTR);
            assertEquals(1, self.getPontaA().getLado());
            assertEquals(2, self.getPontaB().getLado());
            assertEquals(1, dangling.getPontaA().getLado());
            assertEquals(farEnd, dangling.getPontaB().getCentro());
            assertEquals(List.of(500, 580), entity.getListaDePontosLigados().stream()
                    .filter(p -> p.getLado() == 1).map(p -> p.getCentro().x).sorted().toList());
        });
    }

    @Test void conceptualSelfRelationshipKeepsItsSidesEvenIfTheDiamondIsOnTheOtherSide() throws Exception {
        onEdt(() -> {
            Entidade entity = add(new Entidade(diagram), "Pessoa", 420, 280, 240, 180);
            Relacionamento relation = add(new Relacionamento(diagram), "Supervisiona", 100, 320, 150, 60);
            PontoDeLinha a = link(entity, relation, 2, 340, false).getPontaA();
            PontoDeLinha b = link(entity, relation, 2, 400, true).getPontaB();
            assertTrue(relation.isAutoRelacionamento());
            diagram.setSelecionado(entity);
            entity.DoAnyThing(entity.CONST_DO_ORGATTR);
            assertEquals(2, a.getLado());
            assertEquals(2, b.getLado());
            assertEquals(List.of(340, 400), List.of(a.getCentro().y, b.getCentro().y));
            diagram.setSelecionado(relation);
            List<Integer> sides = relation.getListaDePontosLigados().stream().map(PontoDeLinha::getLado).toList();
            relation.DoAnyThing(relation.CONST_DO_ORGATTR);
            assertEquals(sides, relation.getListaDePontosLigados().stream().map(PontoDeLinha::getLado).toList());
        });
    }

    private Entidade crowdedEntity() {
        Entidade entity = add(new Entidade(diagram), "Pessoa", 420, 280, 240, 180);
        var partners = List.of(add(new Atributo(diagram), "Código", 130, 260, 110, 20),
                add(new Atributo(diagram), "Nome", 130, 330, 110, 20),
                add(new Atributo(diagram), "Contato", 850, 430, 110, 20),
                add(new Relacionamento(diagram), "Solicita", 190, 480, 100, 60),
                add(new Relacionamento(diagram), "Participa", 800, 230, 100, 60));
        for (int i = 0; i < partners.size(); i++) link(entity, partners.get(i), i < 3 ? 1 : 0,
                i < 3 ? 425 + i * 3 : 282 + i * 3, i % 2 == 1);
        diagram.setSelecionado(entity);
        return entity;
    }

    private <T extends Forma> T add(T shape, String text, int x, int y, int w, int h) {
        shape.setTexto(text);
        shape.SetBounds(x, y, w, h);
        diagram.Add(shape);
        return shape;
    }

    private Ligacao link(Forma organized, Forma partner, int side, int coordinate, boolean reverse) {
        Ligacao line = new Ligacao(diagram);
        Point near = switch (side) {
            case 0 -> new Point(organized.getLeft(), coordinate);
            case 1 -> new Point(coordinate, organized.getTop());
            case 2 -> new Point(organized.getLeftWidth(), coordinate);
            default -> new Point(coordinate, organized.getTopHeight());
        };
        line.Inicie(0, reverse ? near : center(partner), reverse ? center(partner) : near);
        (reverse ? line.getPontaB() : line.getPontaA()).LigarA(organized);
        (reverse ? line.getPontaA() : line.getPontaB()).LigarA(partner);
        diagram.Add(line);
        return line;
    }

    private static Point center(Forma shape) {
        return new Point(shape.getLeft() + shape.getWidth() / 2, shape.getTop() + shape.getHeight() / 2);
    }

    private static void assertSide(Forma shape, int side, List<Forma> partners, List<Integer> coordinates) {
        var points = shape.getListaDePontosLigados().stream().filter(p -> p.getLado() == side)
                .sorted(Comparator.comparingInt(p -> side % 2 == 0 ? p.getCentro().y : p.getCentro().x)).toList();
        assertEquals(partners, points.stream().map(p -> p.getDono().getOutraPonta(p).getEm()).toList());
        assertEquals(coordinates, points.stream().map(p -> side % 2 == 0 ? p.getCentro().y : p.getCentro().x).toList());
        for (PontoDeLinha point : points) {
            Point before = point.getCentro();
            shape.PosicionePonto(point);
            assertEquals(before, point.getCentro());
        }
    }

    private void capture(String name) throws Exception {
        String directory = System.getProperty("brmodelo.snap.output");
        if (directory == null) return;
        Files.createDirectories(Path.of(directory));
        BufferedImage image = new BufferedImage(1100, 650, BufferedImage.TYPE_INT_RGB);
        var graphics = image.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
            diagram.ProcessPaint(graphics);
        } finally { graphics.dispose(); }
        ImageIO.write(image, "png", Path.of(directory, name).toFile());
    }

    @FunctionalInterface private interface CheckedAction { void run() throws Exception; }
    private static void onEdt(CheckedAction action) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try { action.run(); }
            catch (Exception e) { throw new RuntimeException(e); }
        });
    }
}

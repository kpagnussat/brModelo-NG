package controlador;

import desenho.FormaElementar;
import desenho.formas.Forma;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

@Tag("gui")
class EncaixeGradeGuiTest {
    private principal.FramePrincipal frame;
    private Editor editor;
    private String previous;

    @BeforeEach void start() throws Exception {
        previous = Editor.fromConfiguracao.getValor(EncaixeGrade.CHAVE);
        SwingUtilities.invokeAndWait(() -> {
            frame = new principal.FramePrincipal();
            editor = frame.getEditor();
            editor.setAutoSaveInterval(0);
            frame.setSize(1300, 900);
            frame.addNotify();
            frame.validate();
            editor.AddAsAtual("tpConceitual");
            editor.setShowGrid(true);
            editor.setGridWidth(20);
            enabled(true);
        });
    }

    @AfterEach void stop() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Editor.fromConfiguracao.SetAndSaveIfNeed(EncaixeGrade.CHAVE, previous);
            if (editor != null) editor.EndAutoSave();
            if (frame != null) frame.dispose();
        });
    }

    @Test void canvasDragSnapsAtEveryZoomAndAltCanToggleDuringGesture() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            FormaElementar entity = entity(43, 51);
            assertEquals(new Point(43, 51), entity.getLocation(), "Existing/manual coordinates are untouched");
            for (double zoom : new double[]{0.5, 1, 1.5, 2}) {
                editor.setZoom(zoom);
                entity.SetBounds(43, 51, 120, 58);
                drag(entity, 34, 26, 0);
                assertEquals(new Point(80, 80), entity.getLocation(), "zoom=" + zoom);
                enabled(false);
                entity.SetBounds(43, 51, 120, 58);
                drag(entity, 34, 26, 0);
                assertEquals(new Point(77, 77), entity.getLocation());
                enabled(true);
                entity.SetBounds(43, 51, 120, 58);
                drag(entity, 34, 26, MouseEvent.ALT_DOWN_MASK);
                assertEquals(new Point(77, 77), entity.getLocation());
            }
            editor.setZoom(1);
            entity.SetBounds(43, 51, 120, 58);
            Point start = new Point(80, 75);
            send(MouseEvent.MOUSE_PRESSED, start, 0);
            send(MouseEvent.MOUSE_DRAGGED, new Point(94, 87), 0);
            assertEquals(new Point(60, 60), entity.getLocation());
            send(MouseEvent.MOUSE_DRAGGED, new Point(97, 91), MouseEvent.ALT_DOWN_MASK);
            assertEquals(new Point(60, 67), entity.getLocation());
            send(MouseEvent.MOUSE_DRAGGED, new Point(114, 101), 0);
            send(MouseEvent.MOUSE_RELEASED, new Point(114, 101), 0);
            assertEquals(new Point(80, 80), entity.getLocation());
        });
    }

    @Test void selectionOffsetsAndOneUndoStepSurviveMultipleDragEvents() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            FormaElementar anchor = entity(43, 51);
            FormaElementar other = entity(237, 129);
            editor.diagramaAtual.ClearSelect();
            editor.diagramaAtual.DiagramaDoSelecao(anchor, true, true);
            editor.diagramaAtual.DiagramaDoSelecao(other, true, true);
            editor.diagramaAtual.DoMuda(null);
            Point start = new Point(80, 75);
            send(MouseEvent.MOUSE_PRESSED, start, 0);
            send(MouseEvent.MOUSE_DRAGGED, new Point(94, 87), 0);
            send(MouseEvent.MOUSE_DRAGGED, new Point(114, 101), 0);
            send(MouseEvent.MOUSE_RELEASED, new Point(114, 101), 0);
            assertEquals(new Point(80, 80), anchor.getLocation());
            assertEquals(new Point(274, 158), other.getLocation());
            int id = anchor.getID();
            assertTrue(editor.desfazer());
            assertEquals(new Point(43, 51), editor.diagramaAtual.FindByID(id).getLocation());
            assertTrue(editor.refazer());
            assertEquals(new Point(80, 80), editor.diagramaAtual.FindByID(id).getLocation());
        });
    }

    @Test void creationAtPageEdgeStaysAlignedAndInsideThePage() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Diagrama diagram = editor.diagramaAtual;
            editor.setGridWidth(37);
            diagram.setComando(Controler.Comandos.cmdEntidade);
            send(MouseEvent.MOUSE_PRESSED, new Point(diagram.getWidth() - 2, diagram.getHeight() - 2), 0);
            var entity = diagram.getListaDeItens().stream()
                    .filter(item -> item instanceof diagramas.conceitual.Entidade).findFirst().orElseThrow();
            assertEquals(0, entity.getLeft() % 37);
            assertEquals(0, entity.getTop() % 37);
            assertTrue(entity.getLeftWidth() <= diagram.getWidth());
            assertTrue(entity.getTopHeight() <= diagram.getHeight());
        });
    }

    @Test void selectionKeepsItsOffsetsAtThePageEdge() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            FormaElementar anchor = entity(243, 251);
            FormaElementar other = entity(7, 129);
            Diagrama diagram = editor.diagramaAtual;
            diagram.ClearSelect();
            diagram.DiagramaDoSelecao(anchor, true, true);
            diagram.DiagramaDoSelecao(other, true, true);
            drag(anchor, -300, 0, 0);
            assertEquals(new Point(240, 260), anchor.getLocation());
            assertEquals(new Point(4, 138), other.getLocation());
        });
    }

    @Test void automaticallyPlacedAttributeIsOnlySnappedWhenDragged() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            FormaElementar entity = entity(43, 51);
            Diagrama diagram = editor.diagramaAtual;
            diagram.setComando(Controler.Comandos.cmdAtributo);
            send(MouseEvent.MOUSE_PRESSED, new Point(155, 80), 0);
            var attribute = diagram.getListaDeItens().stream()
                    .filter(item -> item instanceof diagramas.conceitual.Atributo).findFirst().orElseThrow();
            assertNotEquals(0, attribute.getTop() % 20, "Keep the automatic satellite placement");
            var link = diagram.getListaDeItens().stream()
                    .filter(item -> item instanceof desenho.linhas.Linha).map(item -> (desenho.linhas.Linha) item)
                    .findFirst().orElseThrow();
            var endpoint = link.getPontaA().getEm() == attribute ? link.getPontaA() : link.getPontaB();
            Point endpointBefore = endpoint.getCentro();
            Point before = attribute.getLocation();
            diagram.setSelecionado(attribute);
            drag(attribute, 34, 26, 0);
            assertEquals(0, attribute.getLeft() % 20);
            assertEquals(0, attribute.getTop() % 20);
            assertEquals(new Point(endpointBefore.x + attribute.getLeft() - before.x,
                    endpointBefore.y + attribute.getTop() - before.y), endpoint.getCentro(), "Link follows the shape");
            assertEquals(new Point(43, 51), entity.getLocation());
        });
    }

    @Test void creationResizeAndKeyboardUseConfiguredGrid() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Diagrama diagram = editor.diagramaAtual;
            diagram.setComando(Controler.Comandos.cmdEntidade);
            send(MouseEvent.MOUSE_PRESSED, new Point(43, 51), 0);
            Forma entity = (Forma) diagram.getListaDeItens().stream()
                    .filter(item -> item instanceof diagramas.conceitual.Entidade).findFirst().orElseThrow();
            assertEquals(new Point(40, 60), entity.getLocation());
            diagram.setSelecionado(entity);
            var handle = entity.getPontos()[2];
            Point start = new Point(handle.getLeft() + 2, handle.getTop() + 2);
            send(MouseEvent.MOUSE_MOVED, start, 0);
            send(MouseEvent.MOUSE_PRESSED, start, 0);
            send(MouseEvent.MOUSE_DRAGGED, new Point(start.x + 13, start.y + 9), 0);
            send(MouseEvent.MOUSE_RELEASED, new Point(start.x + 13, start.y + 9), 0);
            assertEquals(new Rectangle(40, 60, 140, 60), entity.getBounds());
            diagram.ProcesseTeclas(new KeyEvent(editor.getBox(), KeyEvent.KEY_PRESSED, 0,
                    KeyEvent.CTRL_DOWN_MASK, KeyEvent.VK_RIGHT, KeyEvent.CHAR_UNDEFINED));
            assertEquals(60, entity.getLeft());
            diagram.ProcesseTeclas(KeyEvent.VK_DOWN);
            assertEquals(80, entity.getTop());
            enabled(false);
            diagram.ProcesseTeclas(KeyEvent.VK_RIGHT);
            assertEquals(63, entity.getLeft());
            diagram.ProcesseTeclas(new KeyEvent(editor.getBox(), KeyEvent.KEY_PRESSED, 0,
                    KeyEvent.CTRL_DOWN_MASK, KeyEvent.VK_RIGHT, KeyEvent.CHAR_UNDEFINED));
            assertEquals(64, entity.getLeft());
            // Alt also bypasses the grid for resize handles.
            diagram.setSelecionado(entity);
            handle = entity.getPontos()[2];
            start = new Point(handle.getLeft() + 2, handle.getTop() + 2);
            enabled(true);
            send(MouseEvent.MOUSE_MOVED, start, 0);
            send(MouseEvent.MOUSE_PRESSED, start, 0);
            send(MouseEvent.MOUSE_DRAGGED, new Point(start.x + 13, start.y + 7), MouseEvent.ALT_DOWN_MASK);
            send(MouseEvent.MOUSE_RELEASED, new Point(start.x + 13, start.y + 7), MouseEvent.ALT_DOWN_MASK);
            assertEquals(new Rectangle(64, 80, 153, 67), entity.getBounds());
            editor.setGridWidth(0);
            assertEquals(20, editor.getGridWidth());
        });
    }

    @Test void settingPersistsViaConfigurationInspectorAndCaptureIsOptional() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                var property = ConfiguracaoEditor.GenerateProperty(editor).stream()
                        .filter(p -> EncaixeGrade.CHAVE.equals(p.configuracaoStr)).findFirst().orElseThrow();
                assertTrue(ConfiguracaoEditor.AceitaEdicao(editor, property, "false"));
                assertEquals("false", new Configuer().getValor(EncaixeGrade.CHAVE));
                assertTrue(ConfiguracaoEditor.AceitaEdicao(editor, property, "true"));
                assertEquals("true", new Configuer().getValor(EncaixeGrade.CHAVE));
                FormaElementar entity = entity(43, 51);
                capture("snap-before.png");
                drag(entity, 34, 26, 0);
                assertEquals(new Point(80, 80), entity.getLocation());
                capture("snap-after.png");
            } catch (Exception e) { throw new RuntimeException(e); }
        });
    }

    private FormaElementar entity(int x, int y) {
        var entity = editor.diagramaAtual.ExternalRealiseComando(Controler.Comandos.cmdEntidade, new Point(x, y));
        entity.SetBounds(x, y, 120, 58);
        editor.diagramaAtual.setSelecionado(entity);
        return entity;
    }

    private void enabled(boolean value) { Editor.fromConfiguracao.getConfiguracao().put(EncaixeGrade.CHAVE, String.valueOf(value)); }

    private void drag(FormaElementar entity, int dx, int dy, int modifiers) {
        Point start = new Point(entity.getLeft() + 37, entity.getTop() + Math.min(24, entity.getHeight() / 2));
        send(MouseEvent.MOUSE_MOVED, start, modifiers);
        send(MouseEvent.MOUSE_PRESSED, start, modifiers);
        send(MouseEvent.MOUSE_DRAGGED, new Point(start.x + dx, start.y + dy), modifiers);
        send(MouseEvent.MOUSE_RELEASED, new Point(start.x + dx, start.y + dy), modifiers);
    }

    private void send(int id, Point diagramPoint, int modifiers) {
        double zoom = editor.diagramaAtual.getZoom();
        editor.getBox().dispatchEvent(new MouseEvent(editor.getBox(), id, System.currentTimeMillis(), modifiers,
                (int) Math.round(diagramPoint.x * zoom), (int) Math.round(diagramPoint.y * zoom), 1, false,
                id == MouseEvent.MOUSE_DRAGGED || id == MouseEvent.MOUSE_MOVED ? MouseEvent.NOBUTTON : MouseEvent.BUTTON1));
    }

    private void capture(String name) throws Exception {
        String directory = System.getProperty("brmodelo.snap.output");
        if (directory == null) return;
        Path output = Path.of(directory);
        Files.createDirectories(output);
        BufferedImage image = new BufferedImage(620, 360, BufferedImage.TYPE_INT_RGB);
        var graphics = image.createGraphics();
        editor.getBox().paint(graphics);
        graphics.dispose();
        ImageIO.write(image, "png", output.resolve(name).toFile());
    }
}

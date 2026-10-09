package brmodelo;

import controlador.Diagrama;
import controlador.Editor;
import controlador.apoios.GuardaPadraoBrM;
import desenho.formas.Forma;
import diagramas.conceitual.Atributo;
import diagramas.conceitual.Entidade;
import diagramas.conceitual.Ligacao;
import diagramas.conceitual.Relacionamento;
import java.awt.Point;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * A large, interlinked diagram must open, save and snapshot from a thread with a small stack.
 * A fresh application overflows the EDT's 1 MB at ~2,000 items while serialization still runs
 * interpreted; JIT-compiled frames are smaller, so the test uses a bigger diagram on a 256 KB
 * stack, which overflows either way.
 */
@Tag("gui")
class DiagramaGrandeGuiTest {
    private static final long SMALL_STACK = 256L << 10;
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

    @Test void largeInterlinkedDiagramRoundTripsFromASmallStack() throws Exception {
        SwingUtilities.invokeAndWait(() -> grid(20, 10));
        int items = diagram.getListaDeItens().size();
        assertTrue(items > 5000, "items=" + items);

        // Control: the plain recursive walk really does not fit, so the test proves the fix.
        Throwable plain = onThread(SMALL_STACK, () -> {
            try (ObjectOutputStream out = new ObjectOutputStream(new ByteArrayOutputStream())) {
                out.writeObject(diagram);
            }
            return null;
        }).error;
        assertInstanceOf(StackOverflowError.class, plain);

        Outcome<String> roundTrip = onThread(SMALL_STACK, () -> {
            ByteArrayOutputStream snapshot = Diagrama.SaveToStream(diagram); // also every undo step
            assertNotNull(snapshot);
            Diagrama copy = Diagrama.LoadFromStream(snapshot);
            assertNotNull(copy);
            Diagrama opened = new GuardaPadraoBrM(diagram).getDiagrama(); // the file envelope
            assertNotNull(opened);
            assertEquals(items, copy.getListaDeItens().size());
            assertEquals(items, opened.getListaDeItens().size());
            return CanonicalDump.dump(opened);
        });
        assertNull(roundTrip.error, () -> String.valueOf(roundTrip.error));
        assertEquals(CanonicalDump.dump(diagram), roundTrip.value);
    }

    private void grid(int cols, int rows) {
        Entidade[][] e = new Entidade[rows][cols];
        for (int r = 0; r < rows; r++) for (int c = 0; c < cols; c++) {
            int x = 160 + c * 440, y = 120 + r * 330;
            e[r][c] = add(new Entidade(diagram), "E" + r + "_" + c, x, y, 120, 50);
            for (int i = 0; i < 3; i++) link(e[r][c], add(new Atributo(diagram), "a" + r + c + i,
                    x - 110 + i * 120, y - 70, 80, 18));
        }
        int n = 0;
        for (int r = 0; r < rows; r++) for (int c = 0; c < cols; c++) {
            if (c + 1 < cols) relate(e[r][c], e[r][c + 1], 390 + c * 440, 125 + r * 330, n++);
            if (r + 1 < rows) relate(e[r][c], e[r + 1][c], 170 + c * 440, 295 + r * 330, n++);
        }
    }

    private void relate(Entidade p, Entidade q, int x, int y, int n) {
        Relacionamento relation = add(new Relacionamento(diagram), "r" + n, x, y, 100, 50);
        link(relation, p);
        link(relation, q);
    }

    private <T extends Forma> T add(T shape, String text, int x, int y, int w, int h) {
        shape.setTexto(text);
        shape.SetBounds(x, y, w, h);
        diagram.Add(shape);
        return shape;
    }

    private void link(Forma a, Forma b) {
        Ligacao line = new Ligacao(diagram);
        line.Inicie(0, center(a), center(b));
        line.getPontaA().LigarA(a);
        line.getPontaB().LigarA(b);
        diagram.Add(line);
        a.PosicionePonto(line.getPontaA());
        b.PosicionePonto(line.getPontaB());
        line.PrepareCardinalidade();
    }

    private static Point center(Forma f) {
        return new Point(f.getLeft() + f.getWidth() / 2, f.getTop() + f.getHeight() / 2);
    }

    private record Outcome<T>(T value, Throwable error) {}

    private interface Work<T> { T run() throws Exception; }

    private static <T> Outcome<T> onThread(long stack, Work<T> work) throws InterruptedException {
        AtomicReference<T> value = new AtomicReference<>();
        AtomicReference<Throwable> error = new AtomicReference<>();
        Thread thread = new Thread(null, () -> {
            try {
                value.set(work.run());
            } catch (Throwable t) {
                error.set(t);
            }
        }, "small-stack", stack);
        thread.start();
        thread.join();
        return new Outcome<>(value.get(), error.get());
    }
}

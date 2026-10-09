package controlador;

import java.awt.Point;
import java.awt.Rectangle;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EncaixeGradeTest {
    @Test void positionsRoundToNearestGridIncludingNegativeCoordinates() {
        assertEquals(new Point(40, 60), EncaixeGrade.posicao(new Point(47, 51), 20, true, false));
        assertEquals(new Point(-40, -60), EncaixeGrade.posicao(new Point(-47, -51), 20, true, false));
        assertEquals(new Point(30, 60), EncaixeGrade.posicao(new Point(37, 53), 15, true, false));
        assertEquals(40, EncaixeGrade.multiplo(30, 20));
    }

    @Test void zoomIsConvertedBeforeSnappingInDiagramCoordinates() {
        for (double zoom : new double[]{0.5, 1, 1.5, 2, 4}) {
            Point screen = new Point((int) (46 * zoom), (int) (54 * zoom));
            Point diagram = EncaixeGrade.coordenadas(screen, zoom);
            assertEquals(new Point(46, 54), diagram);
            assertEquals(new Point(40, 60), EncaixeGrade.posicao(diagram, 20, true, false));
            assertEquals(diagram, EncaixeGrade.posicao(diagram, 20, true, true));
        }
    }

    @Test void disabledAndAltPreserveRawPositionsAndSizes() {
        Point raw = new Point(47, 51);
        assertEquals(raw, EncaixeGrade.posicao(raw, 20, false, false));
        assertEquals(raw, EncaixeGrade.posicao(raw, 20, true, true));
        Rectangle bounds = new Rectangle(47, 51, 123, 58);
        assertEquals(new Rectangle(47, 51, 136, 65), EncaixeGrade.tamanho(bounds, 2, 13, 7, 20, true, true));
        assertEquals(new Rectangle(47, 51, 136, 65), EncaixeGrade.tamanho(bounds, 2, 13, 7, 20, false, false));
    }

    @Test void anchorDeltaPreservesOffsetsAndDoesNotAccumulateRounding() {
        Point start = new Point(47, 51), mouseStart = new Point(90, 80);
        Point delta = EncaixeGrade.deslocamento(start, mouseStart, new Point(103, 97), start, 20, true, false);
        assertEquals(new Point(13, 9), delta);
        Point satellite = new Point(183, 109);
        satellite.translate(delta.x, delta.y);
        assertEquals(new Point(196, 118), satellite);
        assertEquals(new Point(136, 58), new Point(satellite.x - 60, satellite.y - 60));
        assertEquals(new Point(20, 20), EncaixeGrade.deslocamento(start, mouseStart,
                new Point(123, 117), new Point(60, 60), 20, true, false));
        assertEquals(new Point(0, 8), EncaixeGrade.deslocamento(start, mouseStart,
                new Point(103, 97), new Point(60, 60), 20, true, true));
    }

    @Test void pageLimitsUseInwardGridLinesWithoutDistortingTheSelection() {
        assertEquals(40, EncaixeGrade.limitar(0, 36, 913, 20, true));
        assertEquals(900, EncaixeGrade.limitar(940, 36, 913, 20, true));
        assertEquals(36, EncaixeGrade.limitar(0, 36, 913, 20, false));
    }

    @Test void sizesAndPositionsSnapForEveryResizeHandleAndRespectMinimum() {
        Rectangle original = new Rectangle(43, 51, 123, 58);
        for (int handle = 0; handle < 8; handle++) {
            Rectangle result = EncaixeGrade.tamanho(original, handle, 13, 7, 20, true, false);
            assertEquals(0, result.x % 20);
            assertEquals(0, result.y % 20);
            assertEquals(0, result.width % 20);
            assertEquals(0, result.height % 20);
        }
        assertEquals(new Rectangle(40, 60, 140, 60), EncaixeGrade.tamanho(original, 2, 13, 7, 20, true, false));
        assertEquals(20, EncaixeGrade.tamanho(original, 2, -500, -500, 20, true, false).width);
        assertEquals(12, EncaixeGrade.tamanho(original, 2, -500, -500, 3, true, false).height);
    }
}

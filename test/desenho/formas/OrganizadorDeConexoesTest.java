package desenho.formas;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OrganizadorDeConexoesTest {
    @Test void everyDirectionUsesThePartnerCenter() {
        Rectangle square = new Rectangle(100, 100, 100, 100);
        assertEquals(0, side(square, 10, 150));
        assertEquals(1, side(square, 150, 10));
        assertEquals(2, side(square, 300, 150));
        assertEquals(3, side(square, 150, 300));
        assertEquals(1, side(square, 120, 10));
        assertEquals(1, side(square, 180, 10));
        assertEquals(3, side(square, 120, 300));
        assertEquals(3, side(square, 180, 300));
        assertEquals(0, side(square, 10, 120));
        assertEquals(0, side(square, 10, 180));
        assertEquals(2, side(square, 300, 120));
        assertEquals(2, side(square, 300, 180));
    }

    @Test void diagonalChoiceAccountsForWideAndTallShapes() {
        Rectangle wide = new Rectangle(-200, -50, 400, 100);
        Rectangle tall = new Rectangle(-50, -200, 100, 400);
        for (int x : new int[]{-300, 300}) {
            for (int y : new int[]{-300, 300}) {
                assertEquals(y < 0 ? 1 : 3, side(wide, x, y));
                assertEquals(x < 0 ? 0 : 2, side(tall, x, y));
            }
        }
        assertEquals(2, side(wide, 500, -100));
        assertEquals(1, side(tall, 100, -500));
    }

    @Test void retainedSidesAndCoincidentCentersAreStable() {
        Rectangle shape = new Rectangle(100, 100, 100, 100);
        for (int side = 0; side < 4; side++) {
            assertEquals(side, OrganizadorDeConexoes.lado(shape, new Point(300, 10), side, true));
            assertEquals(side, OrganizadorDeConexoes.lado(shape, new Point(150, 150), side, false));
        }
    }

    @Test void orderingUsesXOnHorizontalSidesAndYOnVerticalSides() {
        List<Point> partners = List.of(new Point(300, 20), new Point(100, 80), new Point(200, 50));
        for (int side = 0; side < 4; side++) {
            final int s = side;
            var ordered = new ArrayList<>(partners);
            ordered.sort(Comparator.comparingInt(p -> OrganizadorDeConexoes.eixo(p, s)));
            assertEquals(side % 2 == 0 ? List.of(partners.get(0), partners.get(2), partners.get(1))
                    : List.of(partners.get(1), partners.get(2), partners.get(0)), ordered);
        }
    }

    @Test void spacingIncludesTheTwoEndGapsAndRespectsMarginsWithoutRoundingDrift() {
        assertEquals(List.of(125, 150, 175), positions(100, 100, 0, 3));
        assertEquals(List.of(33, 67), positions(0, 100, 0, 2));
        assertEquals(List.of(20, 33, 50, 67, 80), positions(0, 100, 20, 5));
        assertEquals(List.of(2, 2, 2), positions(0, 4, 20, 3));
        assertEquals(List.of(0, 1, 1, 1, 2), positions(0, 2, 0, 5));
        assertEquals(List.of(150), positions(100, 100, 0, 1));
    }

    private static List<Integer> positions(int start, int length, int margin, int count) {
        var result = new ArrayList<Integer>();
        for (int i = 1; i <= count; i++) result.add(OrganizadorDeConexoes.coordenada(start, length, margin, i, count));
        return result;
    }

    private static int side(Rectangle shape, int x, int y) {
        return OrganizadorDeConexoes.lado(shape, new Point(x, y), 0, false);
    }
}

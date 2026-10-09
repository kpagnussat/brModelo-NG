package util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PaletaTest {
    @Test void clampsAndFloorsAvailableSpaceForEachToolCount() {
        assertEquals(40, Paleta.tamanho(1000, 17, 1));
        assertEquals(35, Paleta.tamanho(600, 17, 1));
        assertEquals(28, Paleta.tamanho(476, 17, 1));
        assertEquals(28, Paleta.tamanho(475, 17, 1));
        assertEquals(28, Paleta.tamanho(0, 17, 1));
        assertEquals(40, Paleta.tamanho(600, 8, 1));
        assertEquals(36, Paleta.tamanho(658, 18, 1));
        assertEquals(28, Paleta.tamanho(490, 18, 1));
        assertEquals(40, Paleta.tamanho(0, 0, 1));
    }
    @Test void scalesBothLimitsAndScrollsOnlyBelowMinimum() {
        for (float scale : new float[]{1, 1.25f, 1.5f, 2}) {
            int min = Math.round(28 * scale), max = Math.round(40 * scale);
            assertEquals(max, Paleta.tamanho(2000, 17, scale));
            for (int tools : new int[]{0, 8, 17, 18, 20}) {
                for (int height = 0; height <= 2000; height++) {
                    int size = Paleta.tamanho(height, tools, scale);
                    assertTrue(size >= min && size <= max);
                    assertEquals(height < tools * min, tools * size > height,
                            "overflow threshold at scale " + scale + ", height " + height);
                }
            }
        }
    }
}

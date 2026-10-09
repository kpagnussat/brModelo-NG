package util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RenderizacaoTest {
    @Test void linuxDefaultsToJavaRenderingUnlessTheUserChose() {
        assertEquals("false", Renderizacao.padrao("Linux", null));
        assertNull(Renderizacao.padrao("Linux", "true"), "an explicit choice is kept");
        assertNull(Renderizacao.padrao("Windows 11", null));
        assertNull(Renderizacao.padrao("Mac OS X", null));
    }

    @Test void bothPipelinePropertiesAreSetOnLinuxOnly() {
        String os = System.getProperty("os.name");
        String xr = System.getProperty(Renderizacao.XRENDER), pm = System.getProperty(Renderizacao.PIXMAPS);
        try {
            System.clearProperty(Renderizacao.XRENDER);
            System.setProperty(Renderizacao.PIXMAPS, "true");
            Renderizacao.configurar();
            boolean linux = os.toLowerCase(java.util.Locale.ROOT).contains("linux");
            assertEquals(linux ? "false" : null, System.getProperty(Renderizacao.XRENDER));
            assertEquals("true", System.getProperty(Renderizacao.PIXMAPS), "explicit value kept");
        } finally {
            if (xr == null) System.clearProperty(Renderizacao.XRENDER); else System.setProperty(Renderizacao.XRENDER, xr);
            if (pm == null) System.clearProperty(Renderizacao.PIXMAPS); else System.setProperty(Renderizacao.PIXMAPS, pm);
        }
    }
}

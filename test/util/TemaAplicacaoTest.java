package util;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class TemaAplicacaoTest {
    @TempDir java.nio.file.Path temporary;
    @Test void commandLineWinsSavedChoiceAndMissingChoiceFollowsDesktop() {
        assertEquals("claro", TemaAplicacao.escolhaInicial("claro", "escuro"));
        assertEquals("escuro", TemaAplicacao.escolhaInicial(null, "escuro"));
        assertEquals("auto", TemaAplicacao.escolhaInicial(null, null));
        assertEquals("auto", TemaAplicacao.escolhaInicial("auto", "claro"));
    }
    @Test void savingThemePreservesAllOtherConfigBytes() throws Exception {
        var file = temporary.resolve("config.chc");
        String original = "#Config\r\n#data preservada\r\ncfg.zoom=100\r\ncfg.autor=Jos\\u00e9\r\n";
        Files.writeString(file, original, StandardCharsets.ISO_8859_1);
        ConfiguracaoTema.salvar(file, "escuro");
        assertEquals(original + TemaAplicacao.CHAVE + "=escuro\r\n", Files.readString(file, StandardCharsets.ISO_8859_1));
        ConfiguracaoTema.salvar(file, "claro");
        assertEquals(original + TemaAplicacao.CHAVE + "=claro\r\n", Files.readString(file, StandardCharsets.ISO_8859_1));
    }
    @Test void idsAliasesAndPreferredHalvesAreStable() {
        assertEquals(18, CatalogoTemas.TODOS.size());
        assertEquals(18, CatalogoTemas.TODOS.stream().map(CatalogoTemas.Tema::id).distinct().count());
        for (var tema : CatalogoTemas.TODOS) {
            assertEquals(tema.id(), TemaAplicacao.normalizar(tema.id().toUpperCase(java.util.Locale.ROOT)));
            assertEquals(tema.escuro(), tema.fabrica().get().isDark());
            assertEquals(tema, CatalogoTemas.resolver(tema.id(), "claro", "escuro", !tema.escuro()));
        }
        assertEquals("auto", TemaAplicacao.normalizar("sistema"));
        assertEquals("auto", TemaAplicacao.normalizar("unknown"));
        assertEquals("auto", TemaAplicacao.normalizar(null));
        assertEquals("claro", TemaAplicacao.normalizar("light"));
        assertEquals("escuro", TemaAplicacao.normalizar("dark"));
        assertEquals("nord", TemaAplicacao.escolhaInicial("nord", "macos-claro"));
        assertEquals("macos-claro", TemaAplicacao.escolhaInicial(null, "macos-claro"));
        assertEquals("auto", TemaAplicacao.escolhaInicial("sistema", "nord"));
        assertEquals("claro", CatalogoTemas.preferido("nord", false));
        assertEquals("escuro", CatalogoTemas.preferido("arc", true));
        assertEquals("claro", CatalogoTemas.preferido("removed-theme", false));
        assertEquals("arc", CatalogoTemas.resolver("auto", "arc", "nord", false).id());
        assertEquals("nord", CatalogoTemas.resolver("auto", "arc", "nord", true).id());
    }
    @Test void preferencesPreserveOtherBytesAndEachOther() throws Exception {
        var file = temporary.resolve("preferred.chc");
        String original = "# keep date\r\ncfg.author=Jos\\u00e9\r\ncfg.zoom : 90\r\n";
        Files.writeString(file, original, StandardCharsets.ISO_8859_1);
        ConfiguracaoTema.salvar(file, TemaAplicacao.CHAVE, "auto");
        ConfiguracaoTema.salvar(file, TemaAplicacao.CLARO, "arc");
        ConfiguracaoTema.salvar(file, TemaAplicacao.ESCURO, "nord");
        ConfiguracaoTema.salvar(file, TemaAplicacao.CLARO, "macos-claro");
        assertEquals(original + TemaAplicacao.CHAVE + "=auto\r\n" + TemaAplicacao.CLARO
                + "=macos-claro\r\n" + TemaAplicacao.ESCURO + "=nord\r\n",
                Files.readString(file, StandardCharsets.ISO_8859_1));
    }

}

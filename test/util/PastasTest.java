package util;

import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.junit.jupiter.api.Assertions.*;

class PastasTest {
    @TempDir Path temp;
    private final Map<String, String> previous = new HashMap<>();

    @BeforeEach void isolate() throws Exception {
        for (String key : List.of("user.home", "user.dir", "os.name")) previous.put(key, System.getProperty(key));
        System.setProperty("user.home", Files.createDirectories(temp.resolve("home")).toString());
        System.setProperty("user.dir", Files.createDirectories(temp.resolve("working")).toString());
        System.setProperty("os.name", "Linux");
    }

    @AfterEach void restore() {
        previous.forEach((key, value) -> { if (value == null) System.clearProperty(key); else System.setProperty(key, value); });
    }

    // Guards all XDG default paths and rejection of relative environment overrides.
    @ParameterizedTest @EnumSource(Pastas.Tipo.class)
    void defaultsAndRelativePaths(Pastas.Tipo type) {
        String relative = switch (type) { case CONFIG -> ".config"; case ESTADO -> ".local/state"; case DADOS -> ".local/share"; };
        Path expected = temp.resolve("home").resolve(relative).resolve("brmodelo-ng/example.chc");
        assertEquals(expected, Pastas.arquivo(type, "example.chc", key -> null).toPath());
        assertEquals(expected, Pastas.arquivo(type, "example.chc", key -> "relative").toPath());
    }

    // Guards separate absolute XDG config, state and data directories.
    @ParameterizedTest @EnumSource(Pastas.Tipo.class)
    void absoluteOverrides(Pastas.Tipo type) {
        Map<String, String> env = Map.of("XDG_CONFIG_HOME", temp.resolve("config").toString(),
                "XDG_STATE_HOME", temp.resolve("state").toString(), "XDG_DATA_HOME", temp.resolve("data").toString());
        String directory = switch (type) { case CONFIG -> "config"; case ESTADO -> "state"; case DADOS -> "data"; };
        assertEquals(temp.resolve(directory + "/brmodelo-ng/example.chc"), Pastas.arquivo(type, "example.chc", env::get).toPath());
    }

    // Guards copy-on-first-use migration without overwriting current data or deleting legacy files.
    @Test void migrationPreservesBothCopies() throws Exception {
        Path legacy = temp.resolve("working/config.chc");
        Files.writeString(legacy, "invented legacy settings");
        Path migrated = Pastas.arquivo(Pastas.Tipo.CONFIG, "config.chc", key -> null).toPath();
        assertEquals("invented legacy settings", Files.readString(migrated));
        assertTrue(Files.exists(legacy));
        Files.writeString(migrated, "new settings");
        assertEquals("new settings", Files.readString(Pastas.arquivo(Pastas.Tipo.CONFIG, "config.chc", key -> null).toPath()));
        assertEquals("invented legacy settings", Files.readString(legacy));
    }

    // Guards fallback when the target directory cannot be created.
    @Test void unusableHomeKeepsLegacyPath() throws Exception {
        Path blocker = Files.writeString(temp.resolve("blocked"), "file, not a directory");
        System.setProperty("user.home", blocker.toString());
        assertEquals(temp.resolve("working/config.chc"), Pastas.arquivo(Pastas.Tipo.CONFIG, "config.chc", key -> null).toPath());
    }

    // Guards the separate Windows application-data layout and macOS application-support layout.
    @Test void otherPlatforms() {
        System.setProperty("os.name", "Windows 11");
        assertEquals(temp.resolve("home/AppData/Local/brModelo NG/config.chc"), Pastas.arquivo(Pastas.Tipo.CONFIG, "config.chc", key -> null).toPath());
        assertEquals(temp.resolve("local/brModelo NG/config.chc"),
                Pastas.arquivo(Pastas.Tipo.CONFIG, "config.chc", key -> temp.resolve("local").toString()).toPath());
        System.setProperty("os.name", "Mac OS X");
        assertEquals(temp.resolve("home/Library/Application Support/brModelo NG/config.chc"),
                Pastas.arquivo(Pastas.Tipo.CONFIG, "config.chc", key -> null).toPath());
    }
}

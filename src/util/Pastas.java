package util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Locale;

/**
 * Where brModelo keeps its own files (settings, autosave, ready-made parts template).
 *
 * Legacy files live in the working directory. brModelo NG keeps its own, under its
 * own name (the official brModelo never uses these folders):
 *  - Linux and other Unix (XDG Base Directory): settings in $XDG_CONFIG_HOME/brmodelo-ng
 *    (~/.config), autosave in $XDG_STATE_HOME/brmodelo-ng (~/.local/state), the parts template
 *    in $XDG_DATA_HOME/brmodelo-ng (~/.local/share);
 *  - Windows: %LOCALAPPDATA%/brModelo NG;
 *  - macOS: ~/Library/Application Support/brModelo NG.
 * A file still found in the working directory is copied over the first time (never moved or
 * deleted), so existing settings and templates carry on.
 */
public final class Pastas {

    public enum Tipo {
        /** Settings (config.chc). */
        CONFIG("XDG_CONFIG_HOME", ".config"),
        /** Recovery state (autosave.chc). */
        ESTADO("XDG_STATE_HOME", ".local/state"),
        /** User data (Template.brMt). */
        DADOS("XDG_DATA_HOME", ".local/share");

        private final String variavel;
        private final String padrao;

        Tipo(String variavel, String padrao) {
            this.variavel = variavel;
            this.padrao = padrao;
        }
    }

    private Pastas() {
    }

    /** The file to read and write for one of brModelo's own files. */
    public static File arquivo(Tipo tipo, String nome) {
        return arquivo(tipo, nome, System::getenv);
    }

    // Package-private seam lets tests supply XDG values without changing process environment.
    static File arquivo(Tipo tipo, String nome, java.util.function.Function<String, String> ambiente) {
        File legado = new File(System.getProperty("user.dir"), nome);
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        File pasta = os.contains("win")
                ? new File(windowsBase(ambiente), "brModelo NG")
                : os.contains("mac")
                ? new File(System.getProperty("user.home"), "Library/Application Support/brModelo NG")
                : new File(base(tipo, ambiente), "brmodelo-ng");
        if (!pasta.isDirectory() && !pasta.mkdirs()) {
            // Read-only home or similar: keep the old behavior rather than lose the file.
            return legado;
        }
        File arquivo = new File(pasta, nome);
        if (!arquivo.exists() && legado.isFile()) {
            try {
                Files.copy(legado.toPath(), arquivo.toPath());
            } catch (IOException e) {
                BrLogger.Logger("ERROR_COPY_LEGACY_FILE", e.getMessage());
                return legado;
            }
        }
        return arquivo;
    }

    private static File windowsBase(java.util.function.Function<String, String> ambiente) {
        String local = ambiente.apply("LOCALAPPDATA");
        return local != null && new File(local).isAbsolute()
                ? new File(local) : new File(System.getProperty("user.home"), "AppData/Local");
    }

    private static File base(Tipo tipo, java.util.function.Function<String, String> ambiente) {
        String valor = ambiente.apply(tipo.variavel);
        // The spec ignores relative paths in these variables.
        if (valor != null && new File(valor).isAbsolute()) {
            return new File(valor);
        }
        return new File(System.getProperty("user.home"), tipo.padrao);
    }
}

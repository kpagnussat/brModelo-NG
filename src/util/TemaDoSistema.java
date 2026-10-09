package util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Whether the desktop asks applications for a dark appearance. Swing has no API for this, so
 * each platform's own setting is read:
 *  - forced by -Dbrmodelo.tema=escuro|claro (legacy convenience API);
 *  - GTK_THEME=name:dark (or a theme name containing "dark");
 *  - KDE Plasma: the window background color of the active color scheme (kdeglobals);
 *  - GNOME and other GTK desktops: the "color-scheme" setting (prefer-dark), then the GTK
 *    theme name;
 *  - Windows: the "apps use light theme" registry value;
 *  - macOS: the global AppleInterfaceStyle default.
 * Anything unreadable counts as light, the classic look.
 */
public final class TemaDoSistema {

    private TemaDoSistema() {
    }

    public static boolean escuro() {
        String forcado = System.getProperty("brmodelo.tema", "").toLowerCase(Locale.ROOT);
        if (forcado.equals("escuro") || forcado.equals("dark")) {
            return true;
        }
        if (forcado.equals("claro") || forcado.equals("light")) {
            return false;
        }
        return escuroDetectado();
    }

    public static boolean escuroDetectado() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("win")) {
            String r = execute("reg", "query", "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize",
                    "/v", "AppsUseLightTheme");
            return r != null && r.contains("0x0");
        }
        if (os.contains("mac")) {
            String r = execute("defaults", "read", "-g", "AppleInterfaceStyle");
            return r != null && r.trim().equalsIgnoreCase("Dark");
        }
        return escuroLinux();
    }

    private static boolean escuroLinux() {
        String gtkTheme = System.getenv("GTK_THEME");
        if (gtkTheme != null && !gtkTheme.isEmpty()) {
            return gtkTheme.toLowerCase(Locale.ROOT).contains("dark");
        }
        String desktop = String.valueOf(System.getenv("XDG_CURRENT_DESKTOP")).toUpperCase(Locale.ROOT);
        if (desktop.contains("KDE")) {
            Boolean kde = escuroKde();
            if (kde != null) {
                return kde;
            }
        }
        String esquema = execute("gsettings", "get", "org.gnome.desktop.interface", "color-scheme");
        if (esquema != null && esquema.contains("prefer-dark")) {
            return true;
        }
        if (esquema != null && esquema.contains("prefer-light")) {
            return false;
        }
        // Older GNOME and other GTK desktops only tell through the theme name (Adwaita-dark...).
        String tema = execute("gsettings", "get", "org.gnome.desktop.interface", "gtk-theme");
        return tema != null && tema.toLowerCase(Locale.ROOT).contains("dark");
    }

    /** Plasma: luminance of [Colors:Window] BackgroundNormal; null when not configured. */
    private static Boolean escuroKde() {
        String base = System.getenv("XDG_CONFIG_HOME");
        File arq = new File(base != null && !base.isEmpty() ? base : System.getProperty("user.home") + "/.config",
                "kdeglobals");
        try {
            List<String> linhas = Files.readAllLines(arq.toPath(), StandardCharsets.UTF_8);
            boolean secao = false;
            for (String l : linhas) {
                String t = l.trim();
                if (t.startsWith("[")) {
                    secao = t.equals("[Colors:Window]");
                } else if (secao && t.startsWith("BackgroundNormal=")) {
                    String[] rgb = t.substring("BackgroundNormal=".length()).split(",");
                    double lum = (0.2126 * Integer.parseInt(rgb[0].trim()) + 0.7152 * Integer.parseInt(rgb[1].trim())
                            + 0.0722 * Integer.parseInt(rgb[2].trim())) / 255.0;
                    return lum < 0.5;
                }
            }
        } catch (IOException | RuntimeException e) {
            // No kdeglobals or an unexpected format: let the GTK settings decide.
        }
        return null;
    }

    /** Output of a short settings query, or null when the command is missing or fails. */
    private static String execute(String... cmd) {
        try {
            Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
            // These settings queries have tiny output; enforce the timeout before reading
            // so a stalled desktop service cannot hold the UI or the appearance worker.
            if (!p.waitFor(2, TimeUnit.SECONDS)) {
                p.destroyForcibly();
                return null;
            }
            StringBuilder sb = new StringBuilder();
            try (BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String l;
                while ((l = in.readLine()) != null) {
                    sb.append(l).append('\n');
                }
            }
            return p.exitValue() == 0 ? sb.toString() : null;
        } catch (IOException e) {
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }
}

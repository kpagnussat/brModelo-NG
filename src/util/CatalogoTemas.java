package util;

import com.formdev.flatlaf.*;
import com.formdev.flatlaf.themes.*;
import com.formdev.flatlaf.intellijthemes.*;
import com.formdev.flatlaf.intellijthemes.materialthemeuilite.*;
import java.util.List;
import java.util.function.Supplier;

/** Stable configuration IDs; factories never install a LAF while rendering previews. */
public final class CatalogoTemas {
    public record Tema(String id, String nome, boolean escuro, Supplier<FlatLaf> fabrica) {
        @Override public String toString() { return nome; }
    }
    public static final List<Tema> TODOS = List.of(
        new Tema("claro", "FlatLaf Light", false, FlatLightLaf::new),
        new Tema("macos-claro", "macOS Light", false, FlatMacLightLaf::new),
        new Tema("intellij", "IntelliJ", false, FlatIntelliJLaf::new),
        new Tema("arc", "Arc", false, FlatArcIJTheme::new),
        new Tema("solarized-claro", "Solarized Light", false, FlatSolarizedLightIJTheme::new),
        new Tema("github-claro", "GitHub Light", false, FlatMTGitHubIJTheme::new),
        new Tema("material-claro", "Material Lighter", false, FlatMTMaterialLighterIJTheme::new),
        new Tema("escuro", "FlatLaf Dark", true, FlatDarkLaf::new),
        new Tema("macos-escuro", "macOS Dark", true, FlatMacDarkLaf::new),
        new Tema("darcula", "Darcula", true, FlatDarculaLaf::new),
        new Tema("arc-escuro", "Arc Dark", true, FlatArcDarkIJTheme::new),
        new Tema("one-dark", "One Dark", true, FlatOneDarkIJTheme::new),
        new Tema("nord", "Nord", true, FlatNordIJTheme::new),
        new Tema("dracula", "Dracula", true, FlatDraculaIJTheme::new),
        new Tema("solarized-escuro", "Solarized Dark", true, FlatSolarizedDarkIJTheme::new),
        new Tema("github-escuro", "GitHub Dark", true, FlatMTGitHubDarkIJTheme::new),
        new Tema("material-escuro", "Material Darker", true, FlatMTMaterialDarkerIJTheme::new),
        new Tema("gruvbox-escuro", "Gruvbox Dark", true, FlatGruvboxDarkHardIJTheme::new)
    );
    private CatalogoTemas() {}
    public static Tema buscar(String id) {
        return TODOS.stream().filter(t -> t.id().equals(id)).findFirst().orElse(null);
    }
    public static List<Tema> metade(boolean escuro) {
        return TODOS.stream().filter(t -> t.escuro() == escuro).toList();
    }
    public static String preferido(String id, boolean escuro) {
        Tema tema = buscar(id);
        return tema != null && tema.escuro() == escuro ? id : escuro ? "escuro" : "claro";
    }
    public static Tema resolver(String escolha, String claro, String escuro, boolean sistemaEscuro) {
        return buscar(escolha.equals("auto") ? preferido(sistemaEscuro ? escuro : claro, sistemaEscuro) : escolha);
    }
}

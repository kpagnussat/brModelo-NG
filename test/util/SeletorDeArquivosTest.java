package util;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.freedesktop.dbus.types.Variant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class SeletorDeArquivosTest {
    @TempDir Path pasta;

    private SeletorDeArquivos.Pedido pedido(boolean salvar) {
        return new SeletorDeArquivos.Pedido(salvar, "Diagrama", salvar ? "Salvar" : "Abrir",
                pasta.toFile(), "Escola Aurora", Dialogos.filtrosDiagrama(!salvar), 0);
    }

    @Test void preservesEveryDiagramAndImageFilter() {
        assertEquals(List.of("brM3", "brMj", "xml"), Dialogos.filtrosDiagrama(false).stream()
                .flatMap(f -> f.extensoes().stream()).toList());
        var open = Dialogos.filtrosDiagrama(true);
        assertEquals(List.of("brM3", "brMj", "xml"), open.getFirst().extensoes());
        assertTrue(open.getLast().extensoes().isEmpty());
        assertEquals(List.of("png", "bmp"), Dialogos.filtrosImagem().stream()
                .flatMap(f -> f.extensoes().stream()).toList());
    }

    @Test void portalOptionsHaveExactWireSignaturesAndNulTerminatedFolder() {
        var request = pedido(true);
        var options = PortalDeArquivos.opcoes(request, "token_1");
        assertEquals("token_1", options.get("handle_token").getValue());
        assertEquals(true, options.get("modal").getValue());
        assertEquals("Salvar", options.get("accept_label").getValue());
        assertEquals("a(sa(us))", options.get("filters").getSig());
        assertEquals("(sa(us))", options.get("current_filter").getSig());
        assertEquals("ay", options.get("current_folder").getSig());
        byte[] bytes = (byte[]) options.get("current_folder").getValue();
        assertEquals(0, bytes[bytes.length - 1]);
        assertEquals(pasta.toString(), new String(bytes, 0, bytes.length - 1, StandardCharsets.UTF_8));
        assertEquals(request.nome(), options.get("current_name").getValue());
        assertFalse(options.containsKey("multiple"));
        var open = PortalDeArquivos.opcoes(pedido(false), "open");
        assertEquals(false, open.get("multiple").getValue());
        assertFalse(open.containsKey("current_name"));
        var filter = (PortalDeArquivos.Filter) options.get("current_filter").getValue();
        assertEquals("BrModelo(bin)", filter.name);
        assertEquals(0, filter.patterns.getFirst().kind.intValue());
        assertEquals("*.[bB][rR][mM]3", filter.patterns.getFirst().pattern);
        assertEquals("*.[bB][rR][mM][jJ]", PortalDeArquivos.glob("brMj"));
        assertEquals("*", PortalDeArquivos.filtro(new SeletorDeArquivos.Filtro("Todos")).patterns.getFirst().pattern);
        assertEquals("/org/freedesktop/portal/desktop/request/1_42/token_1", PortalDeArquivos.caminhoRequest(":1.42", "token_1"));
    }

    @Test void fileUrisRoundTripSpacesUnicodePercentAndPlus() {
        for (String name : List.of("Aula 1.brM3", "Diagrama São José 日本.brMj", "100% + teste.png")) {
            File file = pasta.resolve(name).toFile();
            assertEquals(file, SeletorDeArquivos.deUri(file.toURI().toASCIIString()));
        }
        for (String uri : List.of("https://example.org/a", "file://remote/a", "file:///a?query", "not a uri")) {
            assertThrows(IllegalArgumentException.class, () -> SeletorDeArquivos.deUri(uri));
        }
    }

    @Test void extensionsFollowSelectedFormatEvenWhenAnotherSuffixWasTyped() {
        for (var filter : Dialogos.filtrosDiagrama(false)) {
            String ext = filter.extensoes().getFirst();
            File plain = pasta.resolve("modelo").toFile();
            assertEquals(pasta.resolve("modelo." + ext).toFile(), SeletorDeArquivos.comExtensao(plain, filter));
            File uppercase = pasta.resolve("modelo." + ext.toUpperCase(java.util.Locale.ROOT)).toFile();
            assertEquals(uppercase, SeletorDeArquivos.comExtensao(uppercase, filter));
            assertEquals(pasta.resolve("modelo.txt." + ext).toFile(),
                    SeletorDeArquivos.comExtensao(pasta.resolve("modelo.txt").toFile(), filter));
        }
        for (var filter : Dialogos.filtrosImagem()) {
            assertEquals(pasta.resolve("modelo." + filter.extensoes().getFirst()).toFile(),
                    SeletorDeArquivos.comExtensao(pasta.resolve("modelo").toFile(), filter));
        }
        File sql = pasta.resolve("query.sql").toFile();
        assertEquals(sql, SeletorDeArquivos.comExtensao(sql, new SeletorDeArquivos.Filtro("Todos")));
    }

    @Test void systemDialogListsTheRequestFiltersInOrder() {
        var abrir = new SeletorDeArquivos.Pedido(false, "Abrir diagrama", "Abrir", null, "",
                Dialogos.filtrosDiagrama(true), 0);
        var dialogo = new com.formdev.flatlaf.util.SystemFileChooser();
        var filtros = SeletorDeArquivos.configurar(dialogo, abrir);
        assertEquals(List.of(filtros.toArray()), List.of(dialogo.getChoosableFileFilters()));
        assertEquals(List.of("brM3", "brMj", "xml"),
                List.of(((com.formdev.flatlaf.util.SystemFileChooser.FileNameExtensionFilter) filtros.getFirst()).getExtensions()));
        assertSame(dialogo.getAcceptAllFileFilter(), filtros.getLast(), "\"All files\" keeps its place");
        assertSame(filtros.getFirst(), dialogo.getFileFilter());
        assertEquals(com.formdev.flatlaf.util.SystemFileChooser.OPEN_DIALOG, dialogo.getDialogType());

        var salvar = new SeletorDeArquivos.Pedido(true, "Salvar", "Salvar", new File("pasta"), "conceitual",
                Dialogos.filtrosDiagrama(false), 1);
        dialogo = new com.formdev.flatlaf.util.SystemFileChooser();
        filtros = SeletorDeArquivos.configurar(dialogo, salvar);
        assertSame(filtros.get(1), dialogo.getFileFilter());
        assertEquals(new File("pasta", "conceitual"), dialogo.getSelectedFile());
        assertEquals("brMj", dialogo.getPlatformProperty(com.formdev.flatlaf.util.SystemFileChooser.WINDOWS_DEFAULT_EXTENSION));
    }

    @Test void backendOrderAndStrictOverrides() {
        for (String os : List.of("Linux", "FreeBSD", "OpenBSD")) {
            assertEquals(List.of("portal", "nativo", "swing"), SeletorDeArquivos.ordem("", os, false));
        }
        for (String os : List.of("Windows 11", "Mac OS X")) {
            assertEquals(List.of("sistema", "nativo", "swing"), SeletorDeArquivos.ordem("auto", os, false));
        }
        for (String forced : List.of("portal", "sistema", "nativo", "swing")) {
            assertEquals(List.of(forced), SeletorDeArquivos.ordem(forced, "Linux", false));
        }
        assertEquals(List.of("swing"), SeletorDeArquivos.ordem("", "Linux", true));
        assertThrows(IllegalArgumentException.class, () -> SeletorDeArquivos.ordem("typo", "Linux", false));
    }

    @Test void errorAndAbsenceFallBackButCancellationDoesNot() {
        var order = List.of("portal", "nativo", "swing");
        List<String> calls = new ArrayList<>();
        SeletorDeArquivos.Backend nativeBackend = (parent, request) -> {
            calls.add("nativo"); return SeletorDeArquivos.Resposta.erro();
        };
        SeletorDeArquivos.Backend swing = (parent, request) -> {
            calls.add("swing"); return new SeletorDeArquivos.Resposta(0, pasta.resolve("a").toFile(), request.filtro());
        };
        for (boolean absent : List.of(false, true)) {
            calls.clear();
            SeletorDeArquivos.Backend portal = (parent, request) -> {
                calls.add("portal");
                if (absent) throw new IllegalStateException("No session bus/interface");
                return PortalDeArquivos.resposta(2, Map.of(), request);
            };
            assertEquals(0, SeletorDeArquivos.tentar(null, pedido(true), order, portal, null, nativeBackend, swing).estado());
            assertEquals(order, calls);
        }
        calls.clear();
        assertEquals(1, SeletorDeArquivos.tentar(null, pedido(true), order,
                (parent, request) -> PortalDeArquivos.resposta(1, Map.of(), request), null, nativeBackend, swing).estado());
        assertTrue(calls.isEmpty());
        assertEquals(2, SeletorDeArquivos.tentar(null, pedido(true), List.of("portal"),
                (parent, request) -> SeletorDeArquivos.Resposta.erro(), null, nativeBackend, swing).estado());
    }

    @Test void portalResponseRetainsSelectedJsonFilterAndRejectsMalformedSuccess() {
        var request = pedido(true);
        var json = request.filtros().get(1);
        File file = pasta.resolve("Escola São José").toFile();
        Map<String, Variant<?>> response = Map.of("uris", new Variant<>(List.of(file.toURI().toASCIIString()), "as"),
                "current_filter", new Variant<>(new Object[]{json.nome(), new Object[0]}, "(sa(us))"));
        var chosen = PortalDeArquivos.resposta(0, response, request);
        assertEquals(0, chosen.estado());
        assertEquals(file, chosen.arquivo());
        assertEquals(json, chosen.filtro());
        assertEquals(pasta.resolve("Escola São José.brMj").toFile(), SeletorDeArquivos.comExtensao(chosen.arquivo(), chosen.filtro()));
        assertEquals(2, PortalDeArquivos.resposta(0, Map.of(), request).estado());
        assertEquals(2, PortalDeArquivos.resposta(0, Map.of("uris", new Variant<>(List.of("https://example.org/a"), "as")), request).estado());
        assertEquals(2, PortalDeArquivos.resposta(99, Map.of(), request).estado());
    }
}

package util;

import java.awt.Component;
import java.awt.Dialog;
import java.awt.FileDialog;
import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import com.formdev.flatlaf.util.SystemFileChooser;

/** One file-selection contract shared by the portal, AWT and Swing backends. */
public final class SeletorDeArquivos {
    private SeletorDeArquivos() {}

    public record Filtro(String nome, List<String> extensoes) {
        public Filtro { extensoes = List.copyOf(extensoes); }
        public Filtro(String nome, String... extensoes) { this(nome, List.of(extensoes)); }
        @Override public String toString() { return nome; }
        boolean aceita(File file) {
            return extensoes.isEmpty() || extensoes.stream()
                    .anyMatch(ext -> ext.equalsIgnoreCase(Arquivo.getExtension(file)));
        }
    }

    public record Pedido(boolean salvar, String titulo, String aceitar, File pasta,
                         String nome, List<Filtro> filtros, int filtroInicial) {
        public Pedido {
            filtros = List.copyOf(filtros);
            if (filtros.isEmpty() || filtroInicial < 0 || filtroInicial >= filtros.size()) {
                throw new IllegalArgumentException("A chooser needs a valid initial filter");
            }
        }
        Filtro filtro() { return filtros.get(filtroInicial); }
    }

    // Cancellation is terminal; only an unavailable or failed backend permits fallback.
    static final int OK = 0, CANCELADO = 1, ERRO = 2;
    // sobrescritaConfirmada: the backend's own save dialog already asked before replacing an
    // existing file (portal, GTK/Windows/macOS native dialogs do; JFileChooser does not).
    record Resposta(int estado, File arquivo, Filtro filtro, boolean sobrescritaConfirmada) {
        Resposta(int estado, File arquivo, Filtro filtro) { this(estado, arquivo, filtro, true); }
        static Resposta cancelar() { return new Resposta(CANCELADO, null, null); }
        static Resposta erro() { return new Resposta(ERRO, null, null); }
    }
    @FunctionalInterface interface Backend {
        Resposta escolher(Component pai, Pedido pedido) throws Exception;
    }

    static List<String> ordem(String forcar, String os, boolean headless) {
        if (headless) return List.of("swing");
        return switch (forcar.toLowerCase(Locale.ROOT)) {
            case "portal" -> List.of("portal");
            case "sistema" -> List.of("sistema");
            case "nativo" -> List.of("nativo");
            case "swing" -> List.of("swing");
            case "", "auto" -> {
                String plataforma = os.toLowerCase(Locale.ROOT);
                yield plataforma.contains("linux") || plataforma.contains("bsd")
                        ? List.of("portal", "nativo", "swing") : List.of("sistema", "nativo", "swing");
            }
            default -> throw new IllegalArgumentException("Unknown brmodelo.seletor: " + forcar);
        };
    }

    static Resposta tentar(Component pai, Pedido pedido, List<String> ordem,
                           Backend portal, Backend sistema, Backend nativo, Backend swing) {
        for (String nome : ordem) {
            Backend backend = switch (nome) {
                case "portal" -> portal;
                case "sistema" -> sistema;
                case "nativo" -> nativo;
                case "swing" -> swing;
                default -> throw new IllegalArgumentException(nome);
            };
            try {
                Resposta resposta = backend.escolher(pai, pedido);
                if (resposta.estado() != ERRO) return resposta;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return Resposta.cancelar();
            } catch (Exception | LinkageError e) {
                System.getLogger(SeletorDeArquivos.class.getName()).log(System.Logger.Level.DEBUG,
                        "File chooser backend failed: " + nome, e);
            }
        }
        return Resposta.erro();
    }

    public static File escolher(Component pai, Pedido pedido) {
        if (!SwingUtilities.isEventDispatchThread() && !GraphicsEnvironment.isHeadless()) {
            AtomicReference<File> file = new AtomicReference<>();
            try {
                SwingUtilities.invokeAndWait(() -> file.set(escolher(pai, pedido)));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (java.lang.reflect.InvocationTargetException e) {
                throw new IllegalStateException(e.getCause());
            }
            return file.get();
        }
        Resposta resposta = tentar(pai, pedido,
                ordem(System.getProperty("brmodelo.seletor", ""), System.getProperty("os.name", ""),
                        GraphicsEnvironment.isHeadless()),
                (parent, request) -> esperarPortal(parent, request, new PortalDeArquivos()),
                SeletorDeArquivos::sistema, SeletorDeArquivos::nativo, SeletorDeArquivos::swing);
        if (resposta.estado() == ERRO) {
            System.getLogger(SeletorDeArquivos.class.getName()).log(System.Logger.Level.WARNING,
                    "No available file chooser; brmodelo.seletor=" + System.getProperty("brmodelo.seletor", "auto"));
        }
        if (resposta.estado() != OK) return null;
        File file = pedido.salvar() ? comExtensao(resposta.arquivo(), resposta.filtro()) : resposta.arquivo();
        if (!pedido.salvar() && !file.isFile()) return null;
        // Confirm the final filename before any caller writes it, unless the backend's dialog
        // already confirmed this exact file (asking twice on GNOME/KDE/Windows/macOS). An
        // appended extension names a file the backend never saw, so that case always asks.
        boolean jaConfirmado = resposta.sobrescritaConfirmada() && file.equals(resposta.arquivo());
        if (pedido.salvar() && !jaConfirmado && file.exists() && JOptionPane.showConfirmDialog(pai,
                file.getName() + ": o arquivo já existe. Deseja sobrescrevê-lo?", pedido.titulo(),
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) != JOptionPane.YES_OPTION) return null;
        return file;
    }

    static File comExtensao(File file, Filtro filtro) {
        if (filtro == null || filtro.extensoes().isEmpty() || filtro.aceita(file)) return file;
        return new File(file.getAbsolutePath() + "." + filtro.extensoes().getFirst());
    }

    static File deUri(String uri) {
        URI parsed = URI.create(uri);
        if (!"file".equalsIgnoreCase(parsed.getScheme())) throw new IllegalArgumentException("Not a local file URI");
        return new File(parsed);
    }

    static File pastaDe(File file) {
        return file.isDirectory() ? file.getAbsoluteFile() : file.getAbsoluteFile().getParentFile();
    }

    static Resposta esperarPortal(Component pai, Pedido pedido, Backend portal) {
        Window owner = pai instanceof Window window ? window : pai == null ? null : SwingUtilities.getWindowAncestor(pai);
        JDialog espera = new JDialog(owner, pedido.titulo(), Dialog.ModalityType.APPLICATION_MODAL);
        espera.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        JPanel painel = new JPanel();
        painel.add(new JLabel("Aguardando seleção de arquivo…"));
        JButton cancelar = new JButton("Cancelar");
        painel.add(cancelar);
        espera.setContentPane(painel);
        util.AcabamentoDialogos.aplicar(espera);
        espera.setLocationRelativeTo(pai);
        AtomicReference<Resposta> resposta = new AtomicReference<>(Resposta.cancelar());
        AtomicBoolean cancelado = new AtomicBoolean();
        Thread worker = Thread.ofPlatform().daemon().name("brmodelo-file-portal").unstarted(() -> {
            try {
                if (!cancelado.get()) resposta.set(portal.escolher(pai, pedido));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception | LinkageError e) {
                resposta.set(Resposta.erro());
                System.getLogger(SeletorDeArquivos.class.getName()).log(System.Logger.Level.DEBUG,
                        "Desktop portal unavailable", e);
            } finally {
                SwingUtilities.invokeLater(espera::dispose);
            }
        });
        Runnable fechar = () -> { cancelado.set(true); worker.interrupt(); espera.dispose(); };
        cancelar.addActionListener(event -> fechar.run());
        espera.addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent event) { fechar.run(); }
        });
        // JDialog's modal secondary event loop blocks app input but keeps the EDT painting.
        // Start from that loop, so even an immediate response cannot dispose before setVisible.
        SwingUtilities.invokeLater(worker::start);
        try {
            espera.setVisible(true);
            return cancelado.get() ? Resposta.cancelar() : resposta.get();
        } finally {
            worker.interrupt();
            espera.dispose();
        }
    }

    /**
     * The operating system's own dialog through FlatLaf: the current Windows 10+ dialog and the
     * macOS panel, both with a working file type list. AWT's FileDialog on Windows is the legacy
     * dialog and ignores FilenameFilter. Without FlatLaf's native library this falls back to Swing,
     * which also asks before overwriting.
     */
    static Resposta sistema(Component pai, Pedido pedido) {
        if (GraphicsEnvironment.isHeadless()) return Resposta.cancelar();
        SystemFileChooser dialogo = new SystemFileChooser(pedido.pasta());
        List<SystemFileChooser.FileFilter> filtros = configurar(dialogo, pedido);
        if (dialogo.showDialog(pai, pedido.aceitar()) != SystemFileChooser.APPROVE_OPTION
                || dialogo.getSelectedFile() == null) return Resposta.cancelar();
        int indice = filtros.indexOf(dialogo.getFileFilter());
        return new Resposta(OK, dialogo.getSelectedFile(), pedido.filtros().get(indice < 0 ? pedido.filtroInicial() : indice));
    }

    static List<SystemFileChooser.FileFilter> configurar(SystemFileChooser dialogo, Pedido pedido) {
        dialogo.setDialogType(pedido.salvar() ? SystemFileChooser.SAVE_DIALOG : SystemFileChooser.OPEN_DIALOG);
        dialogo.setFileSelectionMode(SystemFileChooser.FILES_ONLY);
        dialogo.setDialogTitle(pedido.titulo());
        // Keep the request's order, "All files" included, instead of FlatLaf's accept-all-last default.
        dialogo.setAcceptAllFileFilterUsed(false);
        List<SystemFileChooser.FileFilter> filtros = new ArrayList<>();
        for (Filtro filtro : pedido.filtros()) {
            var filtroSistema = filtro.extensoes().isEmpty() ? dialogo.getAcceptAllFileFilter()
                    : new SystemFileChooser.FileNameExtensionFilter(filtro.nome(), filtro.extensoes().toArray(String[]::new));
            filtros.add(filtroSistema);
            dialogo.addChoosableFileFilter(filtroSistema);
        }
        dialogo.setFileFilter(filtros.get(pedido.filtroInicial()));
        if (pedido.salvar()) {
            if (!pedido.nome().isEmpty()) dialogo.setSelectedFile(new File(pedido.pasta(), pedido.nome()));
            // Windows then appends the chosen type's extension before its own overwrite question.
            if (!pedido.filtro().extensoes().isEmpty()) {
                dialogo.putPlatformProperty(SystemFileChooser.WINDOWS_DEFAULT_EXTENSION, pedido.filtro().extensoes().getFirst());
            }
        }
        return filtros;
    }

    static Resposta nativo(Component pai, Pedido pedido) {
        Filtro filtro = pedido.filtro();
        if (pedido.salvar() && pedido.filtros().size() > 1) {
            // AWT has no portable filter selector; a save picks its format before the native dialog.
            // Opening needs no question: the initial filter already accepts every supported type.
            Object escolhido = JOptionPane.showInputDialog(pai, pedido.salvar() ? "Formato do arquivo:" : "Tipo de arquivo:", pedido.titulo(),
                    JOptionPane.QUESTION_MESSAGE, null, pedido.filtros().toArray(), filtro);
            if (escolhido == null) return Resposta.cancelar();
            filtro = (Filtro) escolhido;
        }
        Window owner = pai instanceof Window window ? window : pai == null ? null : SwingUtilities.getWindowAncestor(pai);
        FileDialog dialogo = owner instanceof Dialog dialog
                ? new FileDialog(dialog, pedido.titulo(), pedido.salvar() ? FileDialog.SAVE : FileDialog.LOAD)
                : new FileDialog(owner instanceof java.awt.Frame frame ? frame : null,
                        pedido.titulo(), pedido.salvar() ? FileDialog.SAVE : FileDialog.LOAD);
        try {
            Filtro ativo = filtro;
            dialogo.setFilenameFilter((dir, name) -> ativo.aceita(new File(dir, name)));
            if (pedido.pasta() != null) dialogo.setDirectory(pedido.pasta().getAbsolutePath());
            if (pedido.salvar()) dialogo.setFile(pedido.nome());
            dialogo.setMultipleMode(false);
            dialogo.setVisible(true);
            if (dialogo.getFile() == null) return Resposta.cancelar();
            return new Resposta(OK, new File(dialogo.getDirectory(), dialogo.getFile()), filtro);
        } finally { dialogo.dispose(); }
    }

    static Resposta swing(Component pai, Pedido pedido) {
        if (GraphicsEnvironment.isHeadless()) return Resposta.cancelar();
        JFileChooser dialogo = new JFileChooser(pedido.pasta());
        dialogo.setFileSelectionMode(JFileChooser.FILES_ONLY);
        dialogo.setDialogTitle(pedido.titulo());
        dialogo.setAcceptAllFileFilterUsed(pedido.filtros().stream().anyMatch(f -> f.extensoes().isEmpty()));
        List<javax.swing.filechooser.FileFilter> filtros = new ArrayList<>();
        for (Filtro filtro : pedido.filtros()) {
            var swingFilter = filtro.extensoes().isEmpty() ? dialogo.getAcceptAllFileFilter()
                    : new FileNameExtensionFilter(filtro.nome(), filtro.extensoes().toArray(String[]::new));
            filtros.add(swingFilter);
            if (!filtro.extensoes().isEmpty()) dialogo.addChoosableFileFilter(swingFilter);
        }
        dialogo.setFileFilter(filtros.get(pedido.filtroInicial()));
        if (pedido.salvar() && !pedido.nome().isEmpty()) dialogo.setSelectedFile(new File(pedido.pasta(), pedido.nome()));
        dialogo.setDialogType(pedido.salvar() ? JFileChooser.SAVE_DIALOG : JFileChooser.OPEN_DIALOG);
        if (dialogo.showDialog(pai, pedido.aceitar()) != JFileChooser.APPROVE_OPTION) return Resposta.cancelar();
        return new Resposta(OK, dialogo.getSelectedFile(), pedido.filtros().get(filtros.indexOf(dialogo.getFileFilter())), false);
    }
}

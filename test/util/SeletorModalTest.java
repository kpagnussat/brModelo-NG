package util;

import java.awt.Dialog;
import java.awt.Window;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import static org.junit.jupiter.api.Assertions.*;

@Tag("gui")
class SeletorModalTest {
    @Test @Timeout(15) void portalWaitKeepsEdtAliveAndBlocksAppInput() throws Exception {
        AtomicBoolean offEdt = new AtomicBoolean();
        AtomicBoolean modal = new AtomicBoolean();
        CountDownLatch tick = new CountDownLatch(1);
        SwingUtilities.invokeAndWait(() -> {
            Timer timer = new Timer(20, event -> {
                for (Window window : Window.getWindows()) {
                    if (window instanceof Dialog dialog && dialog.isVisible() && dialog.isModal()) modal.set(true);
                }
                tick.countDown();
            });
            timer.start();
            try {
                var pedido = new SeletorDeArquivos.Pedido(false, "Teste", "Abrir", null, "",
                        List.of(new SeletorDeArquivos.Filtro("Todos")), 0);
                var result = SeletorDeArquivos.esperarPortal(null, pedido, (parent, request) -> {
                    offEdt.set(!SwingUtilities.isEventDispatchThread());
                    if (!tick.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("EDT blocked");
                    return SeletorDeArquivos.Resposta.cancelar();
                });
                assertEquals(1, result.estado());
            } finally { timer.stop(); }
        });
        assertTrue(offEdt.get());
        assertTrue(modal.get());
    }

    @Test @Timeout(15) void awtNativeDialogOpensAndCancels() throws Exception {
        AtomicBoolean opened = new AtomicBoolean();
        SwingUtilities.invokeAndWait(() -> {
            Timer cancel = new Timer(50, event -> {
                for (Window window : Window.getWindows()) {
                    if (window instanceof java.awt.FileDialog dialog && dialog.isVisible()) {
                        opened.set(true);
                        dialog.setVisible(false);
                    }
                }
            });
            cancel.start();
            try {
                var pedido = new SeletorDeArquivos.Pedido(false, "Teste nativo", "Abrir", null, "",
                        List.of(new SeletorDeArquivos.Filtro("Todos")), 0);
                assertEquals(1, SeletorDeArquivos.nativo(null, pedido).estado());
            } finally { cancel.stop(); }
        });
        assertTrue(opened.get(), "The actual AWT dialog must have been shown");
    }

    @Test @Timeout(15) void awtOpenWithSeveralFiltersGoesStraightToTheNativeDialog() throws Exception {
        AtomicBoolean opened = new AtomicBoolean();
        SwingUtilities.invokeAndWait(() -> {
            // Only the FileDialog is dismissed: a filter question before it would hang until the timeout.
            Timer cancel = new Timer(50, event -> {
                for (Window window : Window.getWindows()) {
                    if (window instanceof java.awt.FileDialog dialog && dialog.isVisible()) {
                        opened.set(true);
                        dialog.setVisible(false);
                    }
                }
            });
            cancel.start();
            try {
                var pedido = new SeletorDeArquivos.Pedido(false, "Teste nativo", "Abrir", null, "",
                        Dialogos.filtrosDiagrama(true), 0);
                assertEquals(1, SeletorDeArquivos.nativo(null, pedido).estado());
            } finally { cancel.stop(); }
        });
        assertTrue(opened.get(), "The actual AWT dialog must have been shown");
    }

    @Test @Timeout(15) void swingReturnsSelectedJsonFilter() throws Exception {
        AtomicBoolean opened = new AtomicBoolean();
        SwingUtilities.invokeAndWait(() -> {
            Timer approve = new Timer(50, event -> {
                for (Window window : Window.getWindows()) {
                    if (window instanceof javax.swing.JDialog dialog && dialog.isVisible()) {
                        javax.swing.JFileChooser chooser = findChooser(dialog);
                        if (chooser == null) continue;
                        opened.set(true);
                        for (var filter : chooser.getChoosableFileFilters()) {
                            if (filter.getDescription().contains("JSON")) chooser.setFileFilter(filter);
                        }
                        chooser.setSelectedFile(new java.io.File("Escola São José"));
                        chooser.approveSelection();
                    }
                }
            });
            approve.start();
            try {
                var pedido = new SeletorDeArquivos.Pedido(true, "Teste Swing", "Salvar", null, "",
                        Dialogos.filtrosDiagrama(false), 0);
                var resposta = SeletorDeArquivos.swing(null, pedido);
                assertEquals(0, resposta.estado());
                assertEquals("brMj", resposta.filtro().extensoes().getFirst());
                assertEquals("Escola São José.brMj", SeletorDeArquivos.comExtensao(resposta.arquivo(), resposta.filtro()).getName());
            } finally { approve.stop(); }
        });
        assertTrue(opened.get());
    }

    @Test @Timeout(15) void localPortalCancellationInterruptsWorkerAndDisposesWait() throws Exception {
        CountDownLatch workerStarted = new CountDownLatch(1);
        CountDownLatch interrupted = new CountDownLatch(1);
        SwingUtilities.invokeAndWait(() -> {
            Timer cancel = new Timer(20, event -> {
                if (workerStarted.getCount() != 0) return;
                for (Window window : Window.getWindows()) {
                    if (window instanceof javax.swing.JDialog dialog && dialog.isVisible()
                            && dialog.getTitle().equals("Cancelar portal")) {
                        dialog.dispatchEvent(new java.awt.event.WindowEvent(dialog, java.awt.event.WindowEvent.WINDOW_CLOSING));
                    }
                }
            });
            cancel.start();
            try {
                var pedido = new SeletorDeArquivos.Pedido(false, "Cancelar portal", "Abrir", null, "",
                        List.of(new SeletorDeArquivos.Filtro("Todos")), 0);
                var resposta = SeletorDeArquivos.esperarPortal(null, pedido, (parent, request) -> {
                    workerStarted.countDown();
                    try { new CountDownLatch(1).await(); }
                    catch (InterruptedException e) { interrupted.countDown(); throw e; }
                    throw new AssertionError("Unreachable");
                });
                assertEquals(1, resposta.estado());
            } finally { cancel.stop(); }
        });
        assertTrue(interrupted.await(5, TimeUnit.SECONDS));
    }

    @Test @Timeout(15) void immediatePortalResponseCannotLeaveWaitOpen() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            var pedido = new SeletorDeArquivos.Pedido(false, "Resposta imediata", "Abrir", null, "",
                    List.of(new SeletorDeArquivos.Filtro("Todos")), 0);
            assertEquals(2, SeletorDeArquivos.esperarPortal(null, pedido,
                    (parent, request) -> SeletorDeArquivos.Resposta.erro()).estado());
        });
    }

    @Test @Timeout(15) void overwriteConfirmationUsesTheCompletedExtension(@org.junit.jupiter.api.io.TempDir java.nio.file.Path temp) throws Exception {
        java.nio.file.Path existing = temp.resolve("Escola São José.brMj");
        java.nio.file.Files.writeString(existing, "keep this content");
        AtomicBoolean confirmedFinalPath = new AtomicBoolean();
        String previous = System.getProperty("brmodelo.seletor");
        System.setProperty("brmodelo.seletor", "swing");
        try {
            SwingUtilities.invokeAndWait(() -> {
                Timer reject = new Timer(30, event -> {
                    for (Window window : Window.getWindows()) {
                        if (!(window instanceof javax.swing.JDialog dialog) || !dialog.isVisible()) continue;
                        var chooser = findChooser(dialog);
                        if (chooser != null) {
                            for (var filter : chooser.getChoosableFileFilters()) {
                                if (filter.getDescription().contains("JSON")) chooser.setFileFilter(filter);
                            }
                            chooser.setSelectedFile(temp.resolve("Escola São José").toFile());
                            chooser.approveSelection();
                            continue;
                        }
                        var pane = findOptionPane(dialog);
                        if (pane != null) {
                            confirmedFinalPath.set(pane.getMessage().toString().contains(existing.getFileName().toString()));
                            pane.setValue(javax.swing.JOptionPane.NO_OPTION);
                        }
                    }
                });
                reject.start();
                try {
                    var pedido = new SeletorDeArquivos.Pedido(true, "Sobrescrita", "Salvar", temp.toFile(), "",
                            Dialogos.filtrosDiagrama(false), 0);
                    assertNull(SeletorDeArquivos.escolher(null, pedido));
                } finally { reject.stop(); }
            });
        } finally {
            if (previous == null) System.clearProperty("brmodelo.seletor");
            else System.setProperty("brmodelo.seletor", previous);
        }
        assertTrue(confirmedFinalPath.get());
        assertEquals("keep this content", java.nio.file.Files.readString(existing));
    }

    private static javax.swing.JOptionPane findOptionPane(java.awt.Container parent) {
        for (var child : parent.getComponents()) {
            if (child instanceof javax.swing.JOptionPane pane) return pane;
            if (child instanceof java.awt.Container container) {
                var found = findOptionPane(container);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static javax.swing.JFileChooser findChooser(java.awt.Container parent) {
        for (var child : parent.getComponents()) {
            if (child instanceof javax.swing.JFileChooser chooser) return chooser;
            if (child instanceof java.awt.Container container) {
                var found = findChooser(container);
                if (found != null) return found;
            }
        }
        return null;
    }
}

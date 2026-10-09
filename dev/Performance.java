import controlador.Editor;
import java.awt.Graphics2D;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import javax.swing.SwingUtilities;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;

/** Read-only input benchmark. All model/UI operations run on the EDT, as in the app. */
public final class Performance {
    @Name("brmodelo.Measurement") @Label("brModelo measurement")
    static class Measurement extends Event {
        @Label("Phase") String phase;
        @Label("Input") String input;
    }

    private static void edt(Runnable action) throws Exception {
        SwingUtilities.invokeAndWait(action);
    }

    private static double timed(String phase, String input, Runnable action) {
        Measurement event = new Measurement();
        event.phase = phase;
        event.input = input;
        event.begin();
        long start = System.nanoTime();
        action.run();
        double ms = (System.nanoTime() - start) / 1e6;
        event.end();
        event.commit();
        return ms;
    }

    private static double median(double[] values) {
        Arrays.sort(values);
        return values[values.length / 2];
    }

    private static void invoke(Method method, Object target, Object... args) {
        try { method.invoke(target, args); }
        catch (Exception e) { throw new RuntimeException(e); }
    }

    // Barrier on the same single writer: total includes queueing and the completed file write.
    private static void awaitWriter(Editor editor) throws Exception {
        try {
            Field field = Editor.class.getDeclaredField("autoSaveWriter");
            field.setAccessible(true);
            ExecutorService writer = (ExecutorService) field.get(editor);
            if (writer != null) writer.submit(() -> {}).get(30, TimeUnit.SECONDS);
        } catch (NoSuchFieldException beforeFix) {
            // The baseline writes synchronously in DoAutoSave.
        }
    }

    public static void main(String[] args) {
        try {
            measure(args);
            System.exit(0);
        } catch (Throwable failure) {
            failure.printStackTrace();
            System.exit(1);
        }
    }

    private static void measure(String[] args) throws Exception {
        CountDownLatch shown = new CountDownLatch(1);
        double[] startup = new double[1];
        // Use the application's actual LAF selection and construction path.
        Method laf = principal.Aplicacao.class.getDeclaredMethod("initLookAndFeel");
        laf.setAccessible(true);
        invoke(laf, null);
        edt(() -> {
            principal.FramePrincipal frame = new principal.FramePrincipal();
            principal.Aplicacao.fmPrincipal = frame;
            frame.addComponentListener(new ComponentAdapter() {
                @Override public void componentShown(ComponentEvent event) {
                    startup[0] = ManagementFactory.getRuntimeMXBean().getUptime();
                    shown.countDown();
                }
            });
            frame.setVisible(true);
        });
        if (!shown.await(30, TimeUnit.SECONDS)) throw new IllegalStateException("Frame not shown");
        System.out.printf(java.util.Locale.ROOT, "startup_ms,%.3f%n", startup[0]);
        if (args.length == 0) System.exit(0);

        Editor editor = principal.Aplicacao.fmPrincipal.getEditor();
        edt(() -> editor.setAutoSaveInterval(0));
        Method open = Editor.class.getDeclaredMethod("AbrirDiagramaFromFile", File.class);
        open.setAccessible(true);
        Method save = Editor.class.getDeclaredMethod("DoAutoSave");
        save.setAccessible(true);
        Field done = Editor.class.getDeclaredField("doneAutoSave");
        done.setAccessible(true);
        System.out.println("input,bytes,open_ms,paint_ms,snapshot_edt_ms,autosave_total_ms");
        for (String name : args) {
            File file = new File(name);
            double[] opens = new double[5];
            for (int i = 0; i < opens.length; i++) {
                final int at = i;
                edt(() -> {
                    // Opening an already-open path shows a modal warning. Remove the old
                    // in-memory tabs before each trial; never save or rewrite the input.
                    editor.FecharTudo();
                    opens[at] = timed("open", name, () -> invoke(open, editor, file));
                    if (!file.getAbsolutePath().equals(editor.diagramaAtual.getArquivo()))
                        throw new IllegalStateException("Input did not load: " + file);
                    principal.Aplicacao.fmPrincipal.validate();
                });
            }
            double[] paints = new double[100];
            edt(() -> {
                var box = editor.getBox();
                BufferedImage image = new BufferedImage(box.getWidth(), box.getHeight(), BufferedImage.TYPE_INT_RGB);
                Graphics2D graphics = image.createGraphics();
                try {
                    for (int i = 0; i < 10; i++) box.paint(graphics);
                    for (int i = 0; i < paints.length; i++)
                        paints[i] = timed("paint", name, () -> box.paint(graphics));
                } finally { graphics.dispose(); }
            });
            double[] snapshots = new double[11], totals = new double[11];
            for (int i = 0; i < snapshots.length; i++) {
                final int at = i;
                long start = System.nanoTime();
                edt(() -> {
                    editor.diagramaAtual.setMudou(true);
                    try { done.setBoolean(editor, false); }
                    catch (Exception e) { throw new RuntimeException(e); }
                    snapshots[at] = timed("autosave", name, () -> invoke(save, editor));
                });
                awaitWriter(editor);
                totals[at] = (System.nanoTime() - start) / 1e6;
            }
            System.out.printf(java.util.Locale.ROOT, "%s,%d,%.3f,%.3f,%.3f,%.3f%n", name,
                    Files.size(file.toPath()), median(opens), median(paints), median(snapshots), median(totals));
            edt(() -> editor.diagramaAtual.setMudou(false));
        }
        edt(() -> {
            for (int i = 0; i < 1000; i++) util.Icones.de("/imagens/menu_salvar.png");
            double[] batches = new double[21];
            for (int i = 0; i < batches.length; i++) batches[i] = timed("icons", "menu_salvar", () -> {
                for (int n = 0; n < 10000; n++) util.Icones.de("/imagens/menu_salvar.png");
            });
            System.out.printf(java.util.Locale.ROOT, "icon_10000_ms,%.3f%n", median(batches));
        });
        // A short baseline check deliberately bypasses GC, which can mask abandoned Timers.
        long before = timerThreads();
        edt(() -> {
            for (int i = 0; i < 100; i++) editor.DoStatus("check " + i);
            for (int i = 0; i < 100; i++) editor.setAutoSaveInterval(i % 2 + 1);
            editor.setAutoSaveInterval(0);
        });
        System.out.printf("timer_threads,%d,%d%n", before, timerThreads());
        System.exit(0);
    }

    private static long timerThreads() {
        return Thread.getAllStackTraces().keySet().stream()
                .filter(t -> t.isAlive() && t.getName().startsWith("Timer-")).count();
    }

}

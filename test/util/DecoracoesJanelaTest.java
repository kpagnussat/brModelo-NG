package util;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.ui.FlatRootPaneUI;
import java.awt.*;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class DecoracoesJanelaTest {
    @Test void defaultFrameIsModernOnlyOnWindowsAndTheFlagOverridesIt() {
        assertTrue(DecoracoesJanela.modernas("", "Windows 11"));
        assertFalse(DecoracoesJanela.modernas("nativas", "Windows 11"));
        for (String os : new String[]{"Linux", "Mac OS X"}) {
            assertFalse(DecoracoesJanela.modernas("", os), os);
            assertTrue(DecoracoesJanela.modernas("modernas", os), os);
        }
    }

    @Tag("gui")
    @Test void linuxOptInIsSquareOpaqueWithVisibleOutlineAndWorkingMenu() throws Exception {
        Assumptions.assumeTrue(System.getProperty("os.name").toLowerCase(java.util.Locale.ROOT).contains("linux"));
        String flag = System.getProperty("brmodelo.decoracoes");
        String flatFlag = System.getProperty("flatlaf.useWindowDecorations");
        var old = UIManager.getLookAndFeel();
        Object rootUI = UIManager.get("RootPaneUI");
        Object border = UIManager.get("RootPane.activeBorderColor");
        boolean frames = JFrame.isDefaultLookAndFeelDecorated();
        boolean dialogs = JDialog.isDefaultLookAndFeelDecorated();
        JFrame[] window = new JFrame[1];
        JButton button = new JButton("Test");
        JMenu menu = new JMenu("Menu");
        menu.add(new JMenuItem("Item"));
        AtomicInteger clicks = new AtomicInteger();
        button.addActionListener(event -> clicks.incrementAndGet());
        try {
            SwingUtilities.invokeAndWait(() -> {
                System.clearProperty("brmodelo.decoracoes");
                TemaAplicacao.decoracoes();
                assertFalse(JFrame.isDefaultLookAndFeelDecorated(), "Linux defaults to the system frame");
                assertFalse(JDialog.isDefaultLookAndFeelDecorated());
                System.setProperty("brmodelo.decoracoes", "modernas");
                TemaAplicacao.decoracoes();
                FlatDarkLaf.setup();
                EstiloUI.iniciar();
                assertEquals(UIManager.getColor("Component.borderColor"),
                        UIManager.getColor("RootPane.activeBorderColor"));
                JFrame frame = window[0] = new JFrame("Decoration test");
                JMenuBar bar = new JMenuBar();
                bar.add(menu);
                frame.setJMenuBar(bar);
                frame.add(button);
                frame.setSize(450, 300);
                frame.setVisible(true);
                assertTrue(frame.isUndecorated());
                // An opaque window never shows the desktop through it while resizing.
                assertEquals(255, frame.getBackground().getAlpha());
                assertInstanceOf(FlatRootPaneUI.class, frame.getRootPane().getUI());
                JDialog dialog = new JDialog(frame, "Test dialog", false);
                dialog.setSize(240, 180);
                dialog.addNotify();
                assertTrue(dialog.isUndecorated());
                dialog.dispose();
            });
            SwingUtilities.invokeAndWait(() -> {
                JRootPane root = window[0].getRootPane();
                Point target = SwingUtilities.convertPoint(menu, menu.getWidth() / 2, menu.getHeight() / 2, root);
                assertSame(menu, SwingUtilities.getDeepestComponentAt(root, target.x, target.y),
                        "The embedded menu must stay reachable in the title bar");
                button.doClick();
                assertEquals(1, clicks.get());
            });
        } finally {
            SwingUtilities.invokeAndWait(() -> {
                if (window[0] != null) window[0].dispose();
                restore("brmodelo.decoracoes", flag);
                restore("flatlaf.useWindowDecorations", flatFlag);
                JFrame.setDefaultLookAndFeelDecorated(frames);
                JDialog.setDefaultLookAndFeelDecorated(dialogs);
                UIManager.put("RootPaneUI", rootUI);
                UIManager.put("RootPane.activeBorderColor", border);
                try { UIManager.setLookAndFeel(old); }
                catch (Exception exception) { throw new RuntimeException(exception); }
            });
        }
    }

    private static void restore(String key, String value) {
        if (value == null) System.clearProperty(key); else System.setProperty(key, value);
    }
}

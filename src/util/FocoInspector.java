package util;

import java.awt.KeyboardFocusManager;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import controlador.inspector.Inspector;

/** One focus observer; weak references let closed property dialogs be collected. */
public final class FocoInspector {
    private static final ArrayList<WeakReference<Inspector>> inspectors = new ArrayList<>();
    private static boolean installed;
    private FocoInspector() {}
    public static void instalar(Inspector inspector) {
        inspectors.add(new WeakReference<>(inspector));
        if (installed) return;
        installed = true;
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addPropertyChangeListener("focusOwner", event -> {
            inspectors.removeIf(reference -> reference.get() == null);
            for (WeakReference<Inspector> reference : inspectors) {
                Inspector target = reference.get();
                if (target != null) target.repaint();
            }
        });
    }
}

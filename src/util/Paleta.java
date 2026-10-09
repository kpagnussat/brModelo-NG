package util;

import com.formdev.flatlaf.util.UIScale;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.HierarchyEvent;
import java.beans.PropertyChangeListener;
import javax.swing.*;
import javax.swing.border.Border;

/** Square tools sized to the viewport, with scrolling only below the scaled minimum. */
public final class Paleta implements LayoutManager {
    private final Runnable geometriaAlterada;
    private int ultimaLargura = -1;
    private boolean ultimaRolagem;
    private boolean pendente;

    public Paleta() { this(() -> {}); }
    private Paleta(Runnable geometriaAlterada) { this.geometriaAlterada = geometriaAlterada; }

    public static void instalar(JPanel panel, JScrollPane scroller, Runnable geometriaAlterada) {
        Paleta layout = new Paleta(geometriaAlterada);
        panel.setLayout(layout);
        panel.setMinimumSize(null); // discard the GUI builder's fixed 40px minimum width
        Runnable atualizar = () -> layout.atualizar(panel, scroller);
        scroller.getViewport().addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent event) { atualizar.run(); }
        });
        panel.addPropertyChangeListener(event -> {
            if ("font".equals(event.getPropertyName()) || "UI".equals(event.getPropertyName()))
                atualizar.run();
        });
        // UIScale can change without a viewport resize. Detach with the window to avoid
        // retaining disposed palettes in the global scale listener list.
        PropertyChangeListener escala = event -> atualizar.run();
        panel.addHierarchyListener(event -> {
            if ((event.getChangeFlags() & HierarchyEvent.DISPLAYABILITY_CHANGED) == 0) return;
            if (panel.isDisplayable()) {
                UIScale.addPropertyChangeListener(escala);
                atualizar.run();
            } else UIScale.removePropertyChangeListener(escala);
        });
        if (panel.isDisplayable()) UIScale.addPropertyChangeListener(escala);
        atualizar.run();
    }

    private void atualizar(Container panel, JScrollPane scroller) {
        if (pendente) return;
        pendente = true;
        SwingUtilities.invokeLater(() -> {
            pendente = false;
            panel.invalidate();
            scroller.revalidate();
            geometriaAlterada.run();
            panel.repaint();
        });
    }

    public void addLayoutComponent(String name, Component c) {}
    public void removeLayoutComponent(Component c) {}
    public Dimension minimumLayoutSize(Container c) {
        Insets in = c.getInsets();
        return new Dimension(UIScale.scale(28) + in.left + in.right, in.top + in.bottom);
    }
    public Dimension preferredLayoutSize(Container c) {
        int size = tamanho(c);
        Insets in = c.getInsets();
        return new Dimension(size + in.left + in.right, quantidade(c) * size + in.top + in.bottom);
    }
    public void layoutContainer(Container c) {
        int size = tamanho(c), y = c.getInsets().top;
        for (Component child : c.getComponents()) if (child.isVisible()) {
            child.setBounds(c.getInsets().left, y, size, size);
            if (child instanceof AbstractButton button) {
                EstiloUI.toolbar(button);
                button.setBorder(new Acento());
                button.setBorderPainted(true);
            }
            y += size;
        }
        // A diagram-type change adds/removes tools and invalidates this layout. Notify
        // the split also when only overflow changes (the scrollbar needs extra width).
        int width = preferredLayoutSize(c).width;
        boolean scroll = c.getParent() instanceof JViewport viewport
                && preferredLayoutSize(c).height > viewport.getHeight();
        if (width != ultimaLargura || scroll != ultimaRolagem) {
            ultimaLargura = width;
            ultimaRolagem = scroll;
            if (SwingUtilities.getAncestorOfClass(JScrollPane.class, c) instanceof JScrollPane scroller)
                atualizar(c, scroller);
        }
    }
    private static int quantidade(Container c) {
        int count = 0;
        for (Component child : c.getComponents()) if (child.isVisible()) count++;
        return count;
    }
    private static int tamanho(Container c) {
        int height = c.getParent() instanceof JViewport viewport ? viewport.getExtentSize().height : 0;
        if (height <= 0) return UIScale.scale(40); // preferred size before the first layout
        Insets in = c.getInsets();
        return tamanho(height - in.top - in.bottom, quantidade(c), UIScale.getUserScaleFactor());
    }
    /** Floor division leaves spare pixels below the tools rather than causing overflow. */
    static int tamanho(int alturaDisponivel, int ferramentas, float escala) {
        int min = Math.round(28 * escala), max = Math.round(40 * escala);
        return ferramentas <= 0 ? max : Math.max(min, Math.min(max, alturaDisponivel / ferramentas));
    }
    private static final class Acento implements Border {
        public Insets getBorderInsets(Component c) {
            Icon icon = ((AbstractButton)c).getIcon();
            int x = icon == null ? 4 : Math.max(0, Math.min(4, (c.getWidth() - icon.getIconWidth()) / 2));
            int y = icon == null ? 4 : Math.max(0, Math.min(4, (c.getHeight() - icon.getIconHeight()) / 2));
            return new Insets(y, x, y, x);
        }
        public boolean isBorderOpaque() { return false; }
        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            if (((AbstractButton)c).isSelected()) {
                g.setColor(EstiloUI.cor("Component.accentColor"));
                g.fillRect(x, y + 4, 3, h - 8);
            }
        }
    }
}

package util;

import java.awt.*;
import javax.swing.border.Border;

/** Dynamic gutter separator; a static border must not retain the startup theme color. */
final class BordaLinhaTexto implements Border {
    public Insets getBorderInsets(Component c) { return new Insets(0, 0, 0, 1); }
    public boolean isBorderOpaque() { return false; }
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        g.setColor(EstiloUI.separador());
        g.drawLine(x + width - 1, y, x + width - 1, y + height - 1);
    }
}

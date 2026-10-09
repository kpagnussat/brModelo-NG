/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package desenho.formas;

import controlador.Diagrama;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Point;
import util.DesenhadorDeTexto;

/**
 *
 * @author ccandido
 */
public class FormaRetangular extends Forma {

    private static final long serialVersionUID = -9002399542594756137L;

    public FormaRetangular(Diagrama modelo) {
        super(modelo);
    }

    public FormaRetangular(Diagrama modelo, String texto) {
        super(modelo, texto);
    }

    /**
     * The 1px text offset belongs to the text layout itself: setting it once per paint (as
     * before) missed the first frame after the layout was rebuilt (diagram opened, zoom or
     * print preview), which drew the name 1px off.
     */
    @Override
    public DesenhadorDeTexto getTextoFormatado() {
        DesenhadorDeTexto dz = super.getTextoFormatado();
        dz.CorretorPosicao = new Point(-1, -1);
        return dz;
    }

    @Override
    public void DoPaint(Graphics2D g) {
        g.setPaint(this.getForeColor());
        super.DoPaint(g);
        g.drawRect(getLeft(), getTop(), getWidth() -1, getHeight() -1);
        Paint bkp = g.getPaint();
        g.setPaint(isDisablePainted()? disabledColor : Color.darkGray);
        int L = getLeft();
        int T = getTop();
        int W = getWidth() + L;
        int H = getHeight() + T;
        g.drawLine(L + 1, H, W, H);
        g.drawLine(W, T + 1, W, H);
        g.setPaint(isDisablePainted()? disabledColor : Color.gray);
        g.drawLine(L + 2, H + 1, W + 1, H + 1);
        g.drawLine(W + 1, T + 2, W  +1, H + 1);
        g.setPaint(bkp);
    }
    
}

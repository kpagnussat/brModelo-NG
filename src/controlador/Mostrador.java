/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package controlador;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

/**
 *
 * @author ccandido
 */
public class Mostrador extends BaseControlador {

    /**
     * Intrinsic height of the diagram-tab strip, so its parent's layout manager can size it
     * from the font (at least the original 25px) instead of a hardcoded container height.
     */
    @Override
    public java.awt.Dimension getPreferredSize() {
        return AbasDiagramas.de(this).tamanho();
    }

    public Mostrador() {
        super();
    }
    private Editor master;
    private final ArrayList<Rectangle> areas = new ArrayList<>();
    private final int larg = Math.round(180 * util.Escala.fator(this));
    private final int largPonta = Math.round(24 * util.Escala.fator(this));
    private final int dist = 2;
    private final int tabRecuo = largPonta + dist;
    private final int mover = 20;
    private int scroll = tabRecuo;
    private int selectedIndex = 0;

    public final static String Img = "Controler.interface.mostrador.fechar.img";

    public int getSelectedIndex() {
        return selectedIndex;
    }

    public void setSelectedIndex(int selectedIndex) {
        this.selectedIndex = selectedIndex;
        Construa();
        setTableVisible(selectedIndex);
    }

    public ArrayList<Diagrama> getDiagramas() {
            if (master != null) {
                return master.getDiagramas();
            }
            return null;
    }

    public Editor getMaster() {
        return master;
    }

    public void setMaster(Editor master) {
        this.master = master;
        Construa();
    }

    /**
     * Deve ser chamada apenas quando inserir ou excluir um modelo.
     * @param selIdex
     */
    public void Reset(final int selIdex) {
        setSelectedIndex(selIdex);
        repaint();
    }

    public void Construa() {
        AbasDiagramas.de(this).sincronizar();
    }

    private Rectangle regFechar = null;

    private int fh = -1;

    @Override
    protected void paintComponent(Graphics g) {
        AbasDiagramas.de(this).sincronizar();
        super.paintComponent(g);
    }

    private Rectangle areaFechar(Rectangle tab) {
        int size = com.formdev.flatlaf.util.UIScale.scale(16);
        return new Rectangle(tab.x + tab.width - size - 8, (getHeight() - size) / 2, size, size);
    }

    @Override
    public void mouseExited(MouseEvent e) {
        super.mouseExited(e);
        setOverNow(null);
    }
    /**
     * Mouse over agora.
     */
    private Rectangle overRNow = null;

    public Rectangle getOverNow() {
        return overRNow;
    }

    public void setOverNow(Rectangle overNow) {
        if (this.overRNow == overNow) {
            return;
        }
        if (this.overRNow != null && this.overRNow.equals(overNow)) {
            return;
        }
        //pinto o antigo
        if (this.overRNow != null) {
            repaint(this.overRNow);
        }
        this.overRNow = overNow;
        //pinto o novo
        if (this.overRNow != null) {
            repaint(this.overRNow);
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        super.mouseMoved(e);
        Point p = e.getPoint();
        ChecaCursor(p);
    }

    private void ChecaCursor(Point p) {
        if (areas.isEmpty()) {
            return;
        }
        int i = 0;
        for (Rectangle r : areas) {
            if (r.contains(p)) {
                setOverNow(r);
                if (i > 1) {
                    Rectangle r2 = areaFechar(r);
                    if (r2.contains(p)) {
                        this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
                    } else {
                        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
                    }
                }
                return;
            }
            i++;
        }
        setOverNow(null);
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        super.mouseClicked(e);
    }

    private void ProcessClick(int i) {
        if (i == 0 || i == 1) {
            if (i == 0) {
                if (scroll < tabRecuo) {
                    scroll += mover;
                    repaint();
                }
                return;
            }
            int tam = (((areas.size() - 2) * (larg + dist)) + (2 * largPonta) - dist) - getWidth();
            if (tam > (scroll * -1)) {
                scroll -= mover;
                repaint();
                return;
            }
            return;
        }
        setSelectedIndex(i - 2);
        if (master != null) {
            master.AtiveDiagrama(selectedIndex);
        }
    }

    private void setTableVisible(int idx) {
        AbasDiagramas.de(this).selecionar(idx);
    }
}

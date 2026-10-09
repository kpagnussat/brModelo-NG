/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package desenho.formas;

import desenho.FormaElementar;
import desenho.PontoElementar;
import java.awt.Point;
import java.awt.event.MouseEvent;

/**
 *
 * @author Rick
 */
public class PontoDeForma extends PontoElementar {

    private static final long serialVersionUID = -238630331092502618L;

    public PontoDeForma(FormaElementar pai) {
        super(pai);
    }
    
    // <editor-fold defaultstate="collapsed" desc="Campos">
    private int posicao = -1;

    @Override
    public int getPosicao() {
        return posicao;
    }

    @Override
    public void setPosicao(int posicao) {
        this.posicao = posicao;
        FormaElementar dono = getDono();
        int espaco = dono.distSelecao;
        int mW = getWidth() + espaco;
        int mH = getHeight() + espaco;
        int cr = getWidth() / 2;

        switch (posicao) {
            case 0:
                SetBounds(dono.getLeft() -mW, dono.getTop() - mH, getWidth(), getHeight());
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.NW_RESIZE_CURSOR));
                break;
            case 1:
                SetBounds(dono.getLeft() + dono.getWidth() + espaco, dono.getTop() - mH, getWidth(), getHeight());
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.NE_RESIZE_CURSOR));
                break;
            case 2:
                SetBounds(dono.getLeft() + dono.getWidth() + espaco, dono.getTop() + dono.getHeight() + espaco, getWidth(), getHeight());
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.SE_RESIZE_CURSOR));
                break;
            case 3:
                SetBounds(dono.getLeft() - mW, dono.getTop() + dono.getHeight() + espaco, getWidth(), getHeight());
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.SW_RESIZE_CURSOR));
                break;
            case 4:
                SetBounds(dono.getLeft() + dono.getWidth() / 2 - cr, dono.getTop() -mH, getWidth(), getHeight());
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.N_RESIZE_CURSOR));
                break;
            case 5:
                SetBounds(dono.getLeft() + dono.getWidth() + espaco, dono.getTop() + dono.getHeight() / 2 - cr, getWidth(), getHeight());
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.E_RESIZE_CURSOR));
                break;
            case 6:
                SetBounds(dono.getLeft() + dono.getWidth() / 2 - cr, dono.getTop() + dono.getHeight() + espaco, getWidth(), getHeight());
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.S_RESIZE_CURSOR));
                break;
            case 7:
                SetBounds(dono.getLeft() - mW, dono.getTop() + dono.getHeight() / 2 - cr, getWidth(), getHeight());
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.W_RESIZE_CURSOR));
                break;
        }
    }
    Point down = new Point(0, 0);
    boolean isMouseDown = false;
    public boolean AllwaysHide = false;
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Mouse">
    @Override
    public void mousePressed(MouseEvent e) {
        super.mousePressed(e);
        isMouseDown = true;
        down = new Point(e.getX(), e.getY());
        controlador.EncaixeGrade.iniciar(this, e, getDono().getBounds());
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        isMouseDown = false;
        controlador.EncaixeGrade.terminar(this);
        Forma dono = (Forma) getDono();
        dono.Reenquadre();
        dono.DoRaizeReenquadreReposicione();
        super.mouseReleased(e);
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        super.mouseDragged(e);
        int X = e.getX();
        int Y = e.getY();
        if (isMouseDown && !getDono().isAncorado()) {
            if (((X - down.getX()) != 0) || ((Y - down.getY()) != 0)) {
                if (controlador.EncaixeGrade.ativo()) {
                    controlador.EncaixeGrade.dimensionar(this, e);
                } else {
                    getDono().reSetBounds(getPosicao(), getLeft() + X - down.x, getTop() + Y - down.y);
                }
                down.setLocation(e.getPoint());
            }
        }
    }
    // </editor-fold>
}

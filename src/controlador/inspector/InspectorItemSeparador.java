/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package controlador.inspector;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;

/**
 *
 * @author ccandido
 */
public class InspectorItemSeparador extends InspectorItemBase {

    public InspectorItemSeparador(Inspector criador) {
        super(criador);
        setBackground(util.EstiloUI.fundo(this, "Panel.background"));
    }

    public InspectorItemSeparador() {
        super();
        setBackground(util.EstiloUI.fundo(this, "Panel.background"));
    }

    /**
     * Group headers stand out from the property rows. Light themes keep the original warm
     * tint (blue channel lowered by 15, clamped: the 3.3.2 fix by Vinicius Oliveira Queiroz);
     * on a dark theme that tint turns muddy olive, so the header is lifted toward the text color.
     */
    private static Color corDeFundo(Color c) {
        return util.EstiloUI.elevado();
    }

    /**Configura InspectorItemSeparador para ser base do componente. Um item a mais, invisível (visível, porém, invisível por conta da cor) colocado para facilitar o redesenho do inspector quando se oculta os últimos itens*/
    public boolean endOFF = false;
    
    @Override
    protected void paintBase(Graphics2D g) {
        GradePropriedades.cabecalho(this, g);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (endOFF) return;
        super.mousePressed(e);
        Rectangle r = this.getBounds();
        r = new Rectangle(0, 0, r.width, r.height);
        if (!r.contains(e.getPoint())) return;
        if ('+'  == getEstado()) {
            setEstado('-'); 
        } else {
            setEstado('+');
        }
        Criador.HideShow(this, getEstado());
    }

    private char estado = '-';

    public char getEstado() {
        return estado;
    }

    public void setEstado(char estado) {
        if (this.estado != estado) {
            this.estado = estado;
            repaint();
        }
    }
}

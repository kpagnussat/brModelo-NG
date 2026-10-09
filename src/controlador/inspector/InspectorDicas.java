/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package controlador.inspector;

import controlador.BaseControlador;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import util.DesenhadorDeTexto;

/**
 *
 * @author ccandido
 */
public class InspectorDicas extends BaseControlador {

    public InspectorDicas() {
        super();
        setBackground(util.EstiloUI.fundo(this, "Panel.background"));
    }
    private String Texto = "";

    public String getTexto() {
        return Texto;
    }

    public void setTexto(String Texto) {
        if (this.Texto == null || this.Texto.equals(Texto)) {
            return;
        }
        this.Texto = Texto;
        getTextoFormatado().setTexto(Texto);
        repaint();
    }

    @Override
    public void setFont(Font font) {
        super.setFont(font);
        getTextoFormatado().setFont(font);
    }
    private Color ForeColor = util.EstiloUI.cor("Label.foreground");

    public Color getForeColor() {
        return ForeColor;
    }

    public void setForeColor(Color ForeColor) {
        this.ForeColor = ForeColor;
    }
    private DesenhadorDeTexto TextoFormatado = null;

    public DesenhadorDeTexto getTextoFormatado() {
        if (TextoFormatado == null) {
            TextoFormatado = new DesenhadorDeTexto(getTexto(), getFont(), false);
        }
        return TextoFormatado;
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g); //paint background

        Graphics2D Canvas = (Graphics2D) g;
        Canvas.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING, java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        boolean vazio = getTexto() == null || getTexto().isBlank();
        String text = vazio ? "Selecione uma propriedade para ver a descrição." : getTexto();
        Rectangle area = new Rectangle(8, 8, Math.max(0, getWidth() - 16), Math.max(0, getHeight() - 16));
        getTextoFormatado().PinteTexto(Canvas, util.EstiloUI.cor(vazio ? "Label.disabledForeground" : "Label.foreground"), area, text);
    }
}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package controlador.inspector;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;

/**
 *
 * @author ccandido
 */
public class InspectorItemCor extends InspectorItemExtender {

    public InspectorItemCor(Inspector criador) {
        super(criador);
        setOndeEditar(criador.TipoDlg);
        setMyAction(InspectorExtenderEditor.TipoDeAcao.tpAcaoDlgCor);
    }
    
    public InspectorItemCor(){
        super();
        setMyAction(InspectorExtenderEditor.TipoDeAcao.tpAcaoDlgCor);
    }

    @Override
    protected void paintBase(Graphics2D g) {
        super.paintBase(g);
    }
}

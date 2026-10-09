/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package diagramas.atividade;

import controlador.Diagrama;
import desenho.formas.Forma;
import desenho.formas.FormaCircular;
import desenho.linhas.Linha;
import java.awt.Graphics2D;

/**
 *
 * @author ccandido
 */
public class PreIniFimAtiv extends FormaCircular {

    private static final long serialVersionUID = -499379874848514932L;

    public PreIniFimAtiv(Diagrama modelo) {
        super(modelo);
        editFonte = false;
        showOrgDiag = true;
    }

    public PreIniFimAtiv(Diagrama modelo, String texto) {
        super(modelo, texto);
        editFonte = false;
        showOrgDiag = true;
    }

    @Override
    public boolean CanLiga(Forma forma, Linha lin) {
        return true;
    }

    @Override
    public void PinteTexto(Graphics2D g) {

    }

    @Override
    public void OrganizeDiagrama() {
        OrganizeFluxo();
    }

}

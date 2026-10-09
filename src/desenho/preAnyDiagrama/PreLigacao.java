/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package desenho.preAnyDiagrama;

import controlador.Editor;
import controlador.Diagrama;
import controlador.inspector.InspectorProperty;
import desenho.FormaElementar;
import desenho.formas.Forma;
import desenho.linhas.PontoDeLinha;
import desenho.linhas.SuperLinha;
import java.awt.Point;
import java.util.ArrayList;

/**
 *
 * @author ccandido
 */
public class PreLigacao extends SuperLinha {

    private static final long serialVersionUID = 4654449532952974461L;

    // <editor-fold defaultstate="collapsed" desc="Criação">
    public PreLigacao(Diagrama diagrama) {
        super(diagrama);
        setInteligente(true);
    }

    public PreLigacao(Diagrama diagrama, PreCardinalidade aCard) {
        super(diagrama);
        setInteligente(true);
        InitLCC(aCard);
        diagrama.Remove(aCard, false);
    }

    protected final void InitLCC(PreCardinalidade aCard) {
        setCard(aCard);
        PrepareCardinalidade();
    }


    // </editor-fold>
    
    private PreCardinalidade Card;

    public PreCardinalidade getCard() {
        return Card;
    }

    public void setCard(PreCardinalidade aCard) {
        if (this.Card != aCard) {
            if (this.Card != null) {
                Card.setLigadoA(null);
            }
            this.Card = aCard;
            if (this.Card != null) {
                Card.setLigadoA(this);
            }
        }
    }

    public boolean isCardVisible() {
        if (Card != null) {
            return Card.isVisible();
        } else {
            return false;
        }
    }

    public void PrepareCardinalidade() {
    }

    @Override
    public void reSetBounds() {
        super.reSetBounds();
        PrepareCardinalidade();
    }

    @Override
    public boolean AnexePontos() {
        boolean res = super.AnexePontos();
        if (!res) {
            PrepareCardinalidade();
        }
        return res;
    }

    @Override
    public boolean Destroy() {
        ArrayList<PontoDeLinha> pontos = getPontos();
        for (PontoDeLinha pdl : pontos) {
            pdl.Destroy();
        }
        if (Card != null) {
            Card.setCanBeDeleted(true);
            getMaster().Remove(Card, false);
            //mudei aqui e não testei - comentei abaixo.
        }
        return super.Destroy();
    }

    @Override
    public ArrayList<InspectorProperty> CompleteGenerateProperty(ArrayList<InspectorProperty> GP) {
        GP = super.CompleteGenerateProperty(GP);
        
        GP.add(InspectorProperty.PropertyFactoryCommand(nomeComandos.cmdDoAnyThing.name(), "linha.centre").setTag(140916));
        
        ArrayList<Forma> lst = new ArrayList<>();
        if (getPontaA().getEm() != null) {
            lst.add(getPontaA().getEm());
        }
        if (getPontaB().getEm() != null) {
            lst.add(getPontaB().getEm());
        }
        if (isCardVisible()) {
            lst.add(0, Card);
        }
        boolean ja = false;
        for (Forma f : lst) {
            InspectorProperty ipp = InspectorProperty.PropertyFactoryActionSelect(Editor.fromConfiguracao.getValor("diagrama." + Editor.getClassTexto(f) + ".nome"),
                    f.getTexto(),
                    String.valueOf(f.getID()));
            if (!ja) {
                ja = true;
                GP.add(InspectorProperty.PropertyFactorySeparador("ligacoes"));
            }
            GP.add(ipp);
        }
        return GP;
    }

   @Override
    public FormaElementar getSub(int i) {
        if (i == 0) return getCard();
        return super.getSub(i);
    }

    
    @Override
    public void DoAnyThing(int Tag) {
        super.DoAnyThing(Tag);
        if (Tag == 140916) {
            CentralizarByEntidade();
        }
    }

    public void CentralizarByEntidade() {
        int L = -1;
        Forma A;
        PontoDeLinha P;
        if (getFormaPontaA() instanceof PreEntidade || getFormaPontaA() instanceof PreRelacionamento) {
            L = getPontaA().getLado();
            A = getFormaPontaA();
            P = getPontaA();
            Point pt = A.getPontosCalculados()[L];
            P.setCentro(pt);
        }
        if (getFormaPontaB() instanceof PreEntidade || getFormaPontaB() instanceof PreRelacionamento) {
            L = getPontaB().getLado();
            A = getFormaPontaB();
            P = getPontaB();
            Point pt = A.getPontosCalculados()[L];
            P.setCentro(pt);
        }
        if (L > -1) {
            OrganizeLinha();
            reSetBounds();
            DoMuda();
        }
    }
    
}
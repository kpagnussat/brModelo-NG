/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package controlador.inspector;

import controlador.BaseControlador;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import javax.swing.JComponent;
import javax.swing.JTextField;

/**
 *
 * @author ccandido
 */
public class InspectorItemBase extends BaseControlador {

    //<editor-fold defaultstate="collapsed" desc="Base do componente">
    protected Inspector Criador;
    private boolean selecionado = false;
    protected boolean falhou = false;

    public boolean isSelecionado() {
        return selecionado;
    }

    protected final void getCorParaTexto(Graphics g) {
        g.setColor(GradePropriedades.texto(this));
    }

    /**
     * Intrinsic row size, so the Inspector's layout manager can size rows from content
     * (like CSS auto height) instead of a fixed 20px: at least the original 20px, taller
     * when the theme font needs it. Width is irrelevant: rows stretch to the viewport.
     */
    @Override
    public Dimension getPreferredSize() {
        return new Dimension(0, Math.max(28, getFontMetrics(getFont()).getHeight() + 10));
    }

    protected void setSelecionado(boolean selecionado) {
        this.selecionado = selecionado;
        GradePropriedades.selecionar(this, selecionado);
        DicasInspector.atualizar(this);
        repaint();
    }

    public InspectorItemBase(Inspector criador) {
        this();
        Criador = criador;
        setDoubleBuffered(true);
    }

    public InspectorItemBase() {
        super();
        setLayout(null);
        setFocusable(true);
    }
    private boolean canEdit = true;

    public boolean CanEdit() {
        return canEdit;
    }

    public void setCanEdit(boolean canEdit) {
        if (propriedade.isForceDisable()) {
            canEdit = false;
        } else if (propriedade.isForceEnable()) {
            canEdit = true;
        }
        if (this.canEdit != canEdit) {
            this.canEdit = canEdit;
            if (ondeEditar != null && selecionado) {
                GradePropriedades.selecionar(this, true);
            }
        }
    }

    @Override
    public void paint(Graphics g) {
        super.paintComponent(g);
        RenderingHints renderHints
                = new RenderingHints(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
        renderHints.put(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);

        renderHints.put(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        Graphics2D Canvas = (Graphics2D) g;

        Canvas.addRenderingHints(renderHints);

        Canvas.setStroke(new BasicStroke(
                1f,
                BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND));

        Canvas.setPaint(util.EstiloUI.cor("Label.foreground"));

        paint2D(Canvas);
        paintBorder(g);
        paintChildren(g);
    }

    public void paint2D(Graphics2D g) {
        GradePropriedades.posicionarEditor(this);

        paintBase(g);
    }

    protected void paintBase(Graphics2D g) {
        GradePropriedades.pintar(this, g);
    }

    private JComponent ondeEditar;

    public JComponent getOndeEditar() {
        return ondeEditar;
    }

    public void setOndeEditar(JComponent ondeEditar) {
        if (this.ondeEditar != null) {
            remove(ondeEditar);
        }
        this.ondeEditar = ondeEditar;
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Propriedades">
    private InspectorProperty propriedade = null;

    public InspectorProperty getPropriedade() {
        return propriedade;
    }

    public void setPropriedade(InspectorProperty propriedade) {
        this.propriedade = propriedade;
        setCanEdit(true);

        DicasInspector.atualizar(this);
    }

    public String getTexto() {
        if (propriedade == null) {
            return "";
        }
        return propriedade.caption;
    }

    public String getTransValor() {
        if (propriedade == null) {
            return "";
        }
        return Traduza(propriedade.valor_string);
    }

    public String getValor() {
        if (propriedade == null) {
            return "";
        }
        return propriedade.valor_string;
    }

    public void setValor(String valor) {
        if (propriedade == null) {
            return;
        }
        propriedade.valor_string = valor;
        DicasInspector.atualizar(this);
    }

    public void setFalhou(boolean b) {
        if (this.falhou != b) {
            this.falhou = b;
            repaint();
        }
    }

    public int getTag() {
        return propriedade.getTag();
    }

    public void setTag(int tag) {
        propriedade.setTag(tag);
    }
    //</editor-fold>

    public String Traduza(String texto) {
        return texto;
    }

    public static InspectorItemBase SuperFactory(Inspector principal, InspectorProperty pprt) {
        InspectorItemBase ib = null;
        if (pprt.tipo == InspectorProperty.TipoDeProperty.tpNothing) {
            return null;
        }
        switch (pprt.tipo) {
            case tpBooleano:
                ib = new InspectorItemSN(principal);
                break;
            case tpCor:
                ib = new InspectorItemCor(principal);
                break;
            case tpTextoLongo:
                ib = new InspectorItemExtender(principal, InspectorExtenderEditor.TipoDeAcao.tpAcaoDlgTexto);
                break;
            case tpApenasLeituraTexto:
                ib = new InspectorItemExtender(principal, InspectorExtenderEditor.TipoDeAcao.tpReadOnlyTexto);
                break;
            case tpApenasLeituraCor:
                ib = new InspectorItemExtender(principal, InspectorExtenderEditor.TipoDeAcao.tpReadOnlyCor);
                break;
            case tpSelecObject:
                ib = new InspectorItemExtender(principal, InspectorExtenderEditor.TipoDeAcao.tpAcaoSelectObj);
                break;
            case tpCommand:
                ib = new InspectorItemExtender(principal, InspectorExtenderEditor.TipoDeAcao.tpAcaoCommand);
                break;
            case tpSeparador:
                ib = new InspectorItemSeparador(principal);
                break;
            case tpMenu:
                ib = new InspectorItemMenu(principal);
                break;
            default:
                ib = new InspectorItemTexto(principal);
                break;
        }
        ib.setPropriedade(pprt);
        return ib;
    }

    protected void performGroupSelect() {
    }

    public void RefreshGrupoCanEdit() {
        if (propriedade.agrupada == null || !CanEdit()) {
            return;
        }
        Criador.MakeCanEditGrupo(this);
    }

    private Rectangle area = null;

    public Rectangle getArea() {
        return area;
    }

    public void setArea(Rectangle area) {
        this.area = area;
    }

    transient boolean isMouseDown = false;

    @Override
    public void mouseReleased(MouseEvent e) {
        if (isMouseDown) {
            isMouseDown = false;
        }
        super.mouseReleased(e);
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        super.mouseDragged(e);
        int X = e.getX();
        if (isMouseDown) {
            Caucule(X);
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        GradePropriedades.pressionar(this, e);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        super.mouseMoved(e);
        repaint();
        if (area != null && area.contains(e.getPoint())) {
            setCursor(new Cursor(Cursor.E_RESIZE_CURSOR));
        } else {
            setCursor(Cursor.getDefaultCursor());
        }
    }

    public void Caucule(int movido) {
        if (movido > getWidth() - 20) {
            movido = getWidth() - 20;
        }
        if (movido < 20) {
            movido = 20;
        }
        double x = ((double) movido) / getWidth();
        Criador.setDivisor(x);
    }
}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package controlador.inspector;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.border.Border;

/**
 *
 * @author ccandido
 */
public class InspectorExtenderEditor extends JPanel {

    private final JButton btn;
    private Inspector dono;
    
    public InspectorExtenderEditor(Inspector dono) {
        this();
        this.dono = dono;
        setBackground(util.EstiloUI.fundo(this, "TextField.background"));
        setFocusable(true);
    }
    
    public static class RoundedBorder implements Border {

        private final int radius;

        RoundedBorder(int radius) {
            this.radius = radius;
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(this.radius + 1, this.radius + 1, this.radius + 2, this.radius);
        }

        @Override
        public boolean isBorderOpaque() {
            return true;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            g.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
        }
    }
    
    public InspectorExtenderEditor() {
        super();
        setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 1, 1));
        setBackground(util.EstiloUI.fundo(this, "TextField.background"));
        btn = new JButton("...");
        btn.setBounds(1,1, 20, 20);
        util.EstiloUI.toolbar(btn);
        btn.setToolTipText("Abrir editor ou executar ação");
        btn.setMargin(new Insets(0, 4, 0, 4));
        add(btn);
        validate();
        setFocusable(true);
        btn.addActionListener((ActionEvent e) -> {
            RunDlg();
        });
    }

    public final void RunDlg() {
        if (!isEnabled() || !btn.isEnabled()) return;
        if (getAcaoTipo() == TipoDeAcao.tpAcaoDlgCor) {
            setTexto(util.Dialogos.ShowDlgCor((dono == null ? getRootPane() : dono.getRootPane()), getTexto(), dono.getEditor().diagramaAtual));
        } else if (getAcaoTipo() == TipoDeAcao.tpAcaoDlgTexto) {
            setTexto(util.Dialogos.ShowDlgTexto((dono == null ? getRootPane() : dono.getRootPane()),getTexto()));
        }
        if (dono != null) dono.EndEdit(true, false);
        invalidate();
    }

    public final void RunDlg(String temporario) {
        if (!isEnabled() || !btn.isEnabled()) return;
        if (getAcaoTipo() == TipoDeAcao.tpAcaoDlgCor) {
            setTexto(util.Dialogos.ShowDlgCor((dono == null ? getRootPane() : dono.getRootPane()), temporario, dono.getEditor().diagramaAtual));
        } else if (getAcaoTipo() == TipoDeAcao.tpAcaoDlgTexto) {
            setTexto(util.Dialogos.ShowDlgTexto((dono == null ? getRootPane() : dono.getRootPane()),temporario, getTexto()));
        }
        if (dono != null) dono.EndEdit(true, false);
        invalidate();
    }
    
    private String texto = "";
    
    public enum TipoDeAcao {
        tpAcaoDlgTexto,tpAcaoDlgCor, tpReadOnlyTexto, tpAcaoSelectObj, tpReadOnlyCor, tpAcaoCommand
    } 

    private TipoDeAcao acaoTipo = TipoDeAcao.tpAcaoDlgTexto;

    public TipoDeAcao getAcaoTipo() {
        return acaoTipo;
    }

    public void setAcaoTipo(TipoDeAcao acaoTipo) {
        if (this.acaoTipo != acaoTipo) {
            this.acaoTipo = acaoTipo;
            btn.setEnabled(acaoTipo != TipoDeAcao.tpReadOnlyTexto);
        }
    }
            
    public String getTexto() {
        return texto;
    }

    public void setTexto(String texto) {
        this.texto = texto;
        revalidate();
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        Rectangle r = this.getBounds();
        g.setColor(util.EstiloUI.cor("Label.foreground"));
        int re = 0;
        String bonito = "?";
        if (getAcaoTipo() == TipoDeAcao.tpAcaoDlgCor || getAcaoTipo() == TipoDeAcao.tpReadOnlyCor) {
            g.setColor(util.EstiloUI.cor("Label.foreground"));
            g.fillRect(3, 3, r.height - 7, r.height - 7);
            try {
                Color c = util.Utilidades.StringToColor(getTexto());
                g.setColor(c);
                bonito = getTexto();
            } catch (Exception e) {
                // Keep the default swatch when the stored color cannot be parsed.
            }
            g.fillRect(4, 4, r.height - 8, r.height - 8);
            re = r.height - 1;
        } else {
            bonito = getTexto().replaceAll("\n", " | ");
        }
        
        Rectangle obkp = g.getClipBounds();

        g.setColor(util.EstiloUI.cor("Label.foreground"));
        g.setFont(getFont());
        g.clipRect(re, 0, r.width - r.height -re - (re == 0? 4: 8), r.height);
        g.drawString(bonito, re + 2, (int) (r.height * 0.72) + 1);
        g.drawLine(0, 0, 0, getHeight());
        g.setClip(obkp);
    }

    protected void OrganizeSize() {
        java.awt.Dimension nd = new java.awt.Dimension(getHeight() -1, getHeight() - 2);
        btn.setPreferredSize(nd);
        btn.setSize(nd);
        btn.setToolTipText(getToolTipText() == null ? "Abrir editor ou executar ação" : getToolTipText());
        btn.repaint();
    }
}

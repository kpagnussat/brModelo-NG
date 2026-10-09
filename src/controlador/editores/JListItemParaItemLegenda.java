/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package controlador.editores;

import javax.swing.BorderFactory;
import desenho.formas.Legenda;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

/**
 *
 * @author ccandido
 */
public class JListItemParaItemLegenda extends JLabel implements ListCellRenderer {

    final Color HIGHLIGHT_COLOR = util.EstiloUI.cor("List.selectionBackground");

    private final boolean ehLina;
    
    public JListItemParaItemLegenda(boolean ehLinha) {
        setOpaque(true);
        setIconTextGap(6);
        // Portable replacement for the Windows-only WindowsBorders.DashedBorder (absent from non-Windows JDKs).
        setBorder(BorderFactory.createEmptyBorder(util.AcabamentoDialogos.px(8), util.AcabamentoDialogos.px(8), util.AcabamentoDialogos.px(8), util.AcabamentoDialogos.px(8)));
        this.ehLina = ehLinha;
    }

    @Override
    public Component getListCellRendererComponent(JList list, Object value,
            int index, boolean isSelected, boolean cellHasFocus) {
        Legenda.ItemDeLegenda entry = (Legenda.ItemDeLegenda) value;
        setText(entry.getTexto());

        BufferedImage off_Image = new BufferedImage(ehLina ? 32 : 16, 16, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g = off_Image.createGraphics();
        
        RenderingHints renderHints =
                new RenderingHints(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        renderHints.put(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);
        
        g.addRenderingHints(renderHints);
        g.setColor(entry.getCor());
        if (ehLina) {
            g.fillRect(1, 6, 30, 4);
        } else {
            g.fillRect(1, 1, 14, 14);
        }
        ImageIcon img = new ImageIcon(off_Image);

        setIcon(img);

        if (isSelected) {
            setBackground(util.EstiloUI.cor("List.selectionBackground"));
            setForeground(util.EstiloUI.cor("List.selectionForeground"));
        } else {
            setBackground(util.EstiloUI.cor("List.background"));
            setForeground(util.EstiloUI.cor("Label.foreground"));
        }
        
        return this;
    }
}

package principal;

import java.awt.*;
import javax.swing.*;
import util.EstiloUI;

/** Main-window layout finishing kept outside the generated, Serializable frame. */
final class AcabamentoJanela {
    private AcabamentoJanela() {}
    static void aplicar(FramePrincipal frame, JToolBar diagrams, JToolBar main, JPanel left,
            JTabbedPane tabs, JSplitPane hints, JPanel status, JPanel palette, JScrollPane desk) {
        // GridLayout gave the tab strip the toolbar's height, leaving an empty band.
        controlador.Mostrador strip = frame.getEditor().getShowDiagramas();
        Container rows = strip.getParent();
        Component toolbarRow = rows.getComponent(0);
        rows.setLayout(new BorderLayout());
        rows.add(toolbarRow, BorderLayout.NORTH);
        rows.add(strip, BorderLayout.SOUTH);
        controlador.AbasDiagramas.de(strip).sincronizar();
        toolbar(diagrams);
        toolbar(main);
        main.setMinimumSize(new Dimension(0, main.getPreferredSize().height));
        diagrams.setPreferredSize(new Dimension(diagrams.getPreferredSize().width, main.getPreferredSize().height));
        left.removeAll();
        left.setLayout(new BorderLayout());
        left.add(diagrams, BorderLayout.NORTH);
        left.add(tabs, BorderLayout.CENTER);
        tabs.putClientProperty("JTabbedPane.tabType", "underlined");
        hints.setDividerSize(1);
        EstiloUI.divisorSutil(hints);
        hints.setBorder(null);
        hints.putClientProperty("JSplitPane.expandableSide", "none");
        status.putClientProperty("brmodelo.separador", true);
        status.setBorder(BarraStatus.separador());
        palette.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        palette.setBackground(EstiloUI.fundo(palette, "Panel.background"));
        desk.getViewport().setBackground(EstiloUI.fundo(desk.getViewport(), "mesa"));
    }

    private static void toolbar(JToolBar toolbar) {
        toolbar.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        for (Component child : toolbar.getComponents()) {
            if (child instanceof AbstractButton button) EstiloUI.toolbar(button);
            if (child instanceof JToolBar.Separator separator) {
                separator.setSeparatorSize(new Dimension(8, 1));
                separator.setVisible(false);
                int index = toolbar.getComponentIndex(separator);
                toolbar.remove(separator);
                toolbar.add(Box.createHorizontalStrut(8), index);
            }
        }
    }
}

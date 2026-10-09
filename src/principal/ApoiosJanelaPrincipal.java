package principal;

import controlador.Editor;
import controlador.Acao;
import controlador.Controler.menuComandos;
import javax.swing.Action;
import javax.swing.JButton;
import controlador.apoios.TreeItem;
import java.util.ArrayList;
import java.util.Arrays;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.tree.DefaultTreeCellRenderer;
import partepronta.FormPartes;

/** Hand-written window layout, navigation icons, recent-menu and help initialization. */
final class ApoiosJanelaPrincipal {
    private ApoiosJanelaPrincipal() {}

    static void ajustarDivisorEsquerdo(JPanel inspectors, JSplitPane split) {
        int min = inspectors.getMinimumSize().width + split.getInsets().left;
        if (split.getDividerLocation() < min) split.setDividerLocation(min);
    }

    /**
     * The Inspector tabs use SCROLL_TAB_LAYOUT, so a narrow column hides the last titles
     * behind arrow buttons. Widen the column by what the tab strip is missing (tab bounds
     * come from the look-and-feel after layout; the first tab's left offset is reused as
     * the right margin). Only ever widens; the user can still drag the divider afterwards.
     */
    /**
     * The GUI builder pins the Inspector/hints divider at 550px, so on a tall window the hints
     * panel took half the column and the property grid was cut short. Once the split has its
     * first real size, give the hints about four lines of text and the grid the rest (later
     * resizes go to the grid, as resizeWeight=1 already says). The user can still drag it.
     */
    static void ajustarDivisorDasDicasAoAbrir(JSplitPane split, javax.swing.JComponent dicas) {
        split.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                if (split.getHeight() <= 0) return;
                split.removeComponentListener(this);
                int linha = dicas.getFontMetrics(dicas.getFont()).getHeight();
                int altura = linha * 4 + 16; // four lines plus the 8px padding top and bottom
                int local = split.getHeight() - split.getDividerSize() - altura;
                if (local > split.getMinimumDividerLocation()) split.setDividerLocation(local);
            }
        });
    }

    static void mostrarTodasAsAbasAoAbrir(JTabbedPane abas, JSplitPane split) {
        // Tab widths depend on the font, so they are only known after the first layout.
        // The listener lives here, not in FramePrincipal: a new anonymous class there would
        // renumber FramePrincipal$N and change the serialized-form golden.
        abas.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                if (abas.getWidth() <= 0) return;
                abas.removeComponentListener(this);
                mostrarTodasAsAbas(abas, split);
            }
        });
    }

    static void mostrarTodasAsAbas(JTabbedPane abas, JSplitPane split) {
        int n = abas.getTabCount();
        if (n == 0) return;
        // Measure full titles, because FlatLaf can already have ellipsized the bounds
        // returned by the UI in a narrow column. Include its scaled tab insets.
        java.awt.FontMetrics metrics = abas.getFontMetrics(abas.getFont());
        java.awt.Insets insets = javax.swing.UIManager.getInsets("TabbedPane.tabInsets");
        int padding = insets == null ? 16 : insets.left + insets.right;
        int required = com.formdev.flatlaf.util.UIScale.scale(64);
        for (int i = 0; i < n; i++)
            required += metrics.stringWidth(abas.getTitleAt(i))
                    + com.formdev.flatlaf.util.UIScale.scale(padding);
        int falta = required - abas.getWidth();
        if (falta > 0) split.setDividerLocation(split.getDividerLocation() + falta);
    }

    /**
     * The palette has a fixed width (its buttons), so it sits beside the diagram instead of
     * behind a divider: a JSplitPane kept the divider at its absolute position while the window
     * narrowed, squeezing the palette to nothing until a deferred correction ran.
     */
    static void instalarPaleta(JPanel palette, JSplitPane split, JTabbedPane abas, JScrollPane scroller) {
        java.awt.Component diagrama = split.getLeftComponent();
        javax.swing.JPanel lado = new javax.swing.JPanel(new java.awt.BorderLayout());
        split.remove(diagrama);
        split.remove(abas);
        lado.add(diagrama, java.awt.BorderLayout.CENTER);
        lado.add(abas, java.awt.BorderLayout.EAST);
        abas.setBorder(new SeparadorEsquerdo());
        if (split.getParent() instanceof JSplitPane master && master.getRightComponent() == split)
            master.setRightComponent(lado);
        util.Paleta.instalar(palette, scroller, () -> {
            // A scroll pane's preferred width never counts its vertical scrollbar, so add it
            // when the tools overflow (very short windows) instead of covering the buttons.
            abas.setPreferredSize(null);
            java.awt.Dimension largura = abas.getPreferredSize();
            javax.swing.JViewport viewport = scroller.getViewport();
            if (viewport.getView() != null && viewport.getView().getPreferredSize().height > viewport.getHeight())
                largura.width += scroller.getVerticalScrollBar().getPreferredSize().width;
            abas.setPreferredSize(largura);
            lado.revalidate();
            lado.repaint();
        });
    }

    /** Kept for FramePrincipal's first-layout listener (removing that anonymous class would
     * renumber FramePrincipal$N in the serialized-form golden); the palette has no divider now. */
    static void ajustarDivisorDaPaleta(JSplitPane split, JTabbedPane abas, JScrollPane scroller) {
    }

    /** One-pixel line between diagram and palette, in the current theme's separator color. */
    private static final class SeparadorEsquerdo implements javax.swing.border.Border {
        public java.awt.Insets getBorderInsets(java.awt.Component c) { return new java.awt.Insets(0, 1, 0, 0); }
        public boolean isBorderOpaque() { return true; }
        public void paintBorder(java.awt.Component c, java.awt.Graphics g, int x, int y, int w, int h) {
            g.setColor(util.EstiloUI.separador());
            g.fillRect(x, y, 1, h);
        }
    }

    static void renderizarItem(DefaultTreeCellRenderer renderer, Object value, Editor editor) {
        if (value instanceof TreeItem item) {
            if (item.getId() == 0) {
                renderer.setIcon(editor.getControler().getIconeDoTipo(editor.diagramaAtual.getTipo()));
            } else {
                javax.swing.Icon img = editor.getControler().getIconeInterface(item.getExtraInfo());
                if (img != null) renderer.setIcon(img);
            }
        }
    }

    static void limparRecentes(Editor editor) {
        editor.setRecentes(new ArrayList<>());
        JMenu menu = editor.getMenuRecente();
        JMenuItem limpar = menu.getItem(menu.getItemCount() - 1);
        menu.removeAll();
        menu.addSeparator();
        menu.add(limpar);
        editor.reloadMenuRecentes();
    }

    static void carregarRecentes(JMenu arquivo, Editor editor, Action limpar) {
        JMenu menu = new JMenu(Editor.fromConfiguracao.getValor("Inspector.obj.cfg.recentes"));
        menu.setToolTipText(Editor.fromConfiguracao.getValor("Inspector.dica.cfg.recentes"));
        arquivo.add(menu, 2);
        editor.setMenuRecente(menu);
        JMenuItem item = new JMenuItem(Editor.fromConfiguracao.getValor("Inspector.obj.cfg.recentes.limbar"));
        item.setToolTipText(Editor.fromConfiguracao.getValor("Inspector.dica.cfg.recentes.limpar"));
        item.addActionListener(limpar);
        menu.addSeparator();
        menu.add(item);
        ArrayList<String> lst = new ArrayList<>();
        if (Editor.fromConfiguracao.hasValor("cfg.recentes")) {
            lst.addAll(Arrays.asList(Editor.fromConfiguracao.getValor("cfg.recentes").split(";")));
        }
        editor.setRecentes(lst);
        editor.reloadMenuRecentes();
        menu.setEnabled(lst.size() > 0);
    }


    static FormPartes criarFormPartes(FramePrincipal frame, JMenuItem salvar) {
        FormPartes partes = new FormPartes();
        partes.externalSalvar = salvar;
        partes.setLocationRelativeTo(frame);
        return partes;
    }

    static void vincularAcoes(Editor editor, JButton btnAnterior, JButton btnProximo, JButton btnSendToBack, JButton btnBringToFront, JButton btnDesfazer, JButton btnRefazer, JButton b0, JButton b1, JButton b2, JButton b3, JButton b4, JButton b5, JButton b6, JButton b7, JButton b8, JButton b9, JButton b10, JButton b11, JButton b12, JButton b13, JButton btnSalvar) {
        for (Acao a : editor.getControler().ListaDeAcoesEditaveis) {
            String k = a.getValue(Action.ACTION_COMMAND_KEY).toString();
            menuComandos mc = menuComandos.valueOf(k);
            switch (mc) {
                case cmdSelAnt:
                    btnAnterior.setAction(a);
                    break;
                case cmdSelProx:
                    btnProximo.setAction(a);
                    break;
                case cmdSendToBack:
                    btnSendToBack.setAction(a);
                    break;
                case cmdBringToFront:
                    btnBringToFront.setAction(a);
                    break;
                case cmdUndo:
                    btnDesfazer.setAction(a);
                    break;
                case cmdRendo:
                    btnRefazer.setAction(a);
                    break;
                case cmdMicroAjuste0:
                    b0.setAction(a);
                    break;
                case cmdMicroAjuste1:
                    b1.setAction(a);
                    break;
                case cmdMicroAjuste2:
                    b2.setAction(a);
                    break;
                case cmdMicroAjuste3:
                    b3.setAction(a);
                    break;

                case cmdCopyFormat:
                    b4.setAction(a);
                    break;
                case cmdPasteFormat:
                    b5.setAction(a);
                    break;
                case cmdDimPastLeft:
                    b6.setAction(a);
                    break;
                case cmdDimPastTop:
                    b7.setAction(a);
                    break;
                case cmdDimPastRight:
                    b8.setAction(a);
                    break;
                case cmdDimPastBottom:
                    b9.setAction(a);
                    break;
                case cmdDimPastWidth:
                    b10.setAction(a);
                    break;
                case cmdDimPastHeight:
                    b11.setAction(a);
                    break;

                case cmdDimAlignH:
                    b12.setAction(a);
                    break;
                case cmdDimAlignV:
                    b13.setAction(a);
                    break;

                case cmdSave:
                    btnSalvar.setAction(a);
                    break;
            }
        }
    }
}

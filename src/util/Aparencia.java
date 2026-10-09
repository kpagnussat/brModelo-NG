package util;

import com.formdev.flatlaf.util.UIScale;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.*;
import javax.swing.border.Border;

/** Modal appearance picker; all state stays outside Swing's serialized subclasses. */
public final class Aparencia {
    private final JDialog dialog;
    private final JRadioButton sistema = new JRadioButton("Seguir o sistema");
    private final JRadioButton fixo = new JRadioButton("Usar sempre o tema escolhido");
    private final JComboBox<CatalogoTemas.Tema> claros = new JComboBox<>(CatalogoTemas.metade(false).toArray(CatalogoTemas.Tema[]::new));
    private final JComboBox<CatalogoTemas.Tema> escuros = new JComboBox<>(CatalogoTemas.metade(true).toArray(CatalogoTemas.Tema[]::new));
    private final Map<CatalogoTemas.Tema, JToggleButton> cards = new LinkedHashMap<>();
    private final JScrollPane scroller;
    private final JLabel contexto = new JLabel();
    private boolean sincronizando;

    private Aparencia(Window owner) {
        dialog = new JDialog(owner, "Aparência", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        JPanel root = new JPanel(new BorderLayout(UIScale.scale(12), UIScale.scale(12)));
        root.setBorder(BorderFactory.createEmptyBorder(UIScale.scale(16), UIScale.scale(16), UIScale.scale(16), UIScale.scale(16)));
        dialog.setContentPane(root);
        JPanel modes = new JPanel(); modes.setLayout(new BoxLayout(modes, BoxLayout.Y_AXIS));
        sistema.setMnemonic('S'); fixo.setMnemonic('U');
        DicasInterface.dica(sistema, "themeAuto"); DicasInterface.dica(fixo, "themeFixed");
        DicasInterface.dica(claros, "lightTheme"); DicasInterface.dica(escuros, "darkTheme");
        ButtonGroup group = new ButtonGroup(); group.add(sistema); group.add(fixo);
        sistema.setAlignmentX(Component.LEFT_ALIGNMENT); modes.add(sistema);
        JPanel combos = new JPanel(new FlowLayout(FlowLayout.LEFT, UIScale.scale(8), UIScale.scale(4)));
        JLabel light = new JLabel("Tema claro"); light.setLabelFor(claros); light.setDisplayedMnemonic('C');
        JLabel dark = new JLabel("Tema escuro"); dark.setLabelFor(escuros); dark.setDisplayedMnemonic('E');
        claros.getAccessibleContext().setAccessibleName("Tema claro do sistema");
        escuros.getAccessibleContext().setAccessibleName("Tema escuro do sistema");
        combos.add(light); combos.add(claros); combos.add(dark); combos.add(escuros);
        combos.setAlignmentX(Component.LEFT_ALIGNMENT); modes.add(combos);
        fixo.setAlignmentX(Component.LEFT_ALIGNMENT); modes.add(fixo);
        contexto.setAlignmentX(Component.LEFT_ALIGNMENT);
        contexto.setBorder(BorderFactory.createEmptyBorder(UIScale.scale(8), 0, 0, 0));
        modes.add(contexto); root.add(modes, BorderLayout.NORTH);
        JPanel gallery = new JPanel(); gallery.setLayout(new BoxLayout(gallery, BoxLayout.Y_AXIS));
        secao(gallery, "Claros", false); secao(gallery, "Escuros", true);
        scroller = new JScrollPane(gallery);
        scroller.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroller.setBorder(BorderFactory.createEmptyBorder());
        scroller.getVerticalScrollBar().setUnitIncrement(UIScale.scale(32));
        scroller.setPreferredSize(UIScale.scale(new Dimension(744, 410)));
        root.add(scroller);
        JButton close = new JButton("Fechar"); DicasInterface.dica(close, "close"); close.setMnemonic('F'); close.addActionListener(e -> dialog.dispose());
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0)); footer.add(close); root.add(footer, BorderLayout.SOUTH);
        dialog.getRootPane().setDefaultButton(close);
        dialog.getRootPane().registerKeyboardAction(e -> dialog.dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        sistema.addActionListener(e -> { TemaAplicacao.aplicar("auto"); sincronizar(); });
        fixo.addActionListener(e -> { TemaAplicacao.aplicar(TemaAplicacao.ativo()); sincronizar(); });
        claros.addActionListener(e -> preferir(claros, false)); escuros.addActionListener(e -> preferir(escuros, true));
        sincronizar();
        dialog.pack();
        // Fit real desktops, while retaining all cards in the scrollable region.
        if (owner != null && owner.getGraphicsConfiguration() != null) {
            Rectangle screen = owner.getGraphicsConfiguration().getBounds();
            Insets in = Toolkit.getDefaultToolkit().getScreenInsets(owner.getGraphicsConfiguration());
            dialog.setSize(Math.min(dialog.getWidth(), screen.width - in.left - in.right),
                    Math.min(dialog.getHeight(), screen.height - in.top - in.bottom));
        }
        dialog.setLocationRelativeTo(owner);
    }
    public static JDialog criar(Window owner) { return new Aparencia(owner).dialog; }
    public static void abrir(Component owner) { criar(SwingUtilities.getWindowAncestor(owner)).setVisible(true); }
    private void preferir(JComboBox<CatalogoTemas.Tema> combo, boolean dark) {
        if (sincronizando) return;
        TemaAplicacao.preferir(((CatalogoTemas.Tema) combo.getSelectedItem()).id(), dark);
        sincronizar();
    }
    private void secao(JPanel gallery, String title, boolean dark) {
        JLabel heading = new JLabel(title);
        heading.setFont(heading.getFont().deriveFont(Font.BOLD));
        heading.setBorder(BorderFactory.createEmptyBorder(UIScale.scale(12), 0, UIScale.scale(8), 0));
        heading.setAlignmentX(Component.LEFT_ALIGNMENT); gallery.add(heading);
        java.util.List<CatalogoTemas.Tema> themes = CatalogoTemas.metade(dark);
        JPanel grid = new JPanel(new GridLayout(0, 4, UIScale.scale(8), UIScale.scale(8)));
        grid.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (CatalogoTemas.Tema tema : themes) {
            JToggleButton card = new JToggleButton(tema.nome(), MiniaturaTema.obter(tema));
            card.setHorizontalTextPosition(SwingConstants.CENTER); card.setVerticalTextPosition(SwingConstants.BOTTOM);
            card.setIconTextGap(UIScale.scale(6));
            card.setPreferredSize(UIScale.scale(new Dimension(172, 114)));
            card.setBorder(new Anel()); card.setBorderPainted(true);
            card.getAccessibleContext().setAccessibleDescription("Tema " + (dark ? "escuro" : "claro") + ": " + tema.nome());
            card.putClientProperty("brmodelo.tema.id", tema.id());
            card.addActionListener(e -> { TemaAplicacao.escolher(tema); sincronizar(); });
            cards.put(tema, card); grid.add(card);
            navegar(card, tema);
        }
        for (int i = themes.size(); i % 4 != 0; i++) grid.add(new JPanel());
        grid.setMaximumSize(new Dimension(Integer.MAX_VALUE, grid.getPreferredSize().height));
        gallery.add(grid);
    }
    private void navegar(JToggleButton card, CatalogoTemas.Tema tema) {
        for (int code : new int[]{KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT, KeyEvent.VK_UP, KeyEvent.VK_DOWN}) {
            int step = code == KeyEvent.VK_LEFT ? -1 : code == KeyEvent.VK_RIGHT ? 1 : code == KeyEvent.VK_UP ? -4 : 4;
            card.registerKeyboardAction(e -> {
                JToggleButton target = cards.get(vizinho(tema, step));
                target.requestFocusInWindow(); target.scrollRectToVisible(new Rectangle(target.getSize()));
            }, KeyStroke.getKeyStroke(code, 0), JComponent.WHEN_FOCUSED);
        }
        card.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusGained(java.awt.event.FocusEvent e) { card.scrollRectToVisible(new Rectangle(card.getSize())); }
        });
    }
    /** Follow the visible four-column grid, skipping empty cells between sections. */
    static CatalogoTemas.Tema vizinho(CatalogoTemas.Tema tema, int step) {
        var light = CatalogoTemas.metade(false); var dark = CatalogoTemas.metade(true);
        int offset = (light.size() + 3) / 4 * 4, total = offset + (dark.size() + 3) / 4 * 4;
        int position = tema.escuro() ? offset + dark.indexOf(tema) : light.indexOf(tema);
        for (int i = 0; i < total; i++) {
            position = Math.floorMod(position + step, total);
            if (position < light.size()) return light.get(position);
            if (position >= offset && position - offset < dark.size()) return dark.get(position - offset);
        }
        return tema;
    }
    private void sincronizar() {
        sincronizando = true;
        try {
            boolean auto = TemaAplicacao.escolha().equals("auto");
            sistema.setSelected(auto); fixo.setSelected(!auto);
            claros.setSelectedItem(CatalogoTemas.buscar(TemaAplicacao.preferido(false)));
            escuros.setSelectedItem(CatalogoTemas.buscar(TemaAplicacao.preferido(true)));
            claros.setEnabled(auto); escuros.setEnabled(auto);
            contexto.setText(auto ? "Os cards definem o par preferido. O tema ativo acompanha o sistema."
                    : "Escolha um card para mudar o tema imediatamente.");
            for (var entry : cards.entrySet()) {
                CatalogoTemas.Tema tema = entry.getKey(); JToggleButton card = entry.getValue();
                boolean selected = tema.id().equals(auto ? TemaAplicacao.preferido(tema.escuro()) : TemaAplicacao.escolha());
                card.setSelected(selected);
                card.setToolTipText(DicasInterface.texto(auto ? "themePrefer" : "themeApply") + ": " + tema.nome() + (tema.id().equals(TemaAplicacao.ativo()) ? " — em uso" : selected ? " — preferido do sistema" : ""));
                card.getAccessibleContext().setAccessibleDescription(card.getToolTipText());
            }
        } finally { sincronizando = false; }
    }
    private static final class Anel implements Border {
        public Insets getBorderInsets(Component c) { int n = UIScale.scale(8); return new Insets(n, n, n, n); }
        public boolean isBorderOpaque() { return false; }
        public void paintBorder(Component c, Graphics graphics, int x, int y, int w, int h) {
            AbstractButton card = (AbstractButton)c;
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(EstiloUI.cor(card.isSelected() || card.hasFocus() ? "Component.accentColor" : "Component.borderColor"));
                int n = UIScale.scale(card.isSelected() ? 2 : 1);
                g.setStroke(new BasicStroke(n));
                g.drawRoundRect(x + n, y + n, w - 2 * n - 1, h - 2 * n - 1, UIScale.scale(10), UIScale.scale(10));
                if (card.hasFocus()) { g.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 1, new float[]{2, 2}, 0));
                    g.drawRoundRect(x + 4 * n, y + 4 * n, w - 8 * n - 1, h - 8 * n - 1, 6, 6); }
            } finally { g.dispose(); }
        }
    }
}

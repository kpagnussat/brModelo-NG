package controlador.inspector;

import java.awt.*;

/** Combines a single-command section visually, leaving its items and collapse state intact. */
final class DisposicaoPropriedades implements LayoutManager {
    static InspectorItemSeparador cabecalhoDoComando(InspectorItemBase row) {
        if (row.getPropriedade().tipo != InspectorProperty.TipoDeProperty.tpCommand) return null;
        var items = row.Criador.getItens();
        int index = items.indexOf(row);
        if (index < 1 || !(items.get(index - 1) instanceof InspectorItemSeparador header)
                || !header.getTexto().equals(row.getTexto())) return null;
        return index + 1 == items.size() || items.get(index + 1) instanceof InspectorItemSeparador ? header : null;
    }
    private static boolean unido(Component component) {
        if (!(component instanceof InspectorItemSeparador header) || header.getEstado() != '-') return false;
        var items = header.Criador.getItens();
        int index = items.indexOf(header);
        return index + 1 < items.size() && items.get(index + 1).isVisible()
                && cabecalhoDoComando(items.get(index + 1)) == header;
    }
    @Override public void addLayoutComponent(String name, Component component) {}
    @Override public void removeLayoutComponent(Component component) {}
    @Override public Dimension preferredLayoutSize(Container parent) {
        int height = 0;
        for (Component c : parent.getComponents()) if (c.isVisible() && !unido(c)) height += c.getPreferredSize().height;
        return new Dimension(0, height);
    }
    @Override public Dimension minimumLayoutSize(Container parent) { return preferredLayoutSize(parent); }
    @Override public void layoutContainer(Container parent) {
        int y = 0;
        for (Component c : parent.getComponents()) {
            int height = c.isVisible() && !unido(c) ? c.getPreferredSize().height : 0;
            c.setBounds(0, y, parent.getWidth(), height);
            y += height;
        }
    }
}

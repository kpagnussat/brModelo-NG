package controlador.inspector;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.*;
import util.DicasInterface;

/** Mouse listeners deliberately live outside the Serializable property items. */
public final class DicasInspector {
    private DicasInspector() {}

    static void instalar(InspectorItemBase row) {
        observar(row);
        atualizar(row);
    }

    private static void observar(JComponent component) {
        if (Boolean.TRUE.equals(component.getClientProperty("brmodelo.dica.inspector"))) return;
        component.putClientProperty("brmodelo.dica.inspector", true);
        MouseAdapter listener = new MouseAdapter() {
            private void refresh() {
                InspectorItemBase row = component instanceof InspectorItemBase item ? item
                        : (InspectorItemBase) SwingUtilities.getAncestorOfClass(InspectorItemBase.class, component);
                if (row != null) atualizar(row);
            }
            @Override public void mouseEntered(MouseEvent event) { refresh(); }
            @Override public void mouseMoved(MouseEvent event) { refresh(); }
        };
        component.addMouseListener(listener);
        component.addMouseMotionListener(listener);
        ToolTipManager.sharedInstance().registerComponent(component);
    }

    public static void atualizar(InspectorItemBase row) {
        if (row.getPropriedade() == null || row.Criador == null) return;
        String tip = texto(row);
        row.setToolTipText(tip);
        for (Component child : row.getComponents()) atualizarEditor(child, tip);
    }

    private static void atualizarEditor(Component child, String tip) {
        if (child instanceof JComponent component) {
            component.setToolTipText(tip);
            observar(component);
        }
        if (child instanceof Container container)
            for (Component nested : container.getComponents()) atualizarEditor(nested, tip);
    }

    static String texto(InspectorItemBase row) {
        if (row.Criador.getEditor() != null && !row.Criador.getEditor().isMostrarTooltips()) return null;
        InspectorProperty p = row.getPropriedade();
        String action = !row.CanEdit() || GradePropriedades.leitura(row) ? "readonly" : switch (p.tipo) {
            case tpSeparador -> "collapse";
            case tpBooleano -> "toggle";
            case tpMenu, tpCor, tpSelecObject -> "choose";
            case tpCommand -> "run";
            default -> "edit";
        };
        String value = row.getTransValor();
        // Reserve the same accessories as the property grid (checkbox, swatch, arrow/editor).
        int reserve = switch (p.tipo) {
            case tpBooleano, tpCor, tpApenasLeituraCor, tpMenu, tpTextoLongo, tpSelecObject -> row.getHeight() + 24;
            default -> 16;
        };
        boolean clipped = p.tipo != InspectorProperty.TipoDeProperty.tpCommand
                && p.tipo != InspectorProperty.TipoDeProperty.tpSeparador && value != null && !value.isEmpty()
                && (value.contains("\n") || row.getFontMetrics(row.getFont()).stringWidth(value)
                    > row.getWidth() - GradePropriedades.divisor(row) - reserve);
        String description = p.dica == null ? "" : p.dica;
        if (description.isBlank()) description = DicasInterface.escape(p.caption);
        return "<html><div style='width: 340px'>" + description
                + (clipped ? "<br><br>" + DicasInterface.escape(value).replace("\n", "<br>") : "")
                + "<br><br>" + DicasInterface.texto(action) + "</div></html>";
    }
}

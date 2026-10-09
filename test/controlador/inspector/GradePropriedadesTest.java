package controlador.inspector;

import com.formdev.flatlaf.FlatLightLaf;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.HashMap;
import javax.swing.*;
import org.junit.jupiter.api.*;
import principal.FramePrincipal;
import static org.junit.jupiter.api.Assertions.*;
import static controlador.inspector.InspectorProperty.TipoDeProperty.*;

@Tag("gui")
class GradePropriedadesTest {
    @Test void editorsStayInsideDraggedValueColumnAndReadOnlyNeverAddsAnEditor() throws Exception {
        onInspector(inspector -> {
            InspectorItemBase text = inspector.Add(property(tpTextoNormal, "Nome", "Aurora"));
            InspectorItemBase readOnly = inspector.Add(property(tpApenasLeituraTexto, "Arquivo", "aurora.brM3"));
            InspectorItemBase readOnlyColor = inspector.Add(property(tpApenasLeituraCor, "Cor", "80,150,210,255"));
            layout(inspector);
            for (double divider : new double[]{.25, .5, .72}) {
                inspector.setDivisor(divider);
                inspector.PerformSelect(text);
                GradePropriedades.posicionarEditor(text);
                JComponent editor = text.getOndeEditar();
                assertEquals(new Rectangle((int)(text.getWidth() * divider), 0,
                        text.getWidth() - (int)(text.getWidth() * divider), text.getHeight()), editor.getBounds());
                assertEquals(text.getFont(), editor.getFont());
                assertEquals(8, editor.getInsets().left);
                var metrics = text.getFontMetrics(text.getFont());
                assertEquals((text.getHeight() - metrics.getHeight()) / 2 + metrics.getAscent(),
                        editor.getBaseline(editor.getWidth(), editor.getHeight()));
                inspector.PerformSelect(readOnly);
                assertEquals(0, readOnly.getComponentCount());
                assertEquals(UIManager.getColor("Label.disabledForeground"), GradePropriedades.texto(readOnly));
                inspector.PerformSelect(readOnlyColor);
                assertEquals(0, readOnlyColor.getComponentCount());
            }
            text.setCanEdit(false);
            inspector.PerformSelect(text);
            assertEquals(0, text.getComponentCount());
        });
    }
    @Test void mergedCommandShowsOnlyTheButtonAndDoesNotCollapse() throws Exception {
        onInspector(inspector -> {
            ArrayList<InspectorProperty> properties = new ArrayList<>();
            properties.add(InspectorProperty.getPropertySeparador("Converter"));
            properties.add(property(tpCommand, "Converter", "..."));
            properties.add(InspectorProperty.getPropertySeparador("Outros"));
            properties.add(property(tpTextoNormal, "Nome", "Aurora"));
            inspector.Carrege(properties);
            layout(inspector);
            InspectorItemSeparador header = (InspectorItemSeparador)inspector.getItens().get(0);
            InspectorItemBase command = inspector.getItens().get(1);
            assertEquals(0, header.getHeight());
            assertTrue(command.getHeight() > 0);
            assertEquals(5, inspector.getItens().size()); // includes the existing sentinel
            // Pressing near the left edge (where a chevron used to be) must not hide the button.
            GradePropriedades.pressionar(command, press(command, 8));
            layout(inspector);
            assertEquals('-', header.getEstado());
            assertTrue(command.isVisible());
            assertEquals(0, header.getHeight());
            assertSame(properties.get(1), command.getPropriedade());
        });
    }
    @Test void clickingBooleanValueTogglesOnceAndNameOnlySelects() throws Exception {
        onInspector(inspector -> {
            var row = inspector.Add(property(tpBooleano, "Visível", "true"));
            layout(inspector);
            // Observe the shared editor without handing invented dev properties to the diagram.
            for (ItemListener listener : inspector.TipoSN.getItemListeners()) inspector.TipoSN.removeItemListener(listener);
            int[] changes = {0};
            inspector.TipoSN.addItemListener(e -> changes[0]++);
            GradePropriedades.pressionar(row, press(row, 8));
            assertTrue(inspector.TipoSN.isSelected());
            changes[0] = 0;
            GradePropriedades.pressionar(row, press(row, row.getWidth() - 20));
            assertFalse(inspector.TipoSN.isSelected());
            assertEquals(1, changes[0]);
            assertEquals(0, row.getComponentCount());
        });
    }
    @Test void hoverHintsFollowReboundPropertiesEditorsAndPreferences() throws Exception {
        onInspector(inspector -> {
            inspector.getEditor().setMostrarTooltips(true);
            var p = property(tpTextoNormal, "Nome", "Valor muito longo <Aurora> & Biblioteca ".repeat(5));
            p.dica = "Descrição usada no painel de dicas.";
            var row = inspector.Add(p);
            layout(inspector);
            for (int x : new int[]{8, row.getWidth() - 8}) {
                row.dispatchEvent(new MouseEvent(row, MouseEvent.MOUSE_MOVED, 1, 0, x, 10, 0, false));
                String tip = row.getToolTipText();
                assertTrue(tip.contains(p.dica));
                assertTrue(tip.contains("&lt;Aurora&gt; &amp; Biblioteca"));
                assertTrue(tip.contains("Clique para editar"));
            }
            inspector.PerformSelect(row);
            assertEquals(row.getToolTipText(), row.getOndeEditar().getToolTipText());
            var rebound = property(tpTextoNormal, "Nome", "Curto");
            rebound.dica = "Nova descrição";
            row.setPropriedade(rebound);
            assertTrue(row.getToolTipText().contains("Nova descrição"));
            assertFalse(row.getToolTipText().contains("Aurora"));
            row.setCanEdit(false);
            row.dispatchEvent(new MouseEvent(row, MouseEvent.MOUSE_MOVED, 1, 0, 10, 10, 0, false));
            assertTrue(row.getToolTipText().contains("Somente leitura"));
            inspector.getEditor().setMostrarTooltips(false);
            row.dispatchEvent(new MouseEvent(row, MouseEvent.MOUSE_MOVED, 1, 0, 10, 10, 0, false));
            assertNull(row.getToolTipText());
            inspector.getEditor().setMostrarTooltips(true);
            for (var type : new InspectorProperty.TipoDeProperty[]{tpBooleano, tpMenu, tpApenasLeituraTexto}) {
                var prop = property(type, "Opção", type == tpBooleano ? "true" : type == tpMenu ? "0" : "Arquivo");
                prop.opcoesMenu = java.util.List.of("Normal");
                var item = inspector.Add(prop);
                assertTrue(item.getToolTipText().contains(type == tpBooleano ? "Clique para alternar"
                        : type == tpMenu ? "Clique para escolher" : "Somente leitura"));
            }
        });
    }
    private static MouseEvent press(Component c, int x) {
        return new MouseEvent(c, MouseEvent.MOUSE_PRESSED, 1, 0, x, 10, 1, false, MouseEvent.BUTTON1);
    }
    private static InspectorProperty property(InspectorProperty.TipoDeProperty type, String caption, String value) {
        var p = new InspectorProperty();
        p.tipo = type;
        p.caption = caption;
        p.valor_string = value;
        return p;
    }
    private static void layout(Inspector inspector) {
        inspector.setSize(420, 240);
        inspector.doLayout();
        inspector.getViewport().doLayout();
        inspector.getBox().doLayout();
    }
    private static void onInspector(java.util.function.Consumer<Inspector> check) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            var old = UIManager.getLookAndFeel();
            var config = new HashMap<>(controlador.Editor.fromConfiguracao.getConfiguracao());
            FramePrincipal frame = null;
            try {
                FlatLightLaf.setup();
                frame = new FramePrincipal();
                frame.getEditor().setAutoSaveInterval(0);
                Inspector inspector = new Inspector();
                inspector.setEditor(frame.getEditor());
                inspector.setDicas(new InspectorDicas());
                check.accept(inspector);
            } finally {
                if (frame != null) { frame.getEditor().EndAutoSave(); frame.dispose(); }
                controlador.Editor.fromConfiguracao.setConfiguracao(config);
                try { UIManager.setLookAndFeel(old); } catch (Exception e) { throw new RuntimeException(e); }
            }
        });
    }
}

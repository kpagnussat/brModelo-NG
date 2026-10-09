package controlador;

import controlador.inspector.Inspector;
import controlador.inspector.InspectorProperty;
import java.awt.Color;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

/** Loads editor preferences and builds/applies their inspector properties. */
final class ConfiguracaoEditor {
    private ConfiguracaoEditor() {}

    static void PreCarregueConfig(Editor alvo) {
        try {
            String a = Editor.fromConfiguracao.getValor("cfg.mostrardimensoesaomover");
            boolean sn = a.equals("cfg.mostrardimensoesaomover") ? true : Boolean.valueOf(a);
            alvo.setMostrarDimensoesAoMover(sn);

            a = Editor.fromConfiguracao.getValor("cfg.mostrargrade");
            sn = a.equals("cfg.mostrargrade") ? true : Boolean.valueOf(a);
            alvo.setShowGrid(sn);

            a = Editor.fromConfiguracao.getValor("cfg.ancorador");
            sn = a.equals("cfg.ancorador") ? true : Boolean.valueOf(a);
            alvo.setAncorador(sn);

            a = Editor.fromConfiguracao.getValor("cfg.propaguedeletetolines");
            sn = a.equals("cfg.propaguedeletetolines") ? true : Boolean.valueOf(a);
            alvo.setPropagueDeleteToLines(sn);

            a = Editor.fromConfiguracao.getValor("cfg.location.salvar");
            sn = a.equals("cfg.location.salvar") ? false : Boolean.valueOf(a);
            alvo.setSalvarLocation(sn);

            a = Editor.fromConfiguracao.getValor("cfg.gradelargura");
            if (a.equals("cfg.gradelargura")) {
                a = "20";
            }
            int tmp = Integer.valueOf(a);
            alvo.setGridWidth(tmp);

            a = Editor.fromConfiguracao.getValor("cfg.tipodefault");
            if (a.equals("cfg.tipodefault")) {
                a = "0";
            }
            tmp = Integer.valueOf(a);
            alvo.setTipoDefaultInt(tmp);

            a = Editor.fromConfiguracao.getValor("cfg.mostrarids");
            sn = a.equals("cfg.mostrarids") ? false : Boolean.valueOf(a);
            alvo.setMostrarIDs(sn);

            a = Editor.fromConfiguracao.getValor("cfg.mostrartooltips");
            sn = a.equals("cfg.mostrartooltips") ? true : Boolean.valueOf(a);
            alvo.setMostrarTooltips(sn);

            a = Editor.fromConfiguracao.getValor("cfg.autosalvarintervalo");
            if (a.equals("cfg.autosalvarintervalo")) {
                a = "5";
            }
            tmp = Integer.valueOf(a);
            alvo.PreInicieAutoSave(tmp);

        } catch (NumberFormatException e) {
            util.BrLogger.Logger("ERROR_LOAD_CFGFILE", e.getMessage());
        }
    }

    static ArrayList<InspectorProperty> GenerateProperty(Editor alvo) {
        ArrayList<InspectorProperty> res = new ArrayList<>();

        res.add(InspectorProperty.PropertyFactorySeparador("cfg"));
        res.add(InspectorProperty.PropertyFactorySN("cfg.mostrardimensoesaomover", "setMostrarDimensoesAoMover", alvo.isMostrarDimensoesAoMover()));

        ArrayList<String> dias = new ArrayList<>();
        for (Diagrama.TipoDeDiagrama tp : Diagrama.TipoDeDiagrama.values()) {
            String tmp = Editor.fromConfiguracao.getValor("Inspector.lst.tipodiagrama." + tp.name().substring(2).toLowerCase());
            dias.add(tmp);
        }
        res.add(InspectorProperty.PropertyFactoryMenu("cfg.tipodefault", "setTipoDefaultInt", alvo.getTipoDefault().ordinal(), dias));

        res.add(InspectorProperty.PropertyFactorySeparador("desenho"));

        res.add(InspectorProperty.PropertyFactorySN("cfg.propaguedeletetolines", "setPropagueDeleteToLines", alvo.isPropagueDeleteToLines()));

        res.add(InspectorProperty.PropertyFactorySN("cfg.mostrargrade", "setShowGrid", alvo.isShowGrid()));

        res.add(InspectorProperty.PropertyFactorySN(EncaixeGrade.CHAVE, "setEncaixarGrade", EncaixeGrade.ativo()));

        res.add(InspectorProperty.PropertyFactoryNumero("cfg.gradelargura", "setGridWidth", alvo.getGridWidth()));

        res.add(InspectorProperty.PropertyFactorySN("cfg.location.salvar", "setSalvarLocation", alvo.isSalvarLocation()));

        res.add(InspectorProperty.PropertyFactoryNumero("cfg.autosalvarintervalo", "setAutoSaveInterval", alvo.getAutoSaveInterval()));

        res.add(InspectorProperty.PropertyFactorySeparador("cfg.exibicao", true));
        res.add(InspectorProperty.PropertyFactorySN("cfg.ancorador", "setAncorador", alvo.isAncorador()));
        res.add(InspectorProperty.PropertyFactorySN("cfg.mostrarids", "setMostrarIDs", alvo.isMostrarIDs()));
        res.add(InspectorProperty.PropertyFactorySN("cfg.mostrartooltips", "setMostrarTooltips", alvo.isMostrarTooltips()));

        res.add(InspectorProperty.PropertyFactorySeparador("cfg.edicao", true));
        res.add(InspectorProperty.PropertyFactorySN("cfg.apagartextoaoeditar", "setApagarTextoAoEditar", alvo.isApagarTextoAoEditar()));

        return res;
    }

    static boolean AceitaEdicao(Editor alvo, InspectorProperty propriedade, String valor) {
        Editor ed = alvo;
        Class[] par = new Class[1];
        Object[] vl = new Object[1];
        try {
            switch (propriedade.tipo) {
                case tpBooleano:
                    par[0] = Boolean.TYPE;
                    vl[0] = Boolean.parseBoolean(valor);
                    break;
                case tpCor:
                    par[0] = Color.class;
                    vl[0] = util.Utilidades.StringToColor(valor);
                    break;
                case tpMenu:
                    par[0] = Integer.TYPE;
                    int p = Integer.parseInt(valor);
                    vl[0] = p;
                    break;
                case tpNumero:
                    par[0] = Integer.TYPE;
                    int tmp = Integer.parseInt(valor);
                    vl[0] = tmp;
                    break;
                default:
                    par[0] = String.class;
                    vl[0] = valor;
            }
            if (!EncaixeGrade.CHAVE.equals(propriedade.configuracaoStr)) {
                Class cl = ed.getClass();
                Method mthd = cl.getMethod(propriedade.property, par);
                mthd.invoke(ed, vl);
            }

            if (!propriedade.configuracaoStr.isEmpty()) {
                Editor.fromConfiguracao.SetAndSaveIfNeed(propriedade.configuracaoStr, valor);
            }

            alvo.PerformInspectorCfg();

        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            util.BrLogger.Logger("ERROR_SET_PROPERTY", e.getMessage());
            return false;
        }
        return true;
    }
}

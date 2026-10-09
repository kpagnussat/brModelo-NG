package controlador;

import desenho.Ancorador;
import desenho.FormaElementar;

/** Coordinates selection, endpoint colors and anchor visibility on the existing model list. */
final class SelecaoDiagrama {
    private SelecaoDiagrama() {}

    static void HidePontosOnSelecao(Diagrama diagrama, boolean esconde) {
        for (FormaElementar item : diagrama.itensSelecionados) {
            item.HidePontos(esconde);
        }
    }

    static boolean DiagramaDoSelecao(Diagrama diagrama, Ancorador ancorador, FormaElementar item, boolean ehmouse, boolean ForcarMultSel) {
        if (item == null) {
            diagrama.ClearSelect();
            return false;
        }
        if (!item.isSelecionavel()) {
            diagrama.master.getControler().makeEnableComands();
            return false;
        }

        boolean combine = ((diagrama.isShiftDown() || diagrama.isControlDown()) && ehmouse) || ForcarMultSel;
        if (diagrama.itensSelecionados.indexOf(item) == -1) {
            if (combine) {
                diagrama.AddSelect(item);
            } else {
                if (diagrama.itensSelecionados.size() > 0) {
                    diagrama.ClearSelect(false);
                }
                diagrama.AddSelect(item);
            }
            diagrama.PerformInspector();
            diagrama.master.getControler().makeEnableComands();

            ancorador.Posicione(diagrama.getSelecionado());

            return true;
        } else if ((combine) && (diagrama.itensSelecionados.size() > 1)) {
            diagrama.RemoveSelect(item);
            diagrama.PerformInspector();

            ancorador.Posicione(diagrama.getSelecionado());

            return false;
        } else {
            diagrama.master.getControler().makeEnableComands();

            ancorador.Posicione(diagrama.getSelecionado());

            return true;
        }
    }

    static void AddSelect(Diagrama diagrama, FormaElementar item) {
        diagrama.itensSelecionados.add(item);
        if (diagrama.itensSelecionados.size() > 1) {
            diagrama.PontosCor(item, true);
        }
        item.setSelecionado(true);
    }

    static void PromoveToFirstSelect(Diagrama diagrama, FormaElementar item) {
        int idx = diagrama.itensSelecionados.indexOf(item);
        if (idx > 0) {
            diagrama.PontosCor(diagrama.itensSelecionados.get(0), true);
            diagrama.itensSelecionados.remove(item);
            diagrama.itensSelecionados.add(0, item);
            diagrama.PontosCor(item);
        }
    }

    static void RemoveSelect(Diagrama diagrama, FormaElementar item) {
        if (diagrama.itensSelecionados.indexOf(item) == -1) {
            return;
        }
        diagrama.itensSelecionados.remove(item);
        diagrama.PontosCor(item);
        item.setSelecionado(false);
        diagrama.PontosCor(diagrama.itensSelecionados.get(0));
    }

    static void ClearSelect(Diagrama diagrama, Ancorador ancorador, boolean performInsp) {
        diagrama.itensSelecionados.forEach(item -> {
            diagrama.PontosCor(item);
            item.setSelecionado(false);
        });
        diagrama.itensSelecionados.clear();
        if (performInsp) {
            diagrama.master.getControler().makeEnableComands();
            diagrama.PerformInspector();
        }
        ancorador.SetVisible(false);
    }
}

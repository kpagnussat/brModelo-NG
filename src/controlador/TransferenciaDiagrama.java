package controlador;

import desenho.FormaElementar;
import desenho.formas.Desenhador;
import desenho.formas.Forma;
import desenho.linhas.SuperLinha;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import org.w3c.dom.Document;
import util.XMLGenerate;

/** Copies and pastes diagram selections and images through the system clipboard. */
final class TransferenciaDiagrama {
    private TransferenciaDiagrama() {}

    static void doCopy(Diagrama alvo) {
        String res = Diagrama.SaveToXml(alvo, true);
        StringSelection vai = new StringSelection(res);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(vai, alvo);
    }

    static void doCopy(Diagrama alvo, BufferedImage img) {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        util.TransferableImage trans = new util.TransferableImage(img);
        clipboard.setContents(trans, alvo);
    }

    static void doPaste(Diagrama alvo) {
        String txt = getClipboardContents();
        if (!"".equals(txt)) {
            Document doc = util.XMLGenerate.LoadDocument(txt);
            alvo.processePaste(doc);
        } else {
            BufferedImage img = getClipboardContentsImage();
            if (img != null) {
                int x = alvo.getEditor().getMargem() >= alvo.ScrPosicao.x ? 4 : alvo.ScrPosicao.x + 4 - alvo.getEditor().getMargem();
                int y = alvo.getEditor().getMargem() >= alvo.ScrPosicao.y ? 4 : alvo.ScrPosicao.y + 4 - alvo.getEditor().getMargem();

                Point p = alvo.tradutorZoom(new Point(x, y));
                x = p.x;
                y = p.y;

                FormaElementar xres = alvo.ExternalRealiseComando(Controler.Comandos.cmdDesenhador, p);
                Desenhador de = (Desenhador) xres;
                de.setTipoImg();
                de.setImagem(img);
                de.SetBounds(x, y, img.getWidth(), img.getHeight());
                de.InvalidateArea();
                alvo.DiagramaDoSelecao(de, false, false);
                alvo.DoMuda(null);
                alvo.PerformInspector();
            }
        }
    }

    static boolean doPaste(Diagrama alvo, String txt) {
        if (!"".equals(txt)) {
            Document doc = util.XMLGenerate.LoadDocument(txt);
            return (alvo.processePaste(doc));
        }
        return false;
    }

    static boolean processePaste(Diagrama alvo, Document doc) {
        if (doc != null) {
            boolean res = alvo.LoadFromXML(doc, true);
            if (res) {
                Point p = alvo.getPontoMenorSelecionado();
                Point q = new Point((alvo.getEditor().getMargem() >= alvo.ScrPosicao.x ? alvo.ScrPosicao.x : alvo.ScrPosicao.x - alvo.getEditor().getMargem()),
                        (alvo.getEditor().getMargem() >= alvo.ScrPosicao.y ? alvo.ScrPosicao.y : alvo.ScrPosicao.y - alvo.getEditor().getMargem()));
                q = alvo.tradutorZoom(q);
                final int x = p.x - q.x;
                final int y = p.y - q.y;

                ArrayList<FormaElementar> lst = new ArrayList<>();
                alvo.getItensSelecionados().stream().forEach(f -> lst.add(f));
                alvo.setSelecionado(null);

                lst.stream().filter(f -> f instanceof Forma).forEach(fe -> {
                    fe.HidePontos(true);
                    fe.DoMove(-x + 2 * fe.distSelecao, -y + 2 * fe.distSelecao);
                    fe.Reposicione();
                    fe.HidePontos(false);
                });

                lst.stream().filter(f -> f instanceof SuperLinha).map(sl -> (SuperLinha) sl).forEach(fe -> {
                    fe.HidePontos(true);
                    final int a = -x + 2 * fe.distSelecao;
                    final int b = -y + 2 * fe.distSelecao;
                    if (fe.getPontaA().getEm() == null && fe.getPontaB().getEm() == null) {
                        fe.DoMove(a, b);
                    } else {
                        if (fe.getPontaA().getEm() != null && fe.getPontaB().getEm() == null) {
                            fe.getPontos().stream().filter(pt -> pt != fe.getPontaA()).forEach(pt -> pt.DoMove(a, b));
                        }
                        if (fe.getPontaB().getEm() != null && fe.getPontaA().getEm() == null) {
                            fe.getPontos().stream().filter(pt -> pt != fe.getPontaB()).forEach(pt -> pt.DoMove(a, b));
                        }
                    }
                    fe.Reposicione();
                    fe.HidePontos(false);
                });

                //Seleciona novamente.
                if (!lst.isEmpty()) {
                    lst.stream().forEach(el -> alvo.DiagramaDoSelecao(el, false, true));
                    alvo.PromoveToFirstSelect(lst.get(0));
                }
            }
            return true;
        }
        return false;
    }

    static String getClipboardContents() {
        String result = "";
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        //odd: the Object param of getContents is not currently used
        Transferable contents = clipboard.getContents(null);
        boolean hasTransferableText = (contents != null)
                && contents.isDataFlavorSupported(DataFlavor.stringFlavor);
        if (hasTransferableText) {
            try {
                result = (String) contents.getTransferData(DataFlavor.stringFlavor);
            } catch (UnsupportedFlavorException | IOException ex) {
                util.BrLogger.Logger("ERROR_DIAGRAMA_CLIPBOARD", ex.getMessage());
            }
        }
        return result;
    }

    static BufferedImage getClipboardContentsImage() {
        BufferedImage result = null;
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        //odd: the Object param of getContents is not currently used
        Transferable contents = clipboard.getContents(null);
        boolean hasTransferableImg = (contents != null)
                && contents.isDataFlavorSupported(DataFlavor.imageFlavor);
        if (hasTransferableImg) {
            try {
                result = (BufferedImage) contents.getTransferData(DataFlavor.imageFlavor);
            } catch (UnsupportedFlavorException | IOException ex) {
                util.BrLogger.Logger("ERROR_DIAGRAMA_CLIPBOARD", ex.getMessage());
            }
        }
        return result;
    }
}

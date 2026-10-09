package desenho.formas;

import desenho.linhas.PontoDeLinha;
import desenho.preAnyDiagrama.PreEntidade;
import desenho.preAnyDiagrama.PreRelacionamento;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Organizes only the endpoints on a shape; the caller aligns attributes and records undo. */
public final class OrganizadorDeConexoes {
    private OrganizadorDeConexoes() {}

    static int lado(Rectangle forma, Point parceiro, int atual, boolean manter) {
        double dx = parceiro.x - forma.getCenterX();
        double dy = parceiro.y - forma.getCenterY();
        if (manter || (dx == 0 && dy == 0)) return atual;
        // Compare against the ray through a corner, taking the aspect ratio into account.
        if (Math.abs(dx) * forma.height >= Math.abs(dy) * forma.width) {
            return dx < 0 ? 0 : 2;
        }
        return dy < 0 ? 1 : 3;
    }

    static int coordenada(int inicio, int comprimento, int margem, int indice, int quantidade) {
        int recuo = Math.min(Math.max(0, margem), comprimento / 2);
        int deslocamento = (int) Math.round((double) comprimento * indice / (quantidade + 1));
        return inicio + Math.clamp(deslocamento, recuo, comprimento - recuo);
    }

    static int eixo(Point ponto, int lado) {
        return lado % 2 == 0 ? ponto.y : ponto.x;
    }

    public static boolean organize(Forma forma) {
        List<List<PontoDeLinha>> lados = new ArrayList<>();
        for (int i = 0; i < 4; i++) lados.add(new ArrayList<>());
        boolean mudou = false;
        // Determine every side before moving anything, so order and side use the original partners.
        for (PontoDeLinha ponto : forma.getListaDePontosLigados()) {
            Forma parceiro = ponto.getDono().getOutraPonta(ponto).getEm();
            int lado = lado(forma.getBounds(), posicaoParceiro(ponto), ponto.getLado(),
                    parceiro == null || parceiro == forma || autoRelacionamento(forma, parceiro) ||
                    forma.getMaster().getItensSelecionados().contains(parceiro));
            lados.get(lado).add(ponto);
        }
        for (int lado = 0; lado < 4; lado++) {
            final int atual = lado;
            List<PontoDeLinha> pontos = lados.get(lado);
            // Stable for equal partner coordinates, including the two endpoints of a self-link.
            pontos.sort(Comparator.comparingInt(p -> eixo(posicaoParceiro(p), atual)));
            for (int i = 0; i < pontos.size(); i++) {
                PontoDeLinha ponto = pontos.get(i);
                Point antes = ponto.getCentro();
                int ladoAntes = ponto.getLado();
                Point destino = destino(forma, lado, i + 1, pontos.size());
                ponto.setCentro(destino);
                // Same attachment/snap and line refresh used by PontoDeLinha.mouseReleased.
                forma.PosicionePonto(ponto);
                ponto.getDono().OrganizeLinha();
                ponto.getDono().reSetBounds();
                mudou |= ladoAntes != ponto.getLado() || !antes.equals(ponto.getCentro());
            }
        }
        return mudou;
    }

    private static boolean autoRelacionamento(Forma forma, Forma parceiro) {
        // In the conceptual editor a self-relationship is also represented by two links
        // through the same diamond. Keep those sides, just like a direct self-link.
        return (forma instanceof PreRelacionamento r && parceiro instanceof PreEntidade && r.isAutoRelacionamento())
                || (parceiro instanceof PreRelacionamento outro && forma instanceof PreEntidade && outro.isAutoRelacionamento());
    }

    private static Point posicaoParceiro(PontoDeLinha ponto) {
        PontoDeLinha outra = ponto.getDono().getOutraPonta(ponto);
        if (outra.getEm() == null) return outra.getCentro();
        Rectangle bounds = outra.getEm().getBounds();
        return new Point(bounds.x + bounds.width / 2, bounds.y + bounds.height / 2);
    }

    private static Point destino(Forma forma, int lado, int indice, int quantidade) {
        int posicao = lado % 2 == 0
                ? coordenada(forma.getTop(), forma.getHeight(), forma.INI_ORGDIAG, indice, quantidade)
                : coordenada(forma.getLeft(), forma.getWidth(), forma.INI_ORGDIAG, indice, quantidade);
        if (forma instanceof FormaLosangular losango) {
            // A diamond has three manual docks per logical side. Use the nearest legal dock
            // on that side, rather than creating an endpoint the user cannot drag there.
            Point[] docks = losango.getAllSubPoints();
            Point melhor = docks[lado];
            for (int i = lado + 4; i < docks.length; i += 4) {
                if (Math.abs(eixo(docks[i], lado) - posicao) < Math.abs(eixo(melhor, lado) - posicao)) {
                    melhor = docks[i];
                }
            }
            return new Point(melhor);
        }
        return switch (lado) {
            case 0 -> new Point(forma.getLeft(), posicao);
            case 1 -> new Point(posicao, forma.getTop());
            case 2 -> new Point(forma.getLeftWidth(), posicao);
            default -> new Point(posicao, forma.getTopHeight());
        };
    }
}

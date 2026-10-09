package controlador;

import desenho.Elementar;
import desenho.FormaElementar;
import desenho.formas.Forma;
import desenho.formas.PontoDeForma;
import desenho.linhas.Linha;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.Map;
import java.util.WeakHashMap;

/** User gestures only: no grid state or drag state is written into diagrams. Used on the EDT. */
public final class EncaixeGrade {
    static final String CHAVE = "cfg.encaixargrade";
    private static final Map<Elementar, Gesto> gestos = new WeakHashMap<>();

    private EncaixeGrade() {}

    private record Gesto(Point mouse, Rectangle bounds) {}

    public static boolean ativo() {
        String valor = Editor.fromConfiguracao.getValor(CHAVE);
        return valor.equals(CHAVE) || Boolean.parseBoolean(valor);
    }

    static Point coordenadas(Point tela, double zoom) {
        double fator = 1.0 / zoom;
        return new Point((int) (tela.x * fator), (int) (tela.y * fator));
    }

    static int multiplo(int valor, int grade) {
        return (int) (Math.round((double) valor / Math.max(1, grade)) * Math.max(1, grade));
    }

    static Point posicao(Point ponto, int grade, boolean ativo, boolean alt) {
        return ativo && !alt ? new Point(multiplo(ponto.x, grade), multiplo(ponto.y, grade)) : new Point(ponto);
    }

    /** A single anchor supplies the delta for the entire selection. */
    static Point deslocamento(Point inicio, Point mouseInicio, Point mouse, Point atual,
                              int grade, boolean ativo, boolean alt) {
        Point alvo = posicao(new Point(inicio.x + mouse.x - mouseInicio.x,
                inicio.y + mouse.y - mouseInicio.y), grade, ativo, alt);
        return new Point(alvo.x - atual.x, alvo.y - atual.y);
    }

    static Rectangle tamanho(Rectangle inicio, int canto, int dx, int dy,
                             int grade, boolean ativo, boolean alt) {
        grade = Math.max(1, grade);
        Rectangle alvo = new Rectangle(inicio);
        boolean esquerda = canto == 0 || canto == 3 || canto == 7;
        boolean direita = canto == 1 || canto == 2 || canto == 5;
        boolean cima = canto == 0 || canto == 1 || canto == 4;
        boolean baixo = canto == 2 || canto == 3 || canto == 6;
        if (esquerda) { alvo.x += dx; alvo.width -= dx; }
        if (direita) alvo.width += dx;
        if (cima) { alvo.y += dy; alvo.height -= dy; }
        if (baixo) alvo.height += dy;
        // Keep the existing minimum, including when crossing the opposite edge.
        int largura = Math.max(10, alvo.width);
        int altura = Math.max(10, alvo.height);
        if (esquerda) alvo.x = inicio.x + inicio.width - largura;
        if (cima) alvo.y = inicio.y + inicio.height - altura;
        alvo.width = largura;
        alvo.height = altura;
        if (ativo && !alt) {
            alvo.x = multiplo(alvo.x, grade);
            alvo.y = multiplo(alvo.y, grade);
            int minimo = ((10 + grade - 1) / grade) * grade;
            alvo.width = Math.max(minimo, multiplo(alvo.width, grade));
            alvo.height = Math.max(minimo, multiplo(alvo.height, grade));
        }
        return alvo;
    }

    static int limitar(int alvo, int minimo, int maximo, int grade, boolean encaixar) {
        if (encaixar) {
            int inferior = (int) Math.ceil((double) minimo / grade) * grade;
            int superior = (int) Math.floor((double) maximo / grade) * grade;
            if (inferior <= superior) return Math.clamp(alvo, inferior, superior);
        }
        return minimo <= maximo ? Math.clamp(alvo, minimo, maximo) : alvo;
    }

    public static void iniciar(Elementar item, MouseEvent evento, Rectangle bounds) {
        gestos.put(item, new Gesto(evento.getPoint(), new Rectangle(bounds)));
    }

    public static void terminar(Elementar item) {
        gestos.remove(item);
    }

    public static Point mover(FormaElementar item, MouseEvent evento, Point anterior) {
        if (!ativo() || item instanceof Linha)
            return new Point(evento.getX() - anterior.x, evento.getY() - anterior.y);
        Gesto gesto = gestos.get(item);
        if (gesto == null) return new Point();
        if (gesto.mouse.equals(evento.getPoint()) && gesto.bounds.getLocation().equals(item.getLocation()))
            return new Point();
        Editor editor = item.getMaster().getEditor();
        Point delta = deslocamento(gesto.bounds.getLocation(), gesto.mouse, evento.getPoint(), item.getLocation(),
                editor.getGridWidth(), true, evento.isAltDown());
        // Keep the whole selection inside the page, so release cannot clamp its members separately.
        Rectangle grupo = new Rectangle(item.getBounds());
        if (item.isSelecionado() && item instanceof Forma) {
            for (FormaElementar selecionado : item.getMaster().getItensSelecionados())
                grupo.add(selecionado.getBounds());
        }
        int x = limitar(item.getLeft() + delta.x, item.getLeft() - grupo.x,
                item.getLeft() + item.getMaster().getWidth() - grupo.x - grupo.width,
                editor.getGridWidth(), !evento.isAltDown());
        int y = limitar(item.getTop() + delta.y, item.getTop() - grupo.y,
                item.getTop() + item.getMaster().getHeight() - grupo.y - grupo.height,
                editor.getGridWidth(), !evento.isAltDown());
        return new Point(x - item.getLeft(), y - item.getTop());
    }

    public static void dimensionar(PontoDeForma ponto, MouseEvent evento) {
        Gesto gesto = gestos.get(ponto);
        if (gesto == null) return;
        Forma dono = (Forma) ponto.getDono();
        Editor editor = dono.getMaster().getEditor();
        Rectangle alvo = tamanho(gesto.bounds, ponto.getPosicao(), evento.getX() - gesto.mouse.x,
                evento.getY() - gesto.mouse.y, editor.getGridWidth(), ativo(), evento.isAltDown());
        alvo.x = limitar(alvo.x, 0, dono.getMaster().getWidth() - alvo.width,
                editor.getGridWidth(), !evento.isAltDown());
        alvo.y = limitar(alvo.y, 0, dono.getMaster().getHeight() - alvo.height,
                editor.getGridWidth(), !evento.isAltDown());
        // Feed the normal resize path, including subclass layout and link notifications.
        var pontos = dono.getPontos();
        pontos[0].setLocation(alvo.x - pontos[0].getWidth() - dono.distSelecao,
                alvo.y - pontos[0].getHeight() - dono.distSelecao);
        pontos[2].setLocation(alvo.x + alvo.width + dono.distSelecao,
                alvo.y + alvo.height + dono.distSelecao);
        dono.reSetBounds();
    }

    /** Only commands that place a standalone shape at the click; satellites retain automatic placement. */
    static void criar(Editor editor, Controler.Comandos comando, FormaElementar item) {
        if (item == null || !ativo()) return;
        if (item instanceof diagramas.conceitual.Texto texto && texto.getLinhaMestre() != null) return;
        boolean manual = switch (comando) {
            case cmdEntidade, cmdRelacionamento, cmdEntidadeAssociativa, cmdUniao, cmdEspecializacao,
                 cmdTabela, cmdInicioAtividade, cmdFimAtividade, cmdEstadoAtividade, cmdDecisaoAtividade,
                 cmdForkJoinAtividade, cmdRaiaAtividade, cmdFluxIniFim, cmdFluxProcesso, cmdFluxConector,
                 cmdFluxDecisao, cmdFluxDocumento, cmdFluxVDocumentos, cmdFluxNota, cmdEapProcesso,
                 cmdEapBarraLigacao, cmdLivreRetangulo, cmdLivreRetanguloArr, cmdLivreComentario,
                 cmdLivreTriangulo, cmdLivreJuncao, cmdLivreDocumento, cmdLivreVariosDocumentos,
                 cmdLivreNota, cmdLivreSuperTexto, cmdLivreLosango, cmdLivreCirculo, cmdLivreDrawer,
                 cmdDesenhador, cmdTexto, cmdLegenda -> true;
            default -> false;
        };
        if (manual) {
            Point alvo = posicao(item.getLocation(), editor.getGridWidth(), true, false);
            alvo.x = limitar(alvo.x, 0, item.getMaster().getWidth() - item.getWidth(), editor.getGridWidth(), true);
            alvo.y = limitar(alvo.y, 0, item.getMaster().getHeight() - item.getHeight(), editor.getGridWidth(), true);
            item.DoMove(alvo.x - item.getLeft(), alvo.y - item.getTop());
        }
    }
}

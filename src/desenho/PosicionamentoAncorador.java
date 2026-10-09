package desenho;

import controlador.Diagrama;
import desenho.formas.Forma;
import desenho.linhas.Linha;
import desenho.linhas.PontoDeLinha;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.List;

/** Screen geometry only: no state is added to the serialized diagram graph. */
public final class PosicionamentoAncorador {
    private static final int ICON_WIDTH = 18;
    private static final int STEP = 20;
    private static final int GAP = 8;
    private PosicionamentoAncorador() {}

    public static Rectangle endpointHitArea(PontoDeLinha point) {
        Rectangle bounds = point.getBounds();
        bounds.grow(bounds.width / 2, bounds.height / 2);
        return bounds;
    }

    private static List<PontoDeLinha> endpoints(Diagrama diagram) {
        List<PontoDeLinha> result = new ArrayList<>();
        for (FormaElementar item : diagram.getListaDeItens()) {
            if (item instanceof Linha line && line.isVisible()) {
                if (line.getPontaA() != null) result.add(line.getPontaA());
                if (line.getPontaB() != null) result.add(line.getPontaB());
            }
        }
        return result;
    }

    /**
     * Over the action strip, a line endpoint under an icon wins (even if its line is not selected),
     * so the user can always grab it. Elsewhere the diagram's own hit-testing applies unchanged:
     * a press near a shape's edge must still select the shape, not detach a line.
     */
    public static Elementar capture(Diagrama diagram, Ancorador strip, Point point) {
        if (!strip.IsMe(point)) return diagram.CaptureFromPoint(point);
        for (PontoDeLinha endpoint : endpoints(diagram)) {
            if (endpointHitArea(endpoint).contains(point)) return endpoint;
        }
        return strip;
    }

    public static List<Rectangle> layout(FormaElementar selected, int count) {
        if (count == 0) return List.of();
        Diagrama diagram = selected.getMaster();
        List<PontoDeLinha> attached = selected instanceof Forma shape
                ? shape.getListaDePontosLigados() : List.of();
        List<PontoDeLinha> endpoints = endpoints(diagram);
        boolean[] occupied = new boolean[4];
        for (PontoDeLinha endpoint : attached) occupied[endpoint.getLado()] = true;
        boolean allOccupied = occupied[0] && occupied[1] && occupied[2] && occupied[3];
        List<Linha> lines = attached.stream().map(PontoDeLinha::getDono).distinct().toList();
        Rectangle best = null;
        double[] bestScore = null;
        int bestSide = 0;
        // Tie order: left, right, top, bottom. Dock numbers are left, top, right, bottom.
        for (int side : new int[]{0, 2, 1, 3}) {
            if (!allOccupied && occupied[side]) continue;
            int clearance = GAP;
            for (PontoDeLinha endpoint : attached) {
                Rectangle hit = endpointHitArea(endpoint);
                int distance = switch (side) {
                    case 0 -> selected.getLeft() - hit.x;
                    case 2 -> hit.x + hit.width - selected.getLeftWidth();
                    case 1 -> selected.getTop() - hit.y;
                    default -> hit.y + hit.height - selected.getTopHeight();
                };
                clearance = Math.max(clearance, distance + 1);
            }
            // The outward candidate is only as far as needed to clear the endpoint handles.
            for (int gap : allOccupied ? new int[]{clearance} : new int[]{GAP, clearance}) {
                Rectangle candidate = candidate(selected, count, side, gap);
                double[] score = score(candidate, diagram, selected, endpoints, lines, gap);
                if (bestScore == null || better(score, bestScore)) {
                    best = candidate;
                    bestScore = score;
                    bestSide = side;
                }
            }
        }
        List<Rectangle> tiles = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            tiles.add(new Rectangle(best.x + (bestSide % 2 == 1 ? STEP * i : 0),
                    best.y + (bestSide % 2 == 0 ? STEP * i : 0), ICON_WIDTH, STEP));
        }
        return tiles;
    }

    private static Rectangle candidate(FormaElementar selected, int count, int side, int gap) {
        int width = side % 2 == 0 ? ICON_WIDTH : STEP * (count - 1) + ICON_WIDTH;
        int height = side % 2 == 0 ? STEP * count : STEP;
        // Align to the leading edge. No tangential shift can move the strip away from its shape.
        int x = switch (side) {
            case 0 -> selected.getLeft() - width - gap;
            case 2 -> selected.getLeftWidth() + gap;
            default -> selected.getLeft();
        };
        int y = switch (side) {
            case 1 -> selected.getTop() - height - gap;
            case 3 -> selected.getTopHeight() + gap;
            default -> selected.getTop();
        };
        return new Rectangle(x, y, width, height);
    }

    private static double[] score(Rectangle strip, Diagrama diagram, FormaElementar selected,
            List<PontoDeLinha> endpoints, List<Linha> lines, int gap) {
        double handles = 0, routes = 0, shapes = 0;
        for (PontoDeLinha endpoint : endpoints) handles += overlap(strip, endpointHitArea(endpoint));
        for (Linha line : lines) {
            Point[] points = line.getPontosParaDesenho();
            if (points == null) continue;
            for (int i = 1; i < points.length; i++) routes += clippedLength(strip, points[i - 1], points[i]);
        }
        for (FormaElementar item : diagram.getListaDeItens()) {
            if (item != selected && item instanceof Forma && item.isVisible()) shapes += overlap(strip, item.getBounds());
        }
        double outside = (double) strip.width * strip.height
                - overlap(strip, new Rectangle(0, 0, diagram.getWidth(), diagram.getHeight()));
        return new double[]{handles, routes, shapes, outside, gap};
    }

    private static double overlap(Rectangle a, Rectangle b) {
        Rectangle intersection = a.intersection(b);
        return intersection.isEmpty() ? 0 : (double) intersection.width * intersection.height;
    }

    /** Length inside the strip, including a one-pixel margin for the painted stroke. */
    private static double clippedLength(Rectangle strip, Point a, Point b) {
        Rectangle r = new Rectangle(strip);
        r.grow(1, 1);
        if (!r.intersectsLine(new Line2D.Double(a, b))) return 0;
        double dx = b.x - a.x, dy = b.y - a.y;
        double start = 0, end = 1;
        double[] directions = {-dx, dx, -dy, dy};
        double[] distances = {a.x - r.x, r.x + r.width - a.x, a.y - r.y, r.y + r.height - a.y};
        for (int i = 0; i < 4; i++) {
            if (directions[i] == 0) {
                if (distances[i] < 0) return 0;
            } else {
                double t = distances[i] / directions[i];
                if (directions[i] < 0) start = Math.max(start, t);
                else end = Math.min(end, t);
            }
        }
        return Math.max(0, end - start) * Math.hypot(dx, dy);
    }

    private static boolean better(double[] candidate, double[] best) {
        for (int i = 0; i < candidate.length; i++) {
            int comparison = Double.compare(candidate[i], best[i]);
            if (comparison != 0) return comparison < 0;
        }
        return false;
    }
}

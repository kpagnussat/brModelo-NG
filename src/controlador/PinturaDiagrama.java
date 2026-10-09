package controlador;

import desenho.Elementar;
import desenho.FormaElementar;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.RenderingHints;

/** Paints grid, print guides and external output; the diagram owns its painting flag. */
final class PinturaDiagrama {
    private PinturaDiagrama() {}

    static void PinteGrade(Diagrama diagrama, Graphics2D g) {
        int w = diagrama.master.getGridWidth();
        int ww = diagrama.getWidth();
        int hh = diagrama.getHeight();
        // Only the lines inside the repaint area: the page is 4096 px wide, and drawing every
        // full-length line on each repaint was most of the canvas cost while resizing.
        Rectangle area = g.getClipBounds();
        if (area == null) area = new Rectangle(0, 0, ww, hh);
        int left = Math.max(0, area.x), right = Math.min(ww, area.x + area.width + 1);
        int top = Math.max(0, area.y), bottom = Math.min(hh, area.y + area.height + 1);

        Paint bkppaint = g.getPaint();
        Object antialias = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        // Axis-aligned hairlines gain nothing from antialiasing, which only made them slower.
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setColor(new Color(241, 246, 251));

        for (int x = Math.max(1, left / w) * w; x <= right && x <= ww; x += w) {
            g.drawLine(x, top, x, bottom);
        }

        for (int y = Math.max(1, top / w) * w; y <= bottom && y <= hh; y += w) {
            g.drawLine(left, y, right, y);
        }

        if (antialias != null) g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, antialias);
        g.setPaint(bkppaint);
    }

    static void PaintAI(Diagrama diagrama, Graphics2D g, int wdt, int ht) {
        Paint bkppaint = g.getPaint();
        g.setColor(new Color(221, 221, 221));
        int w = wdt;
        while (w < diagrama.getWidth()) {
            g.drawLine(w, 1, w, diagrama.getHeight() - 1);
            w += wdt;
        }
        int h = ht;
        while (h < diagrama.getHeight()) {
            g.drawLine(1, h, diagrama.getWidth() - 1, h);
            h += ht;
        }
        g.setPaint(bkppaint);
    }

    static void ExternalPaint(Diagrama diagrama, Graphics g) {
        RenderingHints renderHints
                = new RenderingHints(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
        renderHints.put(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);

        renderHints.put(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        Graphics2D Canvas = (Graphics2D) g;

        Canvas.addRenderingHints(renderHints);

        Canvas.setStroke(new BasicStroke(
                1f,
                BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND));

        Canvas.setPaint(Color.BLACK);

        for (int i = diagrama.subItens.size() - 1; i > -1; i--) {
            Elementar e = diagrama.subItens.get(i);
            if (e.CanPaint()) {
                e.DoPaint(Canvas);
            }
        }
    }

    static void ExternalPaintSelecao(Diagrama diagrama, Graphics g) {
        RenderingHints renderHints
                = new RenderingHints(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
        renderHints.put(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);

        renderHints.put(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        Graphics2D Canvas = (Graphics2D) g;

        Canvas.addRenderingHints(renderHints);

        Canvas.setStroke(new BasicStroke(
                1f,
                BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND));

        Canvas.setPaint(Color.BLACK);

        for (int i = diagrama.getItensSelecionados().size() - 1; i > -1; i--) {
            FormaElementar e = diagrama.getItensSelecionados().get(i);
            e.HidePontos(true);
            if (e.CanPaint()) {
                e.DoPaint(Canvas);
            }
            e.HidePontos(false);
        }
    }
}

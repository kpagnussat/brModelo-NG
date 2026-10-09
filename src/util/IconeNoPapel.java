package util;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.parser.LoaderContext;
import com.github.weisj.jsvg.parser.SVGLoader;
import com.github.weisj.jsvg.view.ViewBox;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.swing.Icon;

/** Vector-only paper renderer: no FlatLaf theme filter, raster cache or UIScale multiplier. */
final class IconeNoPapel implements Icon {
    private static final Map<String, SVGDocument> DOCUMENTOS = new ConcurrentHashMap<>();
    private final SVGDocument documento;
    private final int tamanho;

    private IconeNoPapel(SVGDocument documento, int tamanho) {
        this.documento = documento;
        this.tamanho = tamanho;
    }

    static Icon de(String base, int tamanho, Color tinta) {
        if (base == null) return null;
        String chave = base + (tinta == null ? "" : ":" + tinta.getRGB());
        SVGDocument doc = DOCUMENTOS.computeIfAbsent(chave, ignored -> carregue(base, tinta));
        return doc == null ? null : new IconeNoPapel(doc, tamanho);
    }

    private static SVGDocument carregue(String base, Color tinta) {
        var url = IconeNoPapel.class.getResource("/imagens/svg/" + base + ".svg");
        if (url == null) return null;
        if (tinta == null) return new SVGLoader().load(url);
        // Matches the old disabled-marker dye, but applies it to vector ink before rendering.
        try (var in = url.openStream()) {
            String svg = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            svg = svg.replaceAll("#[0-9a-fA-F]{6}", String.format("#%06x", tinta.getRGB() & 0xffffff));
            return new SVGLoader().load(new ByteArrayInputStream(svg.getBytes(StandardCharsets.UTF_8)),
                    url.toURI(), LoaderContext.createDefault());
        } catch (IOException | java.net.URISyntaxException e) {
            throw new IllegalStateException("Cannot load paper icon " + base, e);
        }
    }

    @Override public int getIconWidth() { return tamanho; }
    @Override public int getIconHeight() { return tamanho; }

    @Override public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D canvas = (Graphics2D) g.create();
        try {
            canvas.translate(x, y);
            canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            documento.render(null, canvas, new ViewBox(tamanho, tamanho));
        } finally {
            canvas.dispose();
        }
    }
}

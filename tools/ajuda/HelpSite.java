import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;
import org.commonmark.node.*;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

/** Build-only CommonMark compiler: no timestamps, absolute paths or network dependencies. */
public final class HelpSite {
    private HelpSite() {}
    private static final Parser PARSER = Parser.builder().build();
    private static final Pattern REFERENCE = Pattern.compile("(?:href|src)=\"([^\"]+)\"");
    public static final String SINGLE_FILE = "ajuda-completa.html";
    private record Topic(String source, String title, Node document) {
        String html() { return source.replaceFirst("\\.md$", ".html"); }
        String key() { return source.replaceFirst("\\.md$", ""); }
        String section() { return "topico-" + key(); }
        String anchor(String heading) { return key() + "--" + heading; }
    }
    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
    private static String text(Node node) {
        StringBuilder result = new StringBuilder();
        node.accept(new AbstractVisitor() {
            @Override public void visit(Text value) { result.append(value.getLiteral()); }
            @Override public void visit(Code value) { result.append(value.getLiteral()); }
        });
        return result.toString();
    }
    public static void build(Path source, Path output) throws Exception {
        source = source.toAbsolutePath().normalize();
        output = output.toAbsolutePath().normalize();
        if (output.startsWith(source) || source.startsWith(output)) throw new IOException("Output must be separate from sources");
        List<String> order = new ArrayList<>(List.of("index.md"));
        Node index = PARSER.parse(Files.readString(source.resolve("index.md")));
        index.accept(new AbstractVisitor() {
            @Override public void visit(Link link) {
                if (link.getDestination().endsWith(".md")) order.add(link.getDestination());
                visitChildren(link);
            }
        });
        if (order.size() < 2 || new HashSet<>(order).size() != order.size()) throw new IOException("Invalid help index order");
        List<Topic> topics = new ArrayList<>();
        Map<Node, String> anchors = new IdentityHashMap<>();
        for (String file : order) {
            if (!file.matches("[a-z0-9-]+\\.md")) throw new IOException("Invalid topic: " + file);
            Node document = PARSER.parse(Files.readString(source.resolve(file)));
            if (!(document.getFirstChild() instanceof Heading title) || title.getLevel() != 1)
                throw new IOException("Topic needs a leading title: " + file);
            Map<String, Integer> used = new HashMap<>();
            document.accept(new AbstractVisitor() {
                @Override public void visit(Heading heading) {
                    // GitHub-compatible heading slugs, including accented Portuguese letters.
                    String base = Normalizer.normalize(text(heading).toLowerCase(Locale.ROOT), Normalizer.Form.NFC)
                            .replaceAll("[^\\p{L}\\p{N}_ -]", "").replace(' ', '-');
                    int count = used.merge(base, 1, Integer::sum);
                    anchors.put(heading, base + (count == 1 ? "" : "-" + (count - 1)));
                }
                @Override public void visit(Link link) {
                    String dest = link.getDestination();
                    if (dest.matches("[^:#?]+\\.md(?:#.*)?")) link.setDestination(dest.replaceFirst("\\.md(?=#|$)", ".html"));
                    visitChildren(link);
                }
            });
            topics.add(new Topic(file, text(title), document));
        }
        try (var files = Files.list(source)) {
            if (!files.filter(p -> p.toString().endsWith(".md")).map(p -> p.getFileName().toString()).sorted().toList()
                    .equals(order.stream().sorted().toList())) throw new IOException("Every Markdown topic must appear in index.md");
        }
        if (Files.exists(output)) try (var files = Files.walk(output)) {
            for (Path file : files.sorted(Comparator.reverseOrder()).toList()) Files.delete(file);
        }
        Files.createDirectories(output);
        Files.copy(source.resolve("ajuda.css"), output.resolve("ajuda.css"));
        Path images = source.resolve("img");
        try (var files = Files.walk(images)) {
            for (Path file : files.filter(Files::isRegularFile).sorted().toList()) {
                Path target = output.resolve(source.relativize(file));
                Files.createDirectories(target.getParent()); Files.copy(file, target);
            }
        }
        HtmlRenderer renderer = HtmlRenderer.builder().escapeHtml(true).sanitizeUrls(true)
                .attributeProviderFactory(context -> (node, tag, attributes) -> {
                    if (node instanceof Heading) attributes.put("id", anchors.get(node));
                }).build();
        for (int i = 0; i < topics.size(); i++) {
            Topic topic = topics.get(i);
            StringBuilder nav = new StringBuilder("<a class=\"brand\" href=\"index.html\">brModelo NG <span>Ajuda</span></a><ol>");
            for (Topic item : topics) nav.append("<li><a ").append(item == topic ? "aria-current=\"page\" " : "")
                    .append("href=\"").append(item.html()).append("\">").append(escape(item.title())).append("</a></li>");
            nav.append("</ol>");
            StringBuilder toc = new StringBuilder("<details open><summary>Nesta página</summary><ul>");
            for (Node node = topic.document().getFirstChild(); node != null; node = node.getNext()) {
                if (node instanceof Heading heading && heading.getLevel() == 2)
                    toc.append("<li><a href=\"#").append(anchors.get(node)).append("\">").append(escape(text(node))).append("</a></li>");
            }
            toc.append("</ul></details>");
            String previous = i == 0 ? "<span></span>" : "<a rel=\"prev\" href=\"" + topics.get(i - 1).html() + "\">← " + escape(topics.get(i - 1).title()) + "</a>";
            String next = i == topics.size() - 1 ? "" : "<a rel=\"next\" href=\"" + topics.get(i + 1).html() + "\">" + escape(topics.get(i + 1).title()) + " →</a>";
            Files.writeString(output.resolve(topic.html()), "<!doctype html>\n<html lang=\"pt-BR\"><head><meta charset=\"utf-8\">"
                    + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"><title>" + escape(topic.title())
                    + " · Ajuda brModelo NG</title><link rel=\"stylesheet\" href=\"ajuda.css\"></head><body>"
                    + "<a class=\"skip\" href=\"#conteudo\">Ir para o conteúdo</a><aside><nav aria-label=\"Tópicos\">" + nav
                    + "</nav></aside><main id=\"conteudo\"><header>MANUAL DO ESTUDANTE · BRMODELO NG</header>"
                    + toc + "<article>" + renderer.render(topic.document()) + "</article><nav class=\"pager\" aria-label=\"Anterior e próximo\">"
                    + previous + next + "</nav><footer>Ajuda local · GPL-3.0 · brModelo NG</footer></main></body></html>\n", StandardCharsets.UTF_8);
        }
        buildSingleFile(source, output, topics, anchors);
        validate(output);
        // Manifest also identifies the content-addressed runtime cache; it lists all resources.
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        StringBuilder manifest = new StringBuilder();
        try (var files = Files.walk(output)) {
            for (Path file : files.filter(Files::isRegularFile).sorted().toList()) {
                String relative = output.relativize(file).toString().replace('\\', '/');
                manifest.append(HexFormat.of().formatHex(digest.digest(Files.readAllBytes(file)))).append(' ').append(relative).append('\n');
            }
        }
        Files.writeString(output.resolve("manifest.sha256"), manifest);
    }
    private static void buildSingleFile(Path source, Path output, List<Topic> topics, Map<Node, String> anchors) throws IOException {
        Map<String, Topic> byPage = new HashMap<>();
        Map<Node, String> images = new IdentityHashMap<>();
        for (Topic topic : topics) byPage.put(topic.html(), topic);
        for (Topic topic : topics) {
            List<Image> embedded = new ArrayList<>();
            topic.document().accept(new AbstractVisitor() {
                @Override public void visit(Image image) { embedded.add(image); }
            });
            for (Image image : embedded) {
                URI uri = URI.create(image.getDestination());
                if (uri.isAbsolute() || uri.getAuthority() != null || uri.getQuery() != null || uri.getFragment() != null)
                    throw new IOException("Invalid help image: " + uri);
                Path file = source.resolve(uri.getPath()).normalize();
                if (!file.startsWith(source) || !file.toString().endsWith(".png")) throw new IOException("Invalid help image: " + uri);
                images.put(image, "data:image/png;base64," + Base64.getEncoder().encodeToString(Files.readAllBytes(file)));
            }
        }
        StringBuilder nav = new StringBuilder("<a class=\"brand\" href=\"#" + topics.getFirst().section()
                + "\">brModelo NG <span>Ajuda</span></a><ol>");
        for (Topic topic : topics) nav.append("<li><a href=\"#").append(topic.section()).append("\">")
                .append(escape(topic.title())).append("</a></li>");
        nav.append("</ol>");
        StringBuilder body = new StringBuilder();
        for (int i = 0; i < topics.size(); i++) {
            Topic topic = topics.get(i);
            HtmlRenderer renderer = HtmlRenderer.builder().escapeHtml(true).sanitizeUrls(true)
                    .attributeProviderFactory(context -> (node, tag, attributes) -> {
                        if (node instanceof Heading) attributes.put("id", topic.anchor(anchors.get(node)));
                        if (node instanceof Image) attributes.put("src", images.get(node));
                        if (node instanceof Link link) {
                            URI uri = URI.create(link.getDestination());
                            // Web links (credits, releases) stay clickable: the browser only follows
                            // them on a click, nothing is loaded. Everything the page shows is inline.
                            if (uri.isAbsolute()) {
                                if (!"https".equals(uri.getScheme())) throw new IllegalArgumentException("Invalid help link: " + uri);
                                return;
                            }
                            Topic target = uri.getPath().isEmpty() ? topic : byPage.get(uri.getPath());
                            if (target == null || uri.getQuery() != null) throw new IllegalArgumentException("Invalid help link: " + uri);
                            attributes.put("href", "#" + (uri.getFragment() == null ? target.section() : target.anchor(uri.getFragment())));
                        }
                    }).build();
            body.append("<section id=\"").append(topic.section()).append("\"><details open><summary>")
                    .append(escape(topic.title())).append("</summary><ul>");
            for (Node node = topic.document().getFirstChild(); node != null; node = node.getNext()) {
                if (node instanceof Heading heading && heading.getLevel() == 2)
                    body.append("<li><a href=\"#").append(topic.anchor(anchors.get(node))).append("\">")
                            .append(escape(text(node))).append("</a></li>");
            }
            body.append("</ul></details><article>").append(renderer.render(topic.document()))
                    .append("</article><nav class=\"pager\" aria-label=\"Anterior e próximo\">");
            if (i == 0) body.append("<span></span>");
            else body.append("<a rel=\"prev\" href=\"#").append(topics.get(i - 1).section()).append("\">← ")
                    .append(escape(topics.get(i - 1).title())).append("</a>");
            if (i + 1 < topics.size()) body.append("<a rel=\"next\" href=\"#").append(topics.get(i + 1).section()).append("\">")
                    .append(escape(topics.get(i + 1).title())).append(" →</a>");
            body.append("</nav></section>");
        }
        Files.writeString(output.resolve(SINGLE_FILE), "<!doctype html>\n<html lang=\"pt-BR\"><head><meta charset=\"utf-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"><title>Ajuda brModelo NG</title><style>"
                + Files.readString(source.resolve("ajuda.css")) + "</style></head><body>"
                + "<a class=\"skip\" href=\"#conteudo\">Ir para o conteúdo</a><aside><nav aria-label=\"Tópicos\">" + nav
                + "</nav></aside><main id=\"conteudo\"><header>MANUAL DO ESTUDANTE · BRMODELO NG</header>"
                + body + "<footer>Ajuda local · GPL-3.0 · brModelo NG</footer></main></body></html>\n", StandardCharsets.UTF_8);
        validateSingleFile(output.resolve(SINGLE_FILE));
    }
    public static void validateSingleFile(Path page) throws IOException {
        String html = Files.readString(page);
        if (Pattern.compile("(?i)(?:\\bfile:|\\burl\\s*\\(|@import\\b|<(?:script|link|iframe|object|embed)\\b)").matcher(html).find())
            throw new IOException("External reference in single-file help");
        Set<String> ids = new HashSet<>();
        var identifiers = Pattern.compile("\\bid=\"([^\"]+)\"").matcher(html);
        while (identifiers.find()) if (!ids.add(identifiers.group(1))) throw new IOException("Duplicate help anchor: " + identifiers.group(1));
        var references = REFERENCE.matcher(html);
        while (references.find()) {
            String value = references.group(1);
            if (references.group().startsWith("href=")) {
                if (value.startsWith("https://") && URI.create(value).getHost() != null) continue; // followed only on a click
                if (!value.startsWith("#") || !ids.contains(URI.create(value).getFragment())) throw new IOException("Broken single-file anchor: " + value);
            } else {
                if (!value.startsWith("data:image/png;base64,")) throw new IOException("Non-embedded help image: " + value);
                try {
                    if (Base64.getDecoder().decode(value.substring("data:image/png;base64,".length())).length == 0)
                        throw new IllegalArgumentException("Empty image");
                } catch (IllegalArgumentException e) { throw new IOException("Invalid embedded image", e); }
            }
        }
    }
    public static void validate(Path output) throws IOException {
        try (var files = Files.walk(output)) {
            for (Path page : files.filter(p -> p.toString().endsWith(".html")).toList()) {
                String html = Files.readString(page);
                if (page.getFileName().toString().equals(SINGLE_FILE)) validateSingleFile(page);
                if (html.contains("sis4") || html.contains("3.32")) throw new IOException("Legacy content: " + page);
                var matches = REFERENCE.matcher(html);
                while (matches.find()) {
                    URI uri = URI.create(matches.group(1).replace("&amp;", "&"));
                    if (uri.isAbsolute()) continue;
                    if (uri.getAuthority() != null || uri.getQuery() != null) throw new IOException("Invalid internal URL: " + uri);
                    Path target = uri.getPath().isEmpty() ? page : page.getParent().resolve(uri.getPath()).normalize();
                    if (!target.startsWith(output) || !Files.isRegularFile(target)) throw new IOException("Broken link: " + page + " → " + uri);
                    if (uri.getFragment() != null && !Files.readString(target).contains("id=\"" + uri.getFragment() + "\""))
                        throw new IOException("Broken anchor: " + uri);
                }
            }
        }
    }
    public static void main(String[] args) throws Exception { build(Path.of(args[0]), Path.of(args[1])); }
}

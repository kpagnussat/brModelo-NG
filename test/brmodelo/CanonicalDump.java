package brmodelo;

import controlador.Diagrama;
import desenho.Elementar;
import desenho.FormaElementar;
import desenho.formas.Forma;
import desenho.linhas.Linha;
import desenho.linhas.PontoDeLinha;
import diagramas.logico.*;
import java.nio.file.*;
import java.util.*;

/** Stable semantic dump; excludes UUIDs, editor state and lazily calculated paint caches. */
public final class CanonicalDump {
    private CanonicalDump() {}

    private static final List<String> FLAGS = List.of(
            "isIdentificador", "isOpcional", "isMultivalorado", "getCardMinima", "getCardMaxima",
            "getCard", "getPapel", "isTotal", "isExclusiva", "isNaoExclusiva", "isEntidadeFraca",
            "isDuplaLinha", "isDashed", "isInteligente", "isTemSetaPontaA", "isTemSetaPontaB",
            "isAutosize", "getTextoAdicional", "isCentrarTexto", "getDirecao", "getTipo");

    private static String quote(Object value) {
        return String.valueOf(value).replace("\\", "\\\\").replace("\n", "\\n").replace("\r", "\\r");
    }

    private static void index(Elementar e, String path, IdentityHashMap<Object, String> ids) {
        if (ids.putIfAbsent(e, path) != null) return;
        if (e instanceof FormaElementar shape && shape.getSubItens() != null) {
            int n = 0;
            for (Elementar child : shape.getSubItens()) index(child, path + "/" + n++, ids);
        }
        if (e instanceof Tabela table) {
            for (int i = 0; i < table.getCampos().size(); i++) ids.put(table.getCampos().get(i), path + "/field" + i);
            for (int i = 0; i < table.getConstraints().size(); i++) ids.put(table.getConstraints().get(i), path + "/constraint" + i);
        }
    }

    private static String ref(Object o, IdentityHashMap<Object, String> ids) {
        if (o == null) return "null";
        String id = ids.get(o);
        if (id == null) throw new IllegalStateException("Unindexed model reference: " + o.getClass());
        return id;
    }

    private static void element(Elementar e, StringBuilder out, IdentityHashMap<Object, String> ids,
                                Set<Elementar> visited) throws Exception {
        if (!visited.add(e)) return;
        out.append(ref(e, ids)).append(' ').append(e.getClass().getName()).append(' ').append(e.getBounds());
        if (e instanceof Forma f) out.append(" text=").append(quote(f.getTexto()));
        var font = e.getFont();
        out.append(" font=").append(font == null ? "null" : font.getName() + "/" + font.getStyle() + "/" + font.getSize2D())
                .append(" ink=").append(e.getForeColor()).append(" background=").append(e.getBackColor());
        for (String getter : FLAGS) {
            try {
                var method = e.getClass().getMethod(getter);
                Object value = method.invoke(e);
                out.append(' ').append(getter).append('=').append(value instanceof Elementar ? ref(value, ids) : quote(value));
            } catch (NoSuchMethodException ignored) {
                // Different diagram types expose different document properties.
            }
        }
        if (e instanceof Linha line) {
            out.append(" ends=").append(ref(line.getFormaPontaA(), ids)).append(',').append(ref(line.getFormaPontaB(), ids));
            out.append(" points=");
            for (PontoDeLinha p : line.getPontos()) out.append(p.getCentro()).append('@').append(ref(p.getEm(), ids)).append(';');
        }
        out.append('\n');
        if (e instanceof Tabela table) {
            for (Campo field : table.getCampos()) {
                out.append(ref(field, ids)).append(" text=").append(quote(field.getTexto()))
                        .append(" type=").append(quote(field.getTipo())).append(" PK=").append(field.isKey())
                        .append(" FK=").append(field.isFkey()).append(" unique=").append(field.isUnique())
                        .append(" separator=").append(field.isSeparador())
                        .append(" source=").append(ref(field.getCampoOrigem(), ids))
                        .append(" extra=").append(quote(field.getComplemento()))
                        .append(" notes=").append(quote(field.getObservacao())).append('\n');
            }
            for (Constraint c : table.getConstraints()) {
                out.append(ref(c, ids)).append(" type=").append(c.getTipo()).append(" name=").append(quote(c.getNome()))
                        .append(" named=").append(c.isNomeada()).append(" link=").append(ref(c.getLigacao(), ids))
                        .append(" source=").append(ref(c.getConstraintOrigem(), ids)).append(" fields=");
                for (Campo f : c.getCamposDeOrigem()) out.append(ref(f, ids)).append(';');
                out.append(" -> ");
                for (Campo f : c.getCamposDeDestino()) out.append(ref(f, ids)).append(';');
                out.append('\n');
            }
        }
        if (e instanceof FormaElementar f && f.getSubItens() != null) {
            for (Elementar child : f.getSubItens()) element(child, out, ids, visited);
        }
    }

    public static String dump(Diagrama d) throws Exception {
        IdentityHashMap<Object, String> ids = new IdentityHashMap<>();
        for (int i = 0; i < d.getSubItens().size(); i++) index(d.getSubItens().get(i), "element" + i, ids);
        StringBuilder out = new StringBuilder(d.getClass().getName() + " type=" + d.getTipo() + "\n");
        Set<Elementar> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Elementar e : d.getSubItens()) element(e, out, ids, visited);
        return out.toString();
    }

    /** Explicit baseline writer; normal tests only read the committed dumps. */
    public static void main(String[] args) throws Exception {
        for (String name : Fixtures.names().toList()) {
            Path file = Path.of("test-resources/fixtures", name + ".txt");
            Files.writeString(file, dump(Fixtures.load(name)));
            System.out.println("Wrote " + file);
        }
    }
}

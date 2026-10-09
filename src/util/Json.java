package util;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Small strict JSON syntax codec. Model mapping and class resolution live elsewhere. */
public final class Json {
    private Json() {}

    public static Object ler(String text) throws IOException {
        Parser parser = new Parser(text);
        Object value = parser.value(0);
        parser.space();
        if (parser.pos != text.length()) throw parser.error("Trailing input");
        return value;
    }

    public static String escrever(Object value) throws IOException {
        StringBuilder out = new StringBuilder();
        write(value, out, 0);
        return out.append('\n').toString();
    }

    private static void indent(StringBuilder out, int depth) { out.append("  ".repeat(depth)); }

    private static void string(String value, StringBuilder out) {
        out.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    // Escaping surrogates also preserves unpaired UTF-16 code units.
                    if (c < 32 || Character.isSurrogate(c)) out.append(String.format("\\u%04x", (int) c));
                    else out.append(c);
                }
            }
        }
        out.append('"');
    }

    private static void write(Object value, StringBuilder out, int depth) throws IOException {
        if (depth > 128) throw new IOException("JSON nesting exceeds 128");
        if (value == null) out.append("null");
        else if (value instanceof String s) string(s, out);
        else if (value instanceof Boolean) out.append(value);
        else if (value instanceof Number n) {
            if (n instanceof Double d && !Double.isFinite(d)
                    || n instanceof Float f && !Float.isFinite(f)) throw new IOException("Non-finite JSON number");
            out.append(n);
        } else if (value instanceof Map<?, ?> map) {
            TreeMap<String, Object> sorted = new TreeMap<>();
            for (var entry : map.entrySet()) {
                if (!(entry.getKey() instanceof String s)) throw new IOException("JSON key must be a string");
                sorted.put(s, entry.getValue());
            }
            out.append('{');
            int count = 0;
            for (var entry : sorted.entrySet()) {
                out.append(count++ == 0 ? "\n" : ",\n"); indent(out, depth + 1);
                string(entry.getKey(), out); out.append(": "); write(entry.getValue(), out, depth + 1);
            }
            if (!map.isEmpty()) { out.append('\n'); indent(out, depth); }
            out.append('}');
        } else if (value instanceof List<?> list) {
            out.append('[');
            for (int i = 0; i < list.size(); i++) {
                out.append(i == 0 ? "\n" : ",\n"); indent(out, depth + 1); write(list.get(i), out, depth + 1);
            }
            if (!list.isEmpty()) { out.append('\n'); indent(out, depth); }
            out.append(']');
        } else throw new IOException("Unsupported JSON syntax value: " + value.getClass().getName());
    }

    private static final class Parser {
        final String text;
        int pos;
        Parser(String text) { this.text = text; }
        IOException error(String message) { return new IOException(message + " at JSON offset " + pos); }
        void space() { while (pos < text.length() && " \t\r\n".indexOf(text.charAt(pos)) >= 0) pos++; }
        boolean take(char c) { if (pos < text.length() && text.charAt(pos) == c) { pos++; return true; } return false; }
        void require(char c) throws IOException { if (!take(c)) throw error("Expected '" + c + "'"); }
        Object value(int depth) throws IOException {
            if (depth > 128) throw error("JSON nesting exceeds 128");
            space();
            if (pos == text.length()) throw error("Expected value");
            char c = text.charAt(pos);
            if (c == '"') return string();
            if (take('{')) {
                Map<String, Object> map = new LinkedHashMap<>();
                space(); if (take('}')) return map;
                do {
                    space(); String key = string(); space(); require(':');
                    if (map.containsKey(key)) throw error("Duplicate key: " + key);
                    map.put(key, value(depth + 1)); space();
                    if (take('}')) return map;
                    require(',');
                } while (true);
            }
            if (take('[')) {
                List<Object> list = new ArrayList<>(); space(); if (take(']')) return list;
                do {
                    list.add(value(depth + 1)); space(); if (take(']')) return list; require(',');
                } while (true);
            }
            for (String literal : List.of("null", "true", "false")) {
                if (text.startsWith(literal, pos)) {
                    pos += literal.length(); return literal.equals("null") ? null : Boolean.valueOf(literal);
                }
            }
            int start = pos;
            take('-');
            if (!take('0')) { digits(true); }
            if (take('.')) digits(true);
            if (take('e') || take('E')) { if (!take('+')) take('-'); digits(true); }
            if (pos == start) throw error("Expected number");
            try {
                String token = text.substring(start, pos);
                BigDecimal number = new BigDecimal(token);
                if (token.startsWith("-") && number.signum() == 0 && (token.contains(".") || token.contains("e") || token.contains("E"))) return -0.0d;
                return number;
            }
            catch (NumberFormatException e) { throw error("Invalid number"); }
        }
        void digits(boolean required) throws IOException {
            int start = pos;
            while (pos < text.length() && text.charAt(pos) >= '0' && text.charAt(pos) <= '9') pos++;
            if (required && start == pos) throw error("Expected digit");
        }
        String string() throws IOException {
            require('"'); StringBuilder out = new StringBuilder();
            while (pos < text.length()) {
                char c = text.charAt(pos++);
                if (c == '"') return out.toString();
                if (c < 32) throw error("Control character in string");
                if (c == '\\') {
                    if (pos == text.length()) throw error("Incomplete escape");
                    char escape = text.charAt(pos++);
                    switch (escape) {
                        case '"', '\\', '/' -> out.append(escape);
                        case 'b' -> out.append('\b'); case 'f' -> out.append('\f');
                        case 'n' -> out.append('\n'); case 'r' -> out.append('\r'); case 't' -> out.append('\t');
                        case 'u' -> {
                            if (pos + 4 > text.length()) throw error("Incomplete Unicode escape");
                            int code = 0;
                            for (int i = 0; i < 4; i++) {
                                char hex = text.charAt(pos++);
                                int digit = hex <= 127 ? Character.digit(hex, 16) : -1;
                                if (digit < 0) throw error("Invalid Unicode escape");
                                code = code * 16 + digit;
                            }
                            out.append((char) code);
                        }
                        default -> throw error("Invalid escape");
                    }
                } else out.append(c);
            }
            throw error("Unterminated string");
        }
    }
}

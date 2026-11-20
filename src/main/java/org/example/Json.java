package org.example;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal JSON reader/writer so the project has no external dependencies.
 * parse() returns Map (object), List (array), String, Double, Boolean or null.
 * Bad input throws IllegalArgumentException.
 */
public final class Json {
    private final String text;
    private int pos;

    private Json(String text) {
        this.text = text;
    }

    public static Object parse(String text) {
        if (text == null) throw new IllegalArgumentException("JSON text is null");
        Json p = new Json(text);
        p.skipSpace();
        Object value = p.readValue();
        p.skipSpace();
        if (p.pos != text.length()) throw p.fail("Unexpected text after JSON value");
        return value;
    }

    /** Returns the text as a quoted JSON string. */
    public static String quote(String s) {
        StringBuilder out = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) out.append(String.format("\\u%04x", (int) c));
                    else out.append(c);
                }
            }
        }
        return out.append('"').toString();
    }

    private Object readValue() {
        if (pos >= text.length()) throw fail("Unexpected end of JSON");
        char c = text.charAt(pos);
        if (c == '{') return readObject();
        if (c == '[') return readArray();
        if (c == '"') return readString();
        if (text.startsWith("true", pos)) { pos += 4; return Boolean.TRUE; }
        if (text.startsWith("false", pos)) { pos += 5; return Boolean.FALSE; }
        if (text.startsWith("null", pos)) { pos += 4; return null; }
        if (c == '-' || Character.isDigit(c)) return readNumber();
        throw fail("Unexpected character '" + c + "'");
    }

    private Map<String, Object> readObject() {
        Map<String, Object> map = new LinkedHashMap<>();
        pos++; // {
        skipSpace();
        if (peek() == '}') { pos++; return map; }
        while (true) {
            skipSpace();
            if (peek() != '"') throw fail("Expected a string key");
            String key = readString();
            skipSpace();
            if (peek() != ':') throw fail("Expected ':'");
            pos++;
            skipSpace();
            map.put(key, readValue());
            skipSpace();
            char c = peek();
            pos++;
            if (c == '}') return map;
            if (c != ',') throw fail("Expected ',' or '}'");
        }
    }

    private List<Object> readArray() {
        List<Object> list = new ArrayList<>();
        pos++; // [
        skipSpace();
        if (peek() == ']') { pos++; return list; }
        while (true) {
            skipSpace();
            list.add(readValue());
            skipSpace();
            char c = peek();
            pos++;
            if (c == ']') return list;
            if (c != ',') throw fail("Expected ',' or ']'");
        }
    }

    private String readString() {
        StringBuilder out = new StringBuilder();
        pos++; // opening quote
        while (true) {
            if (pos >= text.length()) throw fail("Unterminated string");
            char c = text.charAt(pos++);
            if (c == '"') return out.toString();
            if (c != '\\') { out.append(c); continue; }
            if (pos >= text.length()) throw fail("Unterminated escape");
            char e = text.charAt(pos++);
            switch (e) {
                case '"' -> out.append('"');
                case '\\' -> out.append('\\');
                case '/' -> out.append('/');
                case 'n' -> out.append('\n');
                case 'r' -> out.append('\r');
                case 't' -> out.append('\t');
                case 'b' -> out.append('\b');
                case 'f' -> out.append('\f');
                case 'u' -> {
                    if (pos + 4 > text.length()) throw fail("Bad \\u escape");
                    try {
                        out.append((char) Integer.parseInt(text.substring(pos, pos + 4), 16));
                    } catch (NumberFormatException ex) {
                        throw fail("Bad \\u escape");
                    }
                    pos += 4;
                }
                default -> throw fail("Unknown escape \\" + e);
            }
        }
    }

    private Double readNumber() {
        int start = pos;
        while (pos < text.length() && "+-0123456789.eE".indexOf(text.charAt(pos)) >= 0) pos++;
        try {
            return Double.parseDouble(text.substring(start, pos));
        } catch (NumberFormatException ex) {
            throw fail("Bad number");
        }
    }

    private char peek() {
        if (pos >= text.length()) throw fail("Unexpected end of JSON");
        return text.charAt(pos);
    }

    private void skipSpace() {
        while (pos < text.length() && Character.isWhitespace(text.charAt(pos))) pos++;
    }

    private IllegalArgumentException fail(String why) {
        return new IllegalArgumentException(why + " at position " + pos);
    }
}

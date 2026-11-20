package org.example;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Turns (level, logger name, message) into a finished line. Never prints.
 *
 * Format placeholders: %t time, %l level, %n logger name, %m message,
 * %r a random phrase for the level (Lab 1 extra credit), %% literal percent.
 * The format string is scanned once, left to right, so a "%n" that appears inside the
 * MESSAGE is never replaced with the logger name.
 */
public class Formatter {
    public static final String DEFAULT_FORMAT = "[%t] [%l] %n: %m";
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final String format;

    public Formatter() {
        this(DEFAULT_FORMAT);
    }

    public Formatter(String format) {
        this.format = (format == null || format.isEmpty()) ? DEFAULT_FORMAT : format;
    }

    public String getFormat() {
        return format;
    }

    public String format(Logger.LogLevel level, String name, String message) {
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < format.length(); i++) {
            char c = format.charAt(i);
            if (c != '%' || i + 1 >= format.length()) {
                line.append(c);
                continue;
            }
            char code = format.charAt(++i);
            switch (code) {
                case 't' -> line.append(LocalTime.now().format(TIME));
                case 'l' -> line.append(level.name());
                case 'n' -> line.append(name);
                case 'm' -> line.append(message);
                case 'r' -> line.append(level.randomMessage());
                case '%' -> line.append('%');
                default -> line.append('%').append(code); // unknown code: keep it visible
            }
        }
        return line.toString();
    }

    /** Same as above, but fills the message with the extra arguments first. */
    public String format(Logger.LogLevel level, String name, String message, Object... args) {
        return format(level, name, fill(message, args));
    }

    /**
     * Bonus task: every % in the message is replaced with the next argument, in order.
     *  - "%%" is a literal percent sign.
     *  - More % than arguments: the leftover % stay as they are.
     *  - More arguments than %: the extra arguments are ignored.
     *  - No arguments at all: the message is returned untouched (so "100%" just works).
     */
    public String fill(String message, Object... args) {
        if (message == null) return "null";
        if (args == null || args.length == 0) return message;

        StringBuilder out = new StringBuilder();
        int next = 0;
        for (int i = 0; i < message.length(); i++) {
            char c = message.charAt(i);
            if (c != '%') {
                out.append(c);
            } else if (i + 1 < message.length() && message.charAt(i + 1) == '%') {
                out.append('%');
                i++;
            } else if (next < args.length) {
                out.append(args[next++]);
            } else {
                out.append('%');
            }
        }
        return out.toString();
    }
}

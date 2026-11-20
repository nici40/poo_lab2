package org.example;

import java.util.Random;

/**
 * Has a name and filters messages by level. It does not know how a line looks
 * (Formatter) or where it goes (ConsoleSink).
 */
public class Logger {

    // order here = how serious it is, TRACE is the least, ERROR the most
    public enum LogLevel {
        TRACE("Looking at the details", "Step by step", "Following the program"),
        INFO("Operation completed", "Everything looks good", "Here is your result"),
        WARN("Something looks suspicious", "Proceed with caution", "This might be a problem"),
        ERROR("Operation failed", "Something went wrong", "Cannot accept this input");

        private final String[] randomMessages; // these go into %r

        LogLevel(String... randomMessages) {
            this.randomMessages = randomMessages;
        }

        // just picks one of the messages above at random
        public String randomMessage() {
            return randomMessages[RANDOM.nextInt(randomMessages.length)];
        }
    }

    private static final Random RANDOM = new Random();

    // still static: the minimum level is shared by all loggers
    private static LogLevel level = LogLevel.INFO;

    private final String name;
    private final Formatter formatter;
    private final ConsoleSink sink;

    /** Composition: the logger builds its own parts; they live and die with it. */
    public Logger(String name) {
        this.name = name;
        this.formatter = new Formatter();
        this.sink = new ConsoleSink();
    }

    /** Aggregation: the parts are created outside and can be shared by many loggers. */
    public Logger(String name, Formatter formatter, ConsoleSink sink) {
        if (formatter == null || sink == null) {
            throw new IllegalArgumentException("formatter and sink must not be null");
        }
        this.name = name;
        this.formatter = formatter;
        this.sink = sink;
    }

    public static void setLevel(LogLevel newLevel) {
        if (newLevel != null) level = newLevel;
    }

    public static LogLevel getLevel() {
        return level;
    }

    public String getName() {
        return name;
    }

    public void log(LogLevel msgLevel, String message, Object... args) {
        // level is checked BEFORE formatting: a filtered message costs no string work
        if (msgLevel.ordinal() < level.ordinal()) {
            return;
        }
        sink.write(formatter.format(msgLevel, name, message, args));
    }

    // shortcuts; the extra arguments fill the % in the message (bonus task)
    public void trace(String message, Object... args) { log(LogLevel.TRACE, message, args); }
    public void info(String message, Object... args)  { log(LogLevel.INFO, message, args); }
    public void warn(String message, Object... args)  { log(LogLevel.WARN, message, args); }
    public void error(String message, Object... args) { log(LogLevel.ERROR, message, args); }
}

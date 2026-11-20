package org.example;


public class Main {

    public static void main(String[] args) {
        ParenthesesValidator validator = new ParenthesesValidator();

        // the 4 tests from the handout, with everything visible
        section("Required tests, level = TRACE");
        Logger.setLevel(Logger.LogLevel.TRACE);
        test(validator, "()", true);
        test(validator, ")((()))", false);
        test(validator, "(", false);
        test(validator, "(())((()())())", true);

        // my own tests, on INFO the trace stuff should disappear but results stay the same
        section("Extra tests, level = INFO");
        Logger.setLevel(Logger.LogLevel.INFO);
        test(validator, "(())", true);
        test(validator, "(()", false);
        test(validator, "())", false);
        test(validator, "", true);


        // on ERROR only the errors should show up
        section("Level = ERROR");
        Logger.setLevel(Logger.LogLevel.ERROR);
        test(validator, "()", true);
        test(validator, "(()", false);

        // proof that level is static, i set it once and both loggers listen
        section("Static level is shared by all loggers");
        Logger first = new Logger("First");
        Logger second = new Logger("Second");
        Logger.setLevel(Logger.LogLevel.WARN);
        first.info("not printed");
        second.info("not printed");
        first.warn("printed");
        second.warn("printed");

        // ----- Lab 2, Part A -----
        Logger.setLevel(Logger.LogLevel.TRACE);

        // A1: the formatter alone. It returns a string and never prints, so main prints it
        section("Lab 2 A1: Formatter alone");
        String[] formats = {"[%l] %m", "%n: %m", "[%t] %m", "%l - %n - %m", "[%l] %n: %r"};
        for (String f : formats) {
            Formatter formatter = new Formatter(f);
            System.out.println(formatter.format(Logger.LogLevel.INFO, "Parentheses", "Valid input"));
        }
        // %n inside the MESSAGE is not replaced with the logger name
        System.out.println(new Formatter("[%l] %n: %m").format(Logger.LogLevel.INFO, "Parser", "Ratio is 100%n"));

        // A2: aggregation, one formatter and one sink shared by two loggers
        section("Lab 2 A2: shared formatter and sink (aggregation)");
        Formatter shared = new Formatter("[%t] [%l] %n: %m");
        ConsoleSink console = new ConsoleSink();
        Logger parser = new Logger("Parser", shared, console);
        Logger manager = new Logger("Manager", shared, console);
        Logger special = new Logger("Special", new Formatter("%l >> %m"), console);
        parser.info("same format as Manager");
        manager.info("same format as Parser");
        special.info("my own format, same sink");

        // A2: composition, the logger builds its own default formatter and sink
        section("Lab 2 A2: default parts (composition)");
        Logger own = new Logger("Own");
        own.info("built my own formatter and sink");

        // bonus: % in the message is filled with the extra arguments
        section("Lab 2 bonus: varargs");
        Logger demo = new Logger("Parentheses", new Formatter("[%l] %n: %m"), console);
        demo.info("Pair found at % and %", 3, 7);
        demo.info("Three slots, one value: % % %", 1);          // leftover % stay
        demo.info("One slot, three values: %", 1, 2, 3);       // extra values ignored
        demo.info("Literal percent: %% done, value %", 50);
    }

    // runs one test and says PASS or FAIL
    private static void test(ParenthesesValidator validator, String input, boolean expected) {
        boolean actual = validator.isValid(input);
        String status = (actual == expected) ? "PASS" : "FAIL";
        System.out.println(status + ": \"" + input + "\" -> " + actual + " (expected " + expected + ")");
        System.out.println();
    }

    private static void section(String title) {
        System.out.println("===== " + title + " =====");
    }
}

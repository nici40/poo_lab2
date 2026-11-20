package org.example;

/**
 * Plain-Java checks (no test framework needed). Run: java org.example.SelfTest
 * Exits with code 1 if any check fails.
 */
public class SelfTest {
    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) throws Exception {
        Logger.setLevel(Logger.LogLevel.TRACE);

        section("Formatter");
        Formatter f = new Formatter("[%l] %n: %m");
        check("basic line", f.format(Logger.LogLevel.INFO, "Parser", "hello"), "[INFO] Parser: hello");
        check("%n inside message is kept", f.format(Logger.LogLevel.INFO, "Parser", "Ratio is 100%n"), "[INFO] Parser: Ratio is 100%n");
        check("varargs in order", f.fill("Pair found at % and %", 3, 7), "Pair found at 3 and 7");
        check("more % than args", f.fill("% and %", 1), "1 and %");
        check("more args than %", f.fill("only %", 1, 2), "only 1");
        check("%% is literal", f.fill("100%% of %", "it"), "100% of it");
        check("no args leaves message alone", f.fill("100% sure"), "100% sure");
        check("%% in format", new Formatter("%% %m").format(Logger.LogLevel.WARN, "X", "m"), "% m");

        section("Memory");
        Memory small = Memory.tier1();
        for (int i = 1; i <= 3; i++) {
            small.add(new Message("user", "q" + i));
            small.add(new Message("assistant", "a" + i));
        }
        check("tier1 keeps capacity", small.size(), 4);
        check("oldest exchange dropped whole", small.getMessages().get(0).toString(), "user: q2");
        check("tiers grow", Memory.tier1().getCapacity() < Memory.tier2().getCapacity()
                && Memory.tier2().getCapacity() < Memory.tier3().getCapacity(), true);

        section("Toolbox");
        Toolbox tools = new Toolbox();
        check("calculator multiply", tools.run("calculator", "12 * 7"), "84");
        check("calculator divide", tools.run("calculator", "1 / 4"), "0.25");
        check("calculator bad input", tools.run("calculator", "twelve").startsWith("ERROR"), true);
        check("division by zero", tools.run("calculator", "1 / 0").startsWith("ERROR"), true);
        check("unknown tool", tools.run("weather", "x").startsWith("ERROR"), true);
        int roll = Integer.parseInt(tools.run("roll_dice", "6"));
        check("roll_dice in range", roll >= 1 && roll <= 6, true);
        check("get_time shape", tools.run("get_time", "").matches("\\d\\d:\\d\\d:\\d\\d"), true);
        check("get_date not empty", !tools.run("get_date", "").isEmpty(), true);

        section("Json");
        check("parse nested", Json.parse("{\"a\":[1,{\"b\":\"x\\ny\"}]}").toString(), "{a=[1.0, {b=x\ny}]}");
        check("quote", Json.quote("a\"b\n"), "\"a\\\"b\\n\"");
        boolean rejected = false;
        try { Json.parse("{\"a\":1} extra"); } catch (IllegalArgumentException e) { rejected = true; }
        check("trailing text rejected", rejected, true);

        section("Agent against the fake server");
        FakeLMServer fake = new FakeLMServer(0); // 0 = any free port
        fake.start();
        String url = "http://localhost:" + fake.getPort();
        Formatter shared = new Formatter("[%l] %n: %m");
        ConsoleSink sink = new ConsoleSink();
        try {
            Agent agent = new Agent("Barbossa", "a grumpy pirate", Memory.tier2(),
                    new LMStudioClient(url, "fake"), new Logger("Agent", shared, sink));
            check("plain answer", agent.chat("My name is Ana"), "Aye.");
            check("memory works", agent.chat("What is my name?"), "Ye be Ana.");
            check("tool call wrapped in text", agent.chat("what is 12 * 7"), "Arr. It be 84.");
            check("unknown tool survives", agent.chat("use a bad tool").startsWith("Arr. It be ERROR"), true);
            check("tool loop is stopped", agent.chat("loop please"), "I could not finish that with my tools.");

            Agent forgetful = new Agent("Barbossa", "a grumpy pirate", Memory.tier1(),
                    new LMStudioClient(url, "fake"), new Logger("Agent", shared, sink));
            forgetful.chat("My name is Ana");
            forgetful.chat("hello");
            forgetful.chat("hello again");
            check("tier1 forgets the name", forgetful.chat("What is my name?"), "No idea who ye are.");
        } finally {
            fake.stop();
        }

        section("Agent with the server turned off");
        Memory memory = Memory.tier2();
        Agent offline = new Agent("Barbossa", "a grumpy pirate", memory,
                new LMStudioClient("http://localhost:" + fake.getPort(), "fake"), new Logger("Agent", shared, sink));
        check("no crash when offline", offline.chat("hello").contains("unavailable"), true);
        check("failed question is not kept in memory", memory.size(), 0);

        System.out.println();
        System.out.println(passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }

    private static void check(String what, Object actual, Object expected) {
        if (String.valueOf(actual).equals(String.valueOf(expected))) {
            passed++;
            System.out.println("PASS: " + what);
        } else {
            failed++;
            System.out.println("FAIL: " + what + " -> got \"" + actual + "\", expected \"" + expected + "\"");
        }
    }

    private static void section(String title) {
        System.out.println("===== " + title + " =====");
    }
}

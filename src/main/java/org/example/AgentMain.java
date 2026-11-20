package org.example;

import java.util.Scanner;

/**
 * Track 2 entry point: a chat loop with the local agent.
 *
 * Options (all optional):
 *   --url   http://localhost:1234     LM Studio server
 *   --model <name>                    model identifier shown in LM Studio
 *   --tier  1 | 2 | 3                 memory size (default 2)
 *   --level TRACE | INFO | WARN | ERROR   (default INFO)
 *   --name  <agent name>   --personality "<text>"
 */
public class AgentMain {

    public static void main(String[] args) {
        String url = option(args, "--url", LMStudioClient.DEFAULT_URL);
        String model = option(args, "--model", LMStudioClient.DEFAULT_MODEL);
        String tier = option(args, "--tier", "2");
        String levelName = option(args, "--level", "INFO");
        String name = option(args, "--name", "Barbossa");
        String personality = option(args, "--personality", "a grumpy pirate who answers in short sentences");

        // one formatter and one sink, created here and shared by every logger (aggregation)
        Formatter formatter = new Formatter("[%t] [%l] %n: %m");
        ConsoleSink sink = new ConsoleSink();
        Logger mainLogger = new Logger("Main", formatter, sink);
        Logger agentLogger = new Logger("Agent", formatter, sink);

        try {
            Logger.setLevel(Logger.LogLevel.valueOf(levelName.toUpperCase()));
        } catch (IllegalArgumentException e) {
            mainLogger.error("Unknown level \"%\", using INFO", levelName);
        }

        Memory memory;
        switch (tier) {
            case "1": memory = Memory.tier1(); break;
            case "3": memory = Memory.tier3(); break;
            case "2": memory = Memory.tier2(); break;
            default:
                mainLogger.error("Unknown tier \"%\", using tier 2", tier);
                memory = Memory.tier2();
        }

        Agent agent = new Agent(name, personality, memory, new LMStudioClient(url, model), agentLogger);
        mainLogger.info("% is ready (memory: % messages, server: %). Type quit to exit.",
                name, memory.getCapacity(), url);

        Scanner in = new Scanner(System.in);
        while (true) {
            System.out.print("you> ");
            if (!in.hasNextLine()) break;
            String line = in.nextLine().trim();
            if (line.isEmpty()) continue;
            if (line.equalsIgnoreCase("quit")) break;
            System.out.println(name + "> " + agent.chat(line));
        }
        mainLogger.info("Bye");
    }

    private static String option(String[] args, String key, String fallback) {
        for (int i = 0; i < args.length - 1; i++) {
            if (args[i].equals(key)) return args[i + 1];
        }
        return fallback;
    }
}

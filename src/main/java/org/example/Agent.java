package org.example;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A chat agent with a personality, a memory and tools.
 *
 * Memory is received from outside (aggregation). The toolbox is always built here
 * (composition). The client and the logger are built here by the short constructor
 * and received by the long one.
 */
public class Agent {
    public static final int MAX_TOOL_CALLS = 3; // per user question

    private final String name;
    private final String personality;
    private final Memory memory;
    private final LMStudioClient client;
    private final Toolbox toolbox;
    private final Logger logger;

    public Agent(String name, String personality, Memory memory) {
        this(name, personality, memory, new LMStudioClient(), new Logger("Agent"));
    }

    public Agent(String name, String personality, Memory memory, LMStudioClient client, Logger logger) {
        this.name = name;
        this.personality = personality;
        this.memory = memory;
        this.client = client;
        this.logger = logger;
        this.toolbox = new Toolbox(logger);
    }

    /** Sends the user message to the model and returns the final answer. Never throws. */
    public String chat(String userMessage) {
        remember(new Message("user", userMessage));
        int toolCalls = 0;

        while (true) {
            List<Message> request = new ArrayList<>();
            request.add(systemMessage());
            request.addAll(memory.getMessages());
            logger.trace("Request sent (% messages)", request.size());

            LMResponse response;
            try {
                response = client.send(request);
            } catch (LMException e) {
                logger.error("%", e.getMessage());
                if (toolCalls == 0) memory.removeLast(); // forget the question that got no answer
                return "(" + name + " is unavailable right now. Check that the LM Studio server is running.)";
            }

            logger.info("Answer: % prompt tokens, % completion tokens, % tokens/s",
                    response.getPromptTokens(), response.getCompletionTokens(),
                    String.format("%.1f", response.tokensPerSecond()));

            String answer = stripThinking(response.getContent());
            if (answer.isEmpty()) {
                logger.error("The model returned an empty answer");
                if (toolCalls == 0) memory.removeLast();
                return "(" + name + " had nothing to say. Try asking again.)";
            }

            Map<String, Object> toolCall = findToolCall(answer);
            if (toolCall == null) {
                remember(new Message("assistant", answer));
                return answer; // normal text: the final answer
            }

            if (toolCalls >= MAX_TOOL_CALLS) {
                logger.warn("Tool call limit (%) reached for one question, stopping", MAX_TOOL_CALLS);
                String giveUp = "I could not finish that with my tools.";
                remember(new Message("assistant", giveUp));
                return giveUp;
            }
            toolCalls++;

            String result = handleToolCall(toolCall);
            remember(new Message("assistant", answer));
            remember(new Message("user", result));
        }
    }

    private Message systemMessage() {
        return new Message("system",
                "You are " + name + ". Personality: " + personality + ". Answer in short sentences.\n"
                + "If you need a tool, answer ONLY with a JSON object, like:\n"
                + "{ \"tool\": \"calculator\", \"input\": \"12 * 7\" }\n"
                + "Available tools: " + Toolbox.TOOL_NAMES + ".\n"
                + "calculator takes two numbers and one operator. roll_dice takes the number of sides.\n"
                + "get_time and get_date take an empty input.\n"
                + "After a tool returns, use its result to answer the user in normal text.\n"
                + "If you do not need a tool, answer normally (NOT JSON).");
    }

    /** Runs the tool named in the JSON and returns the line that goes back to the model. */
    private String handleToolCall(Map<String, Object> toolCall) {
        String tool = String.valueOf(toolCall.get("tool"));
        Object rawInput = toolCall.get("input");
        String input = rawInput == null ? "" : String.valueOf(rawInput);
        if (rawInput instanceof Double d && d == Math.rint(d)) input = String.valueOf(d.longValue());

        String result = toolbox.run(tool, input);
        logger.trace("Tool % returned: %", tool, result);
        return "Tool " + tool + " returned: " + result;
    }

    /**
     * Models often wrap the JSON in extra text, so only the part between the first "{"
     * and the last "}" is parsed. Returns null when the answer is not a tool call.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> findToolCall(String answer) {
        int start = answer.indexOf('{');
        int end = answer.lastIndexOf('}');
        if (start == -1 || end <= start) return null;
        try {
            Object parsed = Json.parse(answer.substring(start, end + 1));
            if (parsed instanceof Map<?, ?> map && map.containsKey("tool")) {
                if (start > 0 || end < answer.length() - 1) {
                    logger.warn("Tool call was wrapped in extra text, using the JSON part only");
                }
                return (Map<String, Object>) map;
            }
        } catch (IllegalArgumentException e) {
            logger.trace("Answer contains braces but is not valid JSON, treating it as text");
        }
        return null;
    }

    /** Removes the <think>...</think> block that reasoning models print before answering. */
    private String stripThinking(String content) {
        if (content == null) return "";
        return content.replaceAll("(?s)<think>.*?</think>", "").trim();
    }

    private void remember(Message message) {
        int dropped = memory.add(message);
        logger.trace("Message added to memory (% of %)", memory.size(), memory.getCapacity());
        if (dropped > 0) {
            logger.warn("Memory is full, dropped % oldest message(s)", dropped);
        }
    }

    public String getName() { return name; }
}

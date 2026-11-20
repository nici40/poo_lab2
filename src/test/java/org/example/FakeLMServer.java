package org.example;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A scripted stand-in for LM Studio, for testing the agent without a model.
 * It speaks the same /v1/chat/completions protocol and follows a few fixed rules:
 *   - "what is 12 * 7"            -> asks for the calculator tool (wrapped in extra text)
 *   - "what time is it"           -> asks for get_time
 *   - "use a bad tool"            -> asks for a tool that does not exist
 *   - "loop"                      -> asks for a tool forever
 *   - "what is my name"           -> looks for "my name is X" in the messages it received
 *   - after "Tool x returned: y"  -> answers in normal text using y
 *
 * Run it on its own:  java org.example.FakeLMServer [port]
 */
public class FakeLMServer {
    private static final Pattern MATH = Pattern.compile("(-?\\d+(?:\\.\\d+)?\\s*[+\\-*/]\\s*-?\\d+(?:\\.\\d+)?)");
    private static final Pattern NAME = Pattern.compile("my name is (\\w+)", Pattern.CASE_INSENSITIVE);

    private final HttpServer server;

    public FakeLMServer(int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", port), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            String answer;
            int promptTokens = body.length() / 4;
            try {
                answer = reply(body);
            } catch (RuntimeException e) {
                answer = "I did not understand the request.";
            }
            String json = "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":" + Json.quote(answer)
                    + "}}],\"usage\":{\"prompt_tokens\":" + promptTokens
                    + ",\"completion_tokens\":" + Math.max(1, answer.length() / 4) + "}}";
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
    }

    public void start() { server.start(); }
    public void stop() { server.stop(0); }
    public int getPort() { return server.getAddress().getPort(); }

    @SuppressWarnings("unchecked")
    private String reply(String body) {
        Map<String, Object> root = (Map<String, Object>) Json.parse(body);
        List<Object> messages = (List<Object>) root.get("messages");
        Map<String, Object> last = (Map<String, Object>) messages.get(messages.size() - 1);
        String text = String.valueOf(last.get("content"));
        String lower = text.toLowerCase();

        if (lower.contains("loop")) return "{ \"tool\": \"roll_dice\", \"input\": \"6\" }";
        if (text.startsWith("Tool ")) {
            // find the question that started this tool round: the latest user message that is not a tool result
            String question = "";
            for (Object o : messages) {
                Map<String, Object> m = (Map<String, Object>) o;
                String c = String.valueOf(m.get("content"));
                if ("user".equals(m.get("role")) && !c.startsWith("Tool ")) question = c.toLowerCase();
            }
            if (question.contains("loop")) return "{ \"tool\": \"roll_dice\", \"input\": \"6\" }";
            return "Arr. It be " + text.substring(text.indexOf("returned:") + 9).trim() + ".";
        }
        if (lower.contains("bad tool")) return "{ \"tool\": \"weather\", \"input\": \"Chisinau\" }";
        if (lower.contains("time")) return "{ \"tool\": \"get_time\", \"input\": \"\" }";
        Matcher math = MATH.matcher(text);
        if (math.find()) return "Sure! { \"tool\": \"calculator\", \"input\": \"" + math.group(1) + "\" } hope that helps";
        if (lower.contains("what is my name")) {
            String found = null;
            for (Object o : messages) {
                Map<String, Object> m = (Map<String, Object>) o;
                if (!"user".equals(m.get("role"))) continue;
                Matcher name = NAME.matcher(String.valueOf(m.get("content")));
                if (name.find()) found = name.group(1);
            }
            return found == null ? "No idea who ye are." : "Ye be " + found + ".";
        }
        return "Aye.";
    }

    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 1234;
        FakeLMServer fake = new FakeLMServer(port);
        fake.start();
        System.out.println("Fake LM server on http://localhost:" + fake.getPort() + " (Ctrl+C to stop)");
    }
}

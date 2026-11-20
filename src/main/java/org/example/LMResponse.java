package org.example;

/** What came back from the model, plus how long it took. */
public class LMResponse {
    private final String content;
    private final int promptTokens;
    private final int completionTokens;
    private final double seconds;

    public LMResponse(String content, int promptTokens, int completionTokens, double seconds) {
        this.content = content;
        this.promptTokens = promptTokens;
        this.completionTokens = completionTokens;
        this.seconds = seconds;
    }

    public String getContent() { return content; }
    public int getPromptTokens() { return promptTokens; }
    public int getCompletionTokens() { return completionTokens; }
    public double getSeconds() { return seconds; }

    public double tokensPerSecond() {
        return seconds > 0 ? completionTokens / seconds : 0;
    }
}

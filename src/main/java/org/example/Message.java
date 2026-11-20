package org.example;

/** One chat message. Role is "system", "user" or "assistant". */
public class Message {
    private final String role;
    private final String content;

    public Message(String role, String content) {
        this.role = role;
        this.content = content;
    }

    public String getRole() { return role; }
    public String getContent() { return content; }

    @Override
    public String toString() {
        return role + ": " + content;
    }
}

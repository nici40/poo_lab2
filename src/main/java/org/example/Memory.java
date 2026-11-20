package org.example;

import java.util.ArrayList;
import java.util.List;

/**
 * The conversation so far (user and assistant messages only; the system message
 * is added by the agent on every request and is never stored here).
 *
 * When the memory is full, the oldest messages are dropped. An exchange is always
 * dropped whole: the memory never starts with an assistant answer whose question is gone.
 */
public class Memory {
    private final int capacity; // maximum number of stored messages
    private final List<Message> messages = new ArrayList<>();

    public Memory(int capacity) {
        if (capacity < 2) throw new IllegalArgumentException("capacity must be at least 2 (one exchange)");
        this.capacity = capacity;
    }

    public static Memory tier1() { return new Memory(4); }   // 2 exchanges
    public static Memory tier2() { return new Memory(12); }  // 6 exchanges
    public static Memory tier3() { return new Memory(30); }  // 15 exchanges

    /** Adds a message and returns how many old messages had to be dropped to make room. */
    public int add(Message message) {
        messages.add(message);
        int dropped = 0;
        while (messages.size() > capacity) {
            messages.remove(0);
            dropped++;
        }
        // Do not leave half an exchange at the front.
        while (dropped > 0 && messages.size() > 1 && !"user".equals(messages.get(0).getRole())) {
            messages.remove(0);
            dropped++;
        }
        return dropped;
    }

    /** Used to undo the last user message when the request failed. */
    public void removeLast() {
        if (!messages.isEmpty()) messages.remove(messages.size() - 1);
    }

    public List<Message> getMessages() {
        return new ArrayList<>(messages); // a copy: callers cannot change the memory
    }

    public int size() { return messages.size(); }
    public int getCapacity() { return capacity; }
}

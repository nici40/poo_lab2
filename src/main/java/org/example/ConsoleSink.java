package org.example;

/** The only place in the logging code that prints to the console. */
public class ConsoleSink {
    public void write(String line) {
        System.out.println(line);
    }
}

# Lab 2: Formatters, Sinks, and a local AI agent (Track 2)

Java 21, no external libraries. Sources are in `src/main/java/org/example`,
dev helpers (fake model server, self-test) in `src/test/java/org/example`.

## Build

    javac -d out src/main/java/org/example/*.java src/test/java/org/example/*.java

In IntelliJ just open the project and run the `main` you want.

## Run

| What | Command |
| --- | --- |
| Part A demo + Lab 1 validator tests | `java -cp out org.example.Main` |
| Chat with the agent (LM Studio on port 1234) | `java -cp out org.example.AgentMain --model <model-id> --tier 2 --level INFO` |
| All checks, no model needed | `java -cp out org.example.SelfTest` |
| Scripted fake model on port 1234 | `java -cp out org.example.FakeLMServer` |

`AgentMain` options: `--url`, `--model`, `--tier 1|2|3`, `--level TRACE|INFO|WARN|ERROR`,
`--name`, `--personality "..."`. Type `quit` to exit.

## Classes

| Class | Job |
| --- | --- |
| `Logger` | Name + level filter. Two constructors: `Logger(name)` (composition), `Logger(name, formatter, sink)` (aggregation). Level is static. |
| `Formatter` | Format string -> finished line. `%t %l %n %m %r %%`. Also fills `%` in the message with varargs. Never prints. |
| `ConsoleSink` | `write(line)`. The only console print in the logging code. |
| `ParenthesesValidator`, `Cockpit` | Lab 1 code, unchanged. |
| `Message` | role + content. |
| `LMStudioClient`, `LMResponse`, `LMException` | HTTP call to `/v1/chat/completions`; returns text, token counts, seconds. |
| `Json` | Small JSON parser and string quoting. |
| `Memory` | Conversation with a capacity. `tier1()` = 4, `tier2()` = 12, `tier3()` = 30 messages. Drops the oldest exchange whole. |
| `Toolbox` | `run(tool, input)`: `get_time`, `get_date`, `calculator`, `roll_dice`. |
| `Agent` | System message, chat loop with the model, JSON tool protocol, max 3 tool calls per question. |
| `AgentMain` | Chat loop from the keyboard. |

## Log events (agent)

| Level | Event |
| --- | --- |
| TRACE | request sent (message count); message added to memory; tool run and result |
| INFO | after every answer: prompt tokens, completion tokens, tokens per second |
| WARN | memory full, old messages dropped; tool call wrapped in extra text; tool call limit reached |
| ERROR | LM Studio not reachable; answer cannot be read or is empty; unknown tool; tool input invalid |

## Design decisions

- A `%n` inside the message is not replaced: the format string is scanned once and the message is inserted as-is.
- Varargs: more `%` than values leaves the extra `%` in place; extra values are ignored; `%%` is a literal `%`; with no values the message is untouched.
- The level is checked before the message is formatted, so filtered messages cost nothing.
- The format is an instance field of `Formatter` (as in the handout's UML and sharing example), not static. Loggers share a format by sharing a `Formatter` object.
- When a request fails, the unanswered question is removed from memory.

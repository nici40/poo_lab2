# OOP Lab 2 handout (text version of OOP_Lab2_Formatters_Sinks_handout.pdf)

Lab 2: Formatters, Sinks, and a Cup of Coffee
Lab Author: Dominic Flocea
November 2026

1 Before you start
Get a hot beverage. Find a cozy place. Put on some music.
It is planned for two weeks, and can be followed such that every day has one small, bite-sized task.
Student can do one task, commit, close the laptop, and rest, continue the work tomorrow, or focus on
the next task right away.

A word about AI
You will use AI a lot in your career. To use them well, you have to be the kind of engineer who can do the
work without them. Read code, compare it, and build the experience to guide agents, instead of agents
guiding you. The skill grows only one way: by writing code yourself.
So for this lab, I am asking you for something simple and hard:
• Write the code yourself. The pressure of an empty method is fine. Embrace it. The feeling is not a
sign that something is wrong. It is the feeling of learning, and should look for to grow.
• Do not train the impulse to call for AI first thing against a problem. When you are stuck, re-read the
task, draw the classes on paper/paint/excalidraw (not sponsored), print a variable, read the documentation of your language, or ask a colleague or your teacher on their opinion. Once lab is presented, use
AI to give you extra feedback, and ask questions on what can be improved.
• This is a safe space. There is no pressure here. A messy solution you wrote yourself is worth more
than a clean one you did not write. A messy lab that does the tasks in the lab guarantees full mark.
Take this lab as an opportunity to chill and code. The tasks are split in days, but can be completed in any
way (two tasks per day, every second day/two tasks per day, and a free week).

2 Objectives
In this laboratory, you will improve the logger from Lab 1, and then use it in a bigger program of your
choice.
You will practice:
• Splitting one class with too many jobs into several classes with one job each;
• Composition: an object creates the objects it is made of;
• Aggregation: an object receives objects created somewhere else, and shares them;
• Reading input from a file and turning text into actions;
• Using your logger to understand what your program is doing.

1

3 Structure, submission, and deadline
The lab has two parts:
• Part A (mandatory, everyone): improve the logger.
• Part B (choose one track):
→ Track 1 – Display Shop (the default lab);
→ Track 2 – Local AI Agent (the challenge lab, for those who like pain).
Both tracks will use the logger from Part A.

Start

Monday, November 9, 2026

Deadline

Sunday, November 20, 2026, 11:59 PM

Branch

lab-comp-aggr-track-x, where x is 1 or 2, depending on the
track you choose

Submission

!!! a URL to a GitHub PR from the branch. You have to continue in
your Lab 1 repository.

Try to Commit at the end of every day, or task. Your git history is your lab diary. It also shows your
progress during the presentation.

Daily plan
Days

Track 1: Display Shop

Track 2: AI Agent

1–2

Part A: Formatter and Sink (everyone)

3–10

Displays, managers, commands

11–14

Debugging, testing, experimenting, report

LM Studio, agent, memory, tools

The last days are not free days. Programs often need more time for testing, fixing, and cleaning up. Use
those days. If you finish early, and would like to code some more - try the bonus task on varargs.

Laboratory Handout

2

4 Part A: The logger does too much
4.1 The problem
In Lab 1, your log method probably looked similar to this:
public void log(LogLevel msgLevel, String message) {
^/ 1. Should this message be printed?
if (msgLevel.ordinal() < level.ordinal()) return;
^/ 2. What does the line look like?
String line = format
.replace("%t", LocalTime.now().toString())
.replace("%l", msgLevel.name())
.replace("%n", name)
.replace("%m", message);

}

^/ 3. Where does the line go?
System.out.println(line);

This one method answers three different questions:
1. Should this message be logged? (the level)
2. How does the line look? (the format)
3. Where does the line go? (the console)
Now imagine a few small requests from your team:
• “Write the logs to a file, not to the console.”
→ You have to open Logger and change it.
• “Test that the format is correct, without printing anything.”
→ You cannot. Formatting and printing are glued together.
One already be excited about this design choice. One can see how splitting the format from the logger
and the sink in which the log goes to makes each individual part easier. Easier to test, understand, and
extend with new functionality. Each responsibility deserves its own class.

4.2 The idea: a pipeline
We split the work between three objects. Each one has a single job:
your code

logger.info("^^.")

Logger

should I log it?

Formatter

how does it look?

Sink

where does it go?

• Logger: has a name and filters messages by level. It does not know how a line looks or where it goes.
It asks its formatter and its sink.
• Formatter: owns the format string (%t, %l, %n, %m) and turns a message into a finished line of text. It
never prints.
• Sink: receives a finished line and delivers it somewhere.

4.3 What is a sink?
A sink is the place where log lines end up. Water flows down into a sink, and log lines flow down into a
sink too.
Examples of sinks in real programs: the console, a file (app.log), a remote log server, a database, or
even “nowhere” (a sink that throws everything away, useful in tests).
Laboratory Handout

3

In this lab, you only need one: ConsoleSink, which prints a line to the console. After this part, the only
place in your logging code that prints to the console is ConsoleSink.
Later in the course, when we learn about interfaces, you will see how one logger can write to any kind of
sink, or to many sinks at once, without changing a single line of Logger.

You are reinventing real software
This is not a toy design. Real logging libraries are built this way:
Library

Logger

Formatter

Sink

Python logging

Logger

Formatter

Handler

Java java.util.logging

Logger

Formatter

Handler

C++ spdlog

logger

formatter

sink

4.4 Task A1 (Day 1): The Formatter class
Day 1

Create a class called Formatter.

• It stores a format string as a static attribute (same as the lab 1 logger).
• It has a method that receives the logger name, level, and the message, and returns the finished line as
a string.
• It supports %t, %l, %n, and %m. If you did the %r extra credit in Lab 1, move it here too.
• It never prints anything.
Test it alone in main: create formatters, set different formats and print what format(^^.) returns.
Think about it: what happens if the message itself contains %n? For example logger.info("Ratio
is 100%n"). Does your formatter replace it with the logger name? Should it? Write your answer in the
report.

4.5 Task A2 (Day 2): The Sink, and a Logger that uses both
Day 2

Create a class called ConsoleSink with one method, write(line), which prints the line.

Then update Logger:
• Remove the static format and the method setFormat. Remove every console print.
• Add a Formatter type attribute and a ConsoleSink type attribute.
• log(level, message) checks the level, asks the formatter for a line, and gives the line to the sink.
• Keep level static. The minimum level is still shared by all loggers.
Give Logger two constructors:
1. Logger(name) creates its own default Formatter and its own ConsoleSink inside the constructor. This is composition: the logger builds its parts, and they live and die together with it.
2. Logger(name, formatter, sink) receives a formatter and a sink that were created outside.
This is aggregation: the logger uses them, but does not own them. Many loggers can share the same
formatter and sink.
Update your parentheses validator from Lab 1 so that it still works with the new logger, do not remove
the code. This way you can also test that nothing breaks, and the lab behaves the same - while having
a better architecture (refactoring baby ).

󰹈

Laboratory Handout

4

4.6 UML diagram
This design is not final. Method access modifiers, or what methods a class contains or exposes is up
to the student. It offers a small guidance on the overall design. Also, do not get too creative to a point
where you are doing a different lab.
Logger
- name : String
- level : LogLevel
- formatter : Formatter
- sink : ConsoleSink
+ Logger(name : String)
+ Logger(name : String, formatter : Formatter, sink : ConsoleSink)
+ setLevel(level : LogLevel)
+ log(level : LogLevel, message : String)
+ trace(message : String)
+ info(message : String)
+ warn(message : String)
+ error(message : String)

Formatter
- format : String
+ Formatter(format : String)
+ format(level : LogLevel, name : String,

ConsoleSink
+ write(line : String)

message : String) : String

This design is not final. Method access modifiers, or what methods a class contains or exposes is up
to the student. It offers a small guidance on the overall design. Also, do not get too creative to a point
where you are doing a different lab.
The non-empty constructor call is aggregation on diagram it would show an empty diamond, meaning
the parts can exist without the logger. When you use the constructor that accepts only the name, the
relationship is composition instead (it would be a filled diamond), because nobody else holds a reference
to those parts. In diagram does diamonds are not present, because it might confuse the student.

Sharing: static vs aggregation
In Lab 1, all loggers shared one format because it was static. Depending on student personal preference
you can share a format in a different way, by making the format an instance variable instead of static,
and for example accept the format via constructor:
Formatter shared = new Formatter("[%t] [%l] %n: %m");
ConsoleSink console = new ConsoleSink();
Logger parser = new Logger("Parser", shared, console);
Logger manager = new Logger("Manager", shared, console);
^/ different format than ones from above, the format is not shared
^/ except when we do provide the same Formatter object.
Logger special = new Logger("Special", new Formatter("%l >> %m"), console);

parser and manager share one formatter object. special has its own. With a static format, this was
impossible. Keep this example in mind for your report.
Laboratory Handout

5

5 Bonus: Format the message too
For students who like a challenge. Worth one extra point.
Right now, to log a message with values inside, you have to build the string yourself:
logger.info("Pair found at " + open + " and " + close);

Make the formatter help with this. Every % inside the message is replaced with the next extra argument,
in order:
logger.info("Pair found at % and %", 3, 7);
^/ [INFO] Parentheses: Pair found at 3 and 7

The extra arguments are a variable number of arguments, also called varargs. Most languages support
them:
Language

Vararg method

Java

void info(String msg, Object^^. args)

C#

void Info(string msg, params object[] args)

Kotlin

fun info(msg: String, vararg args: Any)

Python

def info(self, msg, *args)

C++

template<typename^^. Args>
void info(const std^:string& msg, Args^&^^. args)

In C++, variadic templates are advanced. You may use std^:initializer_list<std^:string> or
std^:vector<std^:string> instead. Inside the method, the arguments behave like an array or a
list. Look up how your language handles them.
Decide, and write in your report:
• What happens if there are more % than arguments?
• What happens if there are more arguments than %?
• How can a user write a literal % in a message? (Hint: many libraries use %%.)
• Do you format the message before or after checking the level? Why does it matter?

5.1 Different ways that the pros do it
Every language has a way to put values inside a template string. Look at how the placeholders differ.
C – the grandfather of them all. The letter after % says the type:
printf("Pair found at %d and %d\n", open, close);

Modern C++ (C++20 / C++23) – std^:format and std^:print use {} instead of %. The compiler
checks the types for you:
#include <print>
std^:print("Pair found at {} and {}\n", open, close);
std^:print("Second first: {1}, then {0}\n", open, close); ^/ positions
std^:print("Ratio: {:.2f}\n", 0.3333);
^/ Ratio: 0.33
std^:string s = std^:format("[{}] {}", "INFO", "Valid input");

Java – String.format uses % like C. The official logger, java.util.logging, uses numbered placeholders {0}, {1}:
String s = String.format("Pair found at %d and %d", open, close);

Laboratory Handout

6

import java.util.logging.Level;
import java.util.logging.Logger;
Logger logger = Logger.getLogger("Parentheses");
logger.log(Level.INFO, "Pair found at {0} and {1}",
new Object[]{ open, close });

The popular SLF4J library (not official, but used everywhere) uses {}: log.info("Pair found at
{} and {}", open, close);
Python – the official logging module uses %s and %d placeholders, and passes the values as varargs:
import logging
logging.basicConfig(level=logging.INFO,
format="[%(asctime)s] [%(levelname)s] %(name)s: %(message)s")
log = logging.getLogger("Parentheses")
log.info("Pair found at %d and %d", open_idx, close_idx)
log.info(f"Pair found at {open_idx} and {close_idx}")

# lazy
# eager

do you know about the difference between eager and lazy. No? Search for the difference, it is a fun, and an
important topic in programming, not related to OOP course or this lab - for the curious.
C# – string.Format and Console.WriteLine use numbered placeholders:
Console.WriteLine("Pair found at {0} and {1}", open, close);

Laboratory Handout

7

6 Part B: Choose your track
Choose one track. Both tracks get the same grade scale. Pick the one you will enjoy.
• Track 1 – Display Shop: classes that hold other objects, and a program that reads commands from a
file. Comfortable, solid, very good practice.
• Track 2 – Local AI Agent: build a small chat agent running on a local language model, with memory
and tools. Harder, more unknowns, more debugging.

7 Track 1: Display Shop
You will build a small shop. Displays are monitors with a size and a sharpness. Managers are shop
assistants who are assigned some displays, compare them for you, and let you buy them. The whole
shop is driven by a file of commands.

7.1 Task T1.1 (Day 3): The Display class
Day 3

Create a Display class:
Display
- model : String
- width : int
- height : int
- ppi : float
+ Display(model, width, height, ppi)
+ compareSize(other : Display)
+ compareSharpness(other : Display)
+ compareWithMonitor(other : Display)

• width and height are in pixels. ppi is pixels per inch (sharpness).
• compareSize
compares the physical size of the screens. Hint: the diagonal in inches is diagonal =
√
width2 + height2
.
ppi
• compareSharpness compares the ppi.
• compareWithMonitor uses both methods above (code reuse!) and gives a final verdict.
The output is descriptive and verbose, for example: Samsung-Odyssey-G5 (31.6") is bigger
than LG-27GP850 (27.0").
Test it in main with three hard-coded displays.

7.2 Task T1.2 (Day 4): The DisplayManager class
Day 4

Create a DisplayManager class:

Laboratory Handout

8

DisplayManager
Display

- name : String

- model : String

- assignedDisplays : List<Display>
+ DisplayManager(name : String)

0..*

+ assignDisplay(d : Display)
+ assist()

- width : int
- height : int
- ppi : float

+ buyDisplay(d : Display) : Display

• assignDisplay(d) adds a display to the list.
• assist() walks through the list and compares each display with the next one, until all of them are
compared.
• buyDisplay(d) removes the display from the list and returns the reference to it.
Test it in main: two managers, a few displays, one buy. Try this: assign the same display object to both
managers. Then buy it from one of them. What does the other manager still have? Why?

7.3 Task T1.3 (Day 5): Logging in the shop
Day 5 Add loggers to your classes. Use the aggregation constructor: create one formatter and one
sink in main and share them between all loggers.

Level

Events

TRACE

a manager is created; a display is created; a display is assigned

INFO

a display is bought

WARN

your choice (what might be suspicious, not wrong eg. manager after purchase left with 0 displays

ERROR

anything that fails (see the commands below)

How does each class get its logger? Does it create one, or receive one? Your choice, but explain it in the
report.

7.4 Task T1.4 (Day 6): The CommandReader
Day 6 The shop will be driven by a text file. Create a CommandReader class that reads a file and
returns its lines.
• The path of the file comes from the command-line arguments of your program (String[] args in
Java, argv in C++ and Python, and so on), or might be hardcoded.
• If the file does not exist, log an ERROR and stop nicely, without a crash.
Today, just read the file and print the lines with their line numbers. Starter code for reading and parsing a
command file in C++, Java, Python, Kotlin, and C# is in the Annex (Section A) at the end of this handout.

7.5 Task T1.5 (Days 7–8): Parsing the commands
Day 7 Write a method that parses the commands, for example parseCommand(line). It takes one
line, splits it into words, and runs the right action. This method drives the whole program, so it can live
next to main. No extra class is needed.
Keep the created managers and displays in two lists. When a command names a manager or a display,
find it with a simple loop over the list.
Today, support comments, empty lines, and the MANAGER and DISPLAY commands.
Laboratory Handout

9

Day 8

Add the remaining commands.

Command

Meaning

# any text

a comment, ignore it

(empty line)

ignore it

MANAGER <name>

create a new manager

DISPLAY <model> <w> <h> <ppi>

create a new display

ASSIGN <model> <manager>

assign an existing display to a manager

COMPARE <model1> <model2>

compareWithMonitor of the two

ASSIST <manager>

the manager compares their displays

BUY <model> <manager>

buy a display from a manager

Notice that displays are created first, and assigned later. A display exists before any manager knows
about it. Composition or aggregation?

7.6 Task T1.6 (Days 9–10): Errors and the example runs
Day 9 A command file can contain mistakes. Your program must never crash because of a bad line.
Log an ERROR that contains the line number and a clear explanation, then continue with the next line.
Examples of mistakes: an unknown command, a missing or extra word, a number that is not a number,
a manager or display that does not exist, buying a display that is not assigned to that manager.
Some situations are not clearly errors. For example: creating a manager that already exists, or asking a
manager with no displays to assist. Decide if they are a WARN or an ERROR, and justify it in the report.
Day 10 Copy the two files below into text files on your computer. Do not type them into your code:
your program must read and parse them.
Before you run them, read each file and write down what you expect to happen on every line. Then run
your program and compare.
File 1: shop-monday.txt
# Monday at the shop. Everything here should work.
MANAGER Bob
MANAGER Alice
DISPLAY Dell-U2723QE 3840 2160 163.0
DISPLAY LG-27GP850 2560 1440 108.8
DISPLAY Samsung-Odyssey-G5 2560 1440 93.0
DISPLAY Apple-StudioDisplay 5120 2880 218.0
ASSIGN Dell-U2723QE Bob
ASSIGN LG-27GP850 Bob
ASSIGN Samsung-Odyssey-G5 Bob
ASSIGN Apple-StudioDisplay Alice
ASSIGN Dell-U2723QE Alice
COMPARE Dell-U2723QE Apple-StudioDisplay
ASSIST Bob
BUY LG-27GP850 Bob
ASSIST Bob
# Bob sells the Dell. Alice also had it. What does Alice show now?
BUY Dell-U2723QE Bob
ASSIST Alice

With the level set to TRACE and the format [%l] %n: %m, the beginning of your output could look like
this (your messages can be different):

Laboratory Handout

10

[TRACE] Parser: Manager Bob created
[TRACE] Parser: Manager Alice created
[TRACE] Parser: Display Dell-U2723QE created (27.0", 163 ppi)
^^.
[TRACE] Manager: Dell-U2723QE assigned to Bob
^^.
[INFO] Manager: Bob sold LG-27GP850

File 2: shop-tuesday.txt
# Tuesday at the shop. Not everything here is correct.
MANAGER Carol
DISPLAY BenQ-PD2705U 3840 2160 163.0
DISPLAY AOC-24G2 1920 1080 abc
DISPLAY Asus-PA278QV 2560 1440
MANAGER Carol
ASSIGN BenQ-PD2705U Dave
ASSIGN Philips-Evnia Carol
ASSIGN BenQ-PD2705U Carol
ASSIGN BenQ-PD2705U Carol
BUY Asus-PA278QV Carol
SELL BenQ-PD2705U Carol
assist carol
BUY BenQ-PD2705U Carol
BUY BenQ-PD2705U Carol
ASSIST Carol
COMPARE BenQ-PD2705U

There are more than eight problems in this file. Find all of them. Are commands case-sensitive (assist
carol)? Your decision, but be consistent and document it.

7.7 Days 11–14: Polish
Write at least one command file of your own that tests something the two files above do not. Run all files
with different log levels (TRACE, INFO, ERROR) and check that the right messages disappear. Clean up
your code. Write the report.

Laboratory Handout

11

8 Track 2: Local AI Agent
For hardcore engineers.
You will build a small chat agent that runs on your own computer, using a local language model in LM
Studio. Your agent will have a personality, it will remember the conversation, and it will be able to use
tools.

8.1 The one rule of this track
You may use AI to write the code that talks to LM Studio (the HTTP request and reading
the response). Nothing else.
Everything else – the agent, memory, tools, the JSON tool protocol, logging – you write by
hand. Mark the AI-assisted class with a comment, for example ^/ AI-assisted: LM
Studio integration.
If you are not sure you can do this without AI, or you notice that you keep reaching for it,
switch to Track 1. Do it early. There is no shame in that. Track 1 is a great lab, and you may
switch at any time until the end of Day 6.
You may use libraries: an HTTP library and a JSON library are fine. Using a library is not the same as
asking an AI to write your code. During the presentation, you will explain every line that is not marked as
AI-assisted.

8.2 Language models do not remember anything
A language model is stateless. Every request is a brand-new conversation. If you say “My name is Ana”
and in the next request ask “What is my name?”, the model has no idea.
Chat apps fake memory: on every request, they send the whole conversation so far. That is the job of
your Memory class. More memory means more text in every request, which means more tokens and
slower answers. Your logger will show you exactly how much slower.

8.3 Task T2.1 (Day 3): Meet LM Studio
Day 3

No code today.

• Install LM Studio from https://lmstudio.ai.
• Download a small instruct model that fits your RAM (a 3B–4B model is a good start, for example from
the Qwen, Llama, or Gemma families). Avoid “reasoning” or “thinking” models for now: they print long
<think> blocks before answering (but know that reasoning could be turned off with a flag on the
models).
• Chat with it in the LM Studio window. Notice the tokens per second it shows.
• Start the local server (Developer tab). By default it runs on http:^/localhost:1234.
• Send one request from the terminal and read the JSON that comes back:
curl http:^/localhost:1234/v1/chat/completions \
-H "Content-Type: application/json" \
-d '{ "model": "<your-model-name>",
"messages": [ { "role": "user", "content": "Say hi in 5 words" } ] }'

Find in the response: the answer text (choices[0].message.content) and the token counts (usage).

8.4 Task T2.2 (Day 4): Talk to LM Studio from your code
Day 4 Create a class LMStudioClient (this is the one class where AI help is allowed). It sends a list
of messages to the server and returns the answer text, the number of prompt tokens, and the number of
completion tokens. Also measure how many seconds the request took.
Laboratory Handout

12

Create a small class for one message, Message, with a role ("system", "user", or "assistant")
and a content.
Test: send one hard-coded question from main and print the answer. Read the client code and make
sure you understand the ideas there.

8.5 Task T2.3 (Day 5): The Agent and its personality
Day 5 Create an Agent class with a name and a personality (for example: “a grumpy pirate who
answers in short sentences”) (short sentences highly recommended for faster responses and testing).
The agent builds a system message from its name and personality, and sends it first in every request.
Add a chat loop in main: read a line from the user, give it to agent.chat(^^.), print the answer, repeat
until the user types quit.
Test: tell the agent your name. Then ask for it. It will not remember. That is tomorrow’s problem.

8.6 Task T2.4 (Days 6–7): Memory
Day 6 Create a Memory class that stores the conversation as a list of Message objects. After every
exchange, the agent adds the user message and the assistant answer to its memory, and sends the
whole memory with every request. The system message is not stored in memory: the agent adds it in
front, every time.
Test the name question again. It should work now.
Day 7 A conversation cannot grow forever. Give Memory a capacity, and three static methods that
create memories of different sizes:
Memory small = Memory.tier1();
Memory medium = Memory.tier2();
Memory large = Memory.tier3();

^/ short memory
^/ longer
^/ longest

You decide the sizes. When the memory is full, the oldest messages are dropped. Can you drop only half
of an exchange (only the user message, without the answer)? Should you?
TIP: keep them relatively short for quick testing.

8.7 Task T2.5 (Day 8): Logging the agent
Day 8

Use your logger in the agent:
Level

Events

TRACE

a request is sent (how many messages); a message is added to memory

INFO

after every answer: prompt tokens, completion tokens, and tokens per second (completion tokens / seconds)

WARN

memory is full and old messages are dropped; your choice for more

ERROR

LM Studio is not running; the answer cannot be read; a tool fails

Your program must not crash when LM Studio is turned off. Try it.

8.8 Task T2.6 (Days 9–10): Tools
A language model cannot check the time or do reliable math. But it can ask your program to do it. We
agree on a protocol: when the model needs a tool, it answers with only a JSON object.
Day 9 Create a Toolbox class with at least three tools, for example: get_time, get_date, calculator
(two numbers and one operator is enough), roll_dice. It has one public method, run(toolName,
input), which returns the tool’s result as a string. An unknown tool name is an ERROR, not a crash.
Laboratory Handout

13

Test the toolbox alone in main, without the model.
Day 10

Teach the agent to use the toolbox. Add the protocol to the system message, for example:

If you need a tool, answer ONLY with a JSON object, like:
{ "tool": "calculator", "input": "12 * 7" }
Available tools: get_time, get_date, calculator, roll_dice.
If you do not need a tool, answer normally (NOT JSON).

Then, in Agent.chat(^^.):
1. Send the conversation to the model.
2. If the answer is JSON with a "tool" key: parse it, run the tool, add the result to the conversation (for
example as a user message: Tool calculator returned: 84), and ask the model again.
3. If the answer is normal text: that is the final answer.
Things will go wrong. The model will wrap the JSON in extra text, use a tool that does not exist, or call
tools forever. Handle these cases: log them, and set a limit on tool calls per question. A short Python
example of parsing a tool call is in the Annex (Section B).

8.9 UML diagram
This is a suggestion. You may change it, but explain your changes.
Agent
- name : String
- personality : String
- memory : Memory
- client : LMStudioClient
- toolbox : Toolbox
- logger : Logger
+ Agent(name, personality, memory : Memory)
+ chat(userMessage : String) : String
- systemMessage() : Message
- handleToolCall(json : String) : String

Memory
- capacity : int

Toolbox

- messages : List<Message>
+ tier1() : Memory
+ tier2() : Memory
+ tier3() : Memory
+ add(message : Message)

+ run(tool : String,
input : String) : String
- getTime() : String
- calculate(input : String) : String

LMStudioClient
- baseUrl : String
- model : String
+ send(messages : List<Message>)
: LMResponse

+ getMessages() : List<Message>

0..*
Message
- role : String
- content : String

Here, the memory is created outside (Memory.tier2()) and given to the agent: aggregation. The
toolbox and the client are created by the agent itself: composition. Do you agree? What would it mean
to give the same memory object to two different agents?
Laboratory Handout

14

8.10 Days 11–14: Experiment and polish
Now play, and measure with your logger:
• Talk to the agent with tier1, then tier3. How do prompt tokens and tokens per second change as
the conversation grows?
• Try two different personalities, or two different models.
• Find questions that break your tool protocol, and make your agent survive them.
Put your measurements in the report: a small table is enough.

Laboratory Handout

15

9 Evaluation
The laboratory grade is based on completed functionality. The following milestones define the starting
grade:
• 5 points: Part A is done. Formatter and ConsoleSink are separate classes. Logger has both
constructors and does not print anything itself. The parentheses validator still works.
• 6 points: The main classes of your track work with hard-coded data in main.
→ Track 1: Display and DisplayManager.
→ Track 2: Agent with a personality chats with LM Studio.
• 7 points: The track is complete.
→ Track 1: commands are read and parsed from a file.
→ Track 2: memory with three tiers, and at least three tools used through JSON.
• 8 points: The logger is integrated with the required log events, and errors are handled without crashes.
→ Track 1: both example files run correctly.
→ Track 2: tokens and tokens per second are logged.
• 9–10 points: Report + Presentation.
Grades above 8 depend on your individual presentation and report. You must explain your classes, where
you used composition and where you used aggregation, and why.
Authenticity and critical thinking which comes from a human-written report shows without a shadow of
a doubt the student’s confidence in their knowledge.
Your final grade may be higher or lower than the starting grade if your implementation or explanations
show that some requirements are not met, are not understood, or are outsourced.
The bonus task (message formatting with varargs) can earn one extra point, up to the maximum grade
allowed by the course.

9.1 Report guidelines
REPORT SHOULD BE UPLOADED TO ESLE in .pdf or .docx format. ALONGSIDE THE REPORT, THE STUDENT MUST OPEN A PR WITH THE 2nd LAB
# Laboratory Report - Formatters, Sinks, and <your track>
**Student:**
**Programming language:**
**Track:** Display Shop / Local AI Agent
*Answer each section briefly, using your own code as examples.
Do not write textbook definitions without showing where the concept
appears in your program.*
^# 1. The logger, split in three
- What is the single responsibility of Logger, Formatter, and ConsoleSink?
- What happens in your formatter if the message itself contains "%n"?
- Which console prints did you remove, and where do they live now?
^# 2. Composition and aggregation
- Show the two Logger constructors. Which one is composition,
which one is aggregation? Explain with object lifetimes.
- Find one more example of each in your track. Include snippets.
- Track 1: the same Display was assigned to Bob and Alice, and Bob
sold it. What did Alice show? Why?
- Track 2: what would happen if two agents shared one Memory object?
^# 3. Static vs shared objects
- In Lab 1 the format was static. Now it is an object that can be
shared. What did you gain? What did you lose?

Laboratory Handout

16

- Why is the level still static? Would you change that?
^# 4. Your track
- Track 1: how does your parsing method turn a line into an action?
Which mistakes in shop-tuesday.txt did you find, and which did
you decide are WARN instead of ERROR? Why?
- Track 2: how does the tool protocol work, step by step?
Include your measurements (tokens, tokens per second, memory tiers).
^# 5. Reflection
- Where did you stare at an empty method the longest?
What finally unblocked you?
- If you had more time, what would you improve, and why?
**Submission requirements:** Answer all sections in your own words.
About 2 pages. Be prepared to explain your answers during the
laboratory presentation.

Good luck, and enjoy the coffee!

Laboratory Handout

17

A Annex: Parsing a command file (Track 1 starter code)
This is the quickest way to get from a file to your commands. Each program opens shop-monday.txt,
reads the first word of a command, and then uses an if/else chain to read the rest. Only MANAGER and
DISPLAY are shown. The other commands follow the same pattern.
It is not production code, and it does not need to be. Replacing the prints with your classes and your
logger is your job.

A.1 C++
std^:ifstream works exactly like std^:cin: the ^> operator reads the next word from the file.
#include <fstream>
#include <iostream>
#include <string>
int main() {
std^:ifstream file("shop-monday.txt");
if (!file) {
std^:cout << "Cannot open the file\n";
return 1;
}

}

std^:string command;
while (file >> command) {
if (command[0] ^= '#') {
std^:string rest;
std^:getline(file, rest); ^/ skip the rest of the comment
} else if (command ^= "MANAGER") {
std^:string name;
file >> name;
std^:cout << "Create manager " << name << "\n";
} else if (command ^= "DISPLAY") {
std^:string model;
int width, height;
float ppi;
file >> model >> width >> height >> ppi;
std^:cout << "Create display " << model << "\n";
} else {
std^:cout << "Unknown command: " << command << "\n";
}
}
return 0;

Laboratory Handout

18

A.2 Java
Scanner does the same job as std^:cin: next() reads the next word.
import java.io.File;
import java.util.Scanner;
public class Main {
public static void main(String[] args) throws Exception {
Scanner file = new Scanner(new File("shop-monday.txt"));
while (file.hasNext()) {
String command = file.next();
if (command.startsWith("#")) {
file.nextLine(); ^/ skip the rest of the comment
} else if (command.equals("MANAGER")) {
String name = file.next();
System.out.println("Create manager " + name);
} else if (command.equals("DISPLAY")) {
String model = file.next();
int width = Integer.parseInt(file.next());
int height = Integer.parseInt(file.next());
float ppi = Float.parseFloat(file.next());
System.out.println("Create display " + model);
} else {
System.out.println("Unknown command: " + command);
}

}

}

}
file.close();

A.3 Python
Python reads the file line by line, and split() cuts a line into words.
with open("shop-monday.txt") as file:
for line in file:
words = line.split()
if not words or words[0].startswith("#"):
continue # empty line or comment
command = words[0]
if command ^= "MANAGER":
name = words[1]
print("Create manager", name)
elif command ^= "DISPLAY":
model = words[1]
width = int(words[2])
height = int(words[3])
ppi = float(words[4])
print("Create display", model)
else:
print("Unknown command:", command)

Laboratory Handout

19

A.4 Kotlin
import java.io.File
^/ classic kt, functional, and helpful
fun main() {
File("shop-monday.txt").forEachLine { line ->
val words = line.split(" ")
val command = words[0]

}

}

if (command ^= "MANAGER") {
val name = words[1]
println("Create manager $name")
} else if (command ^= "DISPLAY") {
val model = words[1]
val width = words[2].toInt()
val height = words[3].toInt()
val ppi = words[4].toFloat()
println("Create display $model")
} else if (command ^= "" ^& !command.startsWith("#")) {
println("Unknown command: $command")
}

A.5 C#
using System;
using System.IO;
class Program {
static void Main() {
foreach (string line in File.ReadLines("shop-monday.txt")) {
string[] words = line.Split(' ');
string command = words[0];

}

}

}

if (command ^= "MANAGER") {
string name = words[1];
Console.WriteLine("Create manager " + name);
} else if (command ^= "DISPLAY") {
string model = words[1];
int width = int.Parse(words[2]);
int height = int.Parse(words[3]);
float ppi = float.Parse(words[4]);
Console.WriteLine("Create display " + model);
} else if (command ^= "" ^& !command.StartsWith("#")) {
Console.WriteLine("Unknown command: " + command);
}

Laboratory Handout

20

B Annex: Parsing a tool call (Track 2 starter code)
The model answers with plain text. Your program has to decide if that text is a tool call or a normal answer. Python’s json module turns JSON text into a dictionary. If the text is not valid JSON, json.loads
raises a JSONDecodeError, so a normal answer simply fails to parse.
import json
# What the model sent back. Try changing it to a normal sentence.
answer = 'Sure! { "tool": "calculator", "input": "12 * 7" }'
# Models often add text around the JSON. Keep only the part
# between the first "{" and the last "}".
start = answer.find("{")
end = answer.rfind("}")
tool_call = None
if start ^= -1 and end > start:
try:
tool_call = json.loads(answer[start:end + 1])
except json.JSONDecodeError:
tool_call = None # it looked like JSON, but it was not
if isinstance(tool_call, dict) and "tool" in tool_call:
tool = tool_call["tool"]
tool_input = tool_call.get("input", "")
print("Tool call:", tool, "with input", tool_input)
# result = toolbox.run(tool, tool_input)
# add "Tool calculator returned: 84" to the conversation,
# and ask the model again
else:
print("Normal answer:", answer)

The same idea works in every language: find the JSON, try to parse it, and if that fails, treat the text as a
normal answer. Java, C#, and Kotlin have JSON libraries (Gson, System.Text.Json, kotlinx.serialization);
in C++, nlohmann/json is the usual choice.

Laboratory Handout

21


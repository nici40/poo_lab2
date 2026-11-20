package org.example;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Things the model can ask the program to do. One public entry point: run(toolName, input).
 * A tool never throws: failures are logged as ERROR and returned as text starting with
 * "ERROR:", so the model can read what went wrong and recover.
 */
public class Toolbox {
    public static final String TOOL_NAMES = "get_time, get_date, calculator, roll_dice";

    private static final Pattern EXPRESSION =
            Pattern.compile("^\\s*(-?\\d+(?:\\.\\d+)?)\\s*([+\\-*/x])\\s*(-?\\d+(?:\\.\\d+)?)\\s*$");

    private final Logger logger;
    private final Random random = new Random();

    public Toolbox() {
        this(new Logger("Toolbox"));
    }

    public Toolbox(Logger logger) {
        this.logger = logger;
    }

    public String run(String toolName, String input) {
        String tool = toolName == null ? "" : toolName.trim().toLowerCase();
        String arg = input == null ? "" : input.trim();
        logger.trace("Running tool % with input \"%\"", tool, arg);

        switch (tool) {
            case "get_time": return getTime();
            case "get_date": return getDate();
            case "calculator": return calculate(arg);
            case "roll_dice": return rollDice(arg);
            default:
                logger.error("Unknown tool \"%\"", toolName);
                return "ERROR: unknown tool \"" + toolName + "\". Available tools: " + TOOL_NAMES + ".";
        }
    }

    private String getTime() {
        return LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

    private String getDate() {
        return LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy"));
    }

    /** Two numbers and one operator, for example "12 * 7". */
    private String calculate(String input) {
        Matcher m = EXPRESSION.matcher(input);
        if (!m.matches()) {
            logger.error("Tool calculator failed: cannot read \"%\"", input);
            return "ERROR: calculator needs two numbers and one operator (+ - * /), like \"12 * 7\".";
        }
        double a = Double.parseDouble(m.group(1));
        double b = Double.parseDouble(m.group(3));
        double result;
        switch (m.group(2)) {
            case "+": result = a + b; break;
            case "-": result = a - b; break;
            case "/":
                if (b == 0) {
                    logger.error("Tool calculator failed: division by zero in \"%\"", input);
                    return "ERROR: division by zero.";
                }
                result = a / b;
                break;
            default: result = a * b; // * or x
        }
        if (result == Math.rint(result) && Math.abs(result) < 1e15) return String.valueOf((long) result);
        return String.valueOf(result);
    }

    /** Input is the number of sides; empty means a 6-sided die. */
    private String rollDice(String input) {
        int sides = 6;
        if (!input.isEmpty()) {
            try {
                sides = Integer.parseInt(input.replaceAll("[^0-9-]", ""));
            } catch (NumberFormatException e) {
                logger.error("Tool roll_dice failed: \"%\" is not a number of sides", input);
                return "ERROR: roll_dice needs a number of sides, like \"6\".";
            }
        }
        if (sides < 2) {
            logger.error("Tool roll_dice failed: a die needs at least 2 sides, got %", sides);
            return "ERROR: a die needs at least 2 sides.";
        }
        return String.valueOf(random.nextInt(sides) + 1);
    }
}

package bg.sofia.uni.fmi.mjt.code.check.server.commands;

import java.util.ArrayList;
import java.util.List;

public class CommandParser {
    public static String getCommandString(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }
        return splitInput(input).getFirst();
    }

    private static final int ARGS_BEGIN_INDEX = 1;
    public static List<String> getCommandArgs(String input) {
        if (input == null) {
            return List.of();
        }

        List<String> args = splitInput(input);
        if (args.isEmpty() || args.size() == ARGS_BEGIN_INDEX) {
            return List.of();
        }
        return args.subList(ARGS_BEGIN_INDEX, args.size());
    }

    private static final Character SEPARATOR = ' ';
    private static final Character QUOTE = '"';
    private static List<String> splitInput(String input) {
        List<String> tokens = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean insideQuote = false;

        for (char c : input.toCharArray()) {
            if (c == QUOTE) {
                insideQuote = !insideQuote;
                continue;
            }
            if (c == SEPARATOR && !insideQuote) {
                if (!sb.isEmpty()) {
                    tokens.add(sb.toString());
                    sb.setLength(0);
                }
            } else {
                sb.append(c);
            }
        }
        if (!sb.isEmpty()) {
            tokens.add(sb.toString());
        }
        return tokens;
    }
}

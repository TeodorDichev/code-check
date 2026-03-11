package bg.sofia.uni.fmi.mjt.code.check.server.commands;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandParserTest {

    @Test
    void testGetCommandStringWithNullInputReturnsEmptyString() {
        assertEquals("", CommandParser.getCommandString(null),
                "Command string should be empty when input is null");
    }

    @Test
    void testGetCommandStringWithBlankInputReturnsEmptyString() {
        assertEquals("", CommandParser.getCommandString("   "),
                "Command string should be empty when input is blank");
    }

    @Test
    void testGetCommandStringWithSimpleInputReturnsFirstToken() {
        assertEquals("login", CommandParser.getCommandString("login user pass"),
                "Command string should be the first token of the input");
    }

    @Test
    void testGetCommandArgsWithNoArgumentsReturnsEmptyList() {
        assertTrue(CommandParser.getCommandArgs("logout").isEmpty(),
                "Arguments list should be empty when only command is provided");
    }

    @Test
    void testGetCommandArgsWithMultipleArgumentsReturnsAllArgsAfterFirst() {
        List<String> expected = List.of("arg1", "arg2");
        assertEquals(expected, CommandParser.getCommandArgs("cmd arg1 arg2"),
                "Arguments list should contain all tokens except the first one");
    }

    @Test
    void testGetCommandArgsWithQuotedArgumentPreservesSpaces() {
        List<String> args = CommandParser.getCommandArgs("create-assignment \"Assignment 1\" 2025");
        assertEquals("Assignment 1", args.getFirst(),
                "Quoted argument should preserve internal spaces");
    }

    @Test
    void testGetCommandArgsWithQuotedArgumentRemovesQuotes() {
        List<String> args = CommandParser.getCommandArgs("cmd \"quotedValue\"");
        assertEquals("quotedValue", args.getFirst(),
                "Quotes should be removed from the resulting tokens");
    }

    @Test
    void testGetCommandArgsWithMultipleSpacesBetweenTokensIgnoresEmptyTokens() {
        List<String> args = CommandParser.getCommandArgs("cmd    arg1  arg2");
        assertEquals(List.of("arg1", "arg2"), args,
                "Extra spaces between arguments should be ignored");
    }
}
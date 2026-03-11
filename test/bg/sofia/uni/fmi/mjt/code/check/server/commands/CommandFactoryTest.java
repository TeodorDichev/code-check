package bg.sofia.uni.fmi.mjt.code.check.server.commands;

import bg.sofia.uni.fmi.mjt.code.check.server.commands.account.LoginCommand;
import bg.sofia.uni.fmi.mjt.code.check.server.commands.account.RegisterCommand;
import bg.sofia.uni.fmi.mjt.code.check.server.commands.common.UnknownCommand;
import bg.sofia.uni.fmi.mjt.code.check.server.commands.course.CreateCourseCommand;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class CommandFactoryTest {

    @Test
    void testOfReturnsRegisterCommandForRegisterInput() {
        assertInstanceOf(RegisterCommand.class, CommandFactory.of("register user pass"),
                "Factory should return a RegisterCommand instance for 'register' input");
    }

    @Test
    void testOfReturnsLoginCommandForLoginInput() {
        assertInstanceOf(LoginCommand.class, CommandFactory.of("login user pass"),
                "Factory should return a LoginCommand instance for 'login' input");
    }

    @Test
    void testOfReturnsCreateCourseCommandForCreateCourseInput() {
        assertInstanceOf(CreateCourseCommand.class, CommandFactory.of("create-course MJT"),
                "Factory should return a CreateCourseCommand instance for 'create-course' input");
    }

    @Test
    void testOfReturnsUnknownCommandForInvalidInput() {
        assertInstanceOf(UnknownCommand.class, CommandFactory.of("non-existent-command"),
                "Factory should return an UnknownCommand instance for unrecognized input");
    }

    @Test
    void testOfReturnsUnknownCommandForEmptyInput() {
        assertInstanceOf(UnknownCommand.class, CommandFactory.of(""),
                "Factory should return an UnknownCommand instance for empty input");
    }

    @Test
    void testOfReturnsUnknownCommandForNullInput() {
        assertInstanceOf(UnknownCommand.class, CommandFactory.of(null),
                "Factory should return an UnknownCommand instance for null input");
    }
}
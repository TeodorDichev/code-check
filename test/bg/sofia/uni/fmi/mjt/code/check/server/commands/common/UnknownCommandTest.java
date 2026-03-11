package bg.sofia.uni.fmi.mjt.code.check.server.commands.common;

import bg.sofia.uni.fmi.mjt.code.check.contract.ServerResponse;
import bg.sofia.uni.fmi.mjt.code.check.contract.Status;
import bg.sofia.uni.fmi.mjt.code.check.server.commands.CommandType;
import bg.sofia.uni.fmi.mjt.code.check.server.entities.User;
import bg.sofia.uni.fmi.mjt.code.check.server.repositories.CodeCheckContext;
import bg.sofia.uni.fmi.mjt.code.check.server.services.logging.Logger;
import bg.sofia.uni.fmi.mjt.code.check.server.session.Session;
import bg.sofia.uni.fmi.mjt.code.check.server.session.SessionStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.channels.SocketChannel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UnknownCommandTest {

    @Mock private SessionStore sessionStore;
    @Mock private CodeCheckContext context;
    @Mock private Logger logger;
    @Mock private Session session;
    @Mock private SocketChannel channel;
    @Mock private User user;

    private UnknownCommand command;

    @BeforeEach
    void setUp() {
        command = new UnknownCommand();
        command.addDependencies(sessionStore, context);
        command.addSessionContext(session);
        command.addLogger(logger);
    }

    @Test
    void testExecuteReturnsErrorWithUser() {
        when(session.key()).thenReturn(channel);
        when(sessionStore.getUser(channel)).thenReturn(user);

        ServerResponse response = command.execute();

        assertEquals(CommandType.UNKNOWN, response.command(),
                "The response command type should be UNKNOWN");
        assertEquals(Status.ERROR, response.status(),
                "Unknown command execution should always return ERROR status");
        assertEquals(user, response.user(),
                "The response should include the user currently in session even for unknown commands");
    }

    @Test
    void testExecuteHandlesException() {
        RuntimeException crash = new RuntimeException("Crash");
        when(session.key()).thenThrow(crash);

        ServerResponse response = command.execute();

        assertEquals(Status.ERROR, response.status(),
                "Should return ERROR status even when an exception occurs inside UnknownCommand");
        assertEquals(CommandType.UNKNOWN, response.command(),
                "Command type should still be UNKNOWN during exception handling");
        verify(logger).logError("Exception occurred in unknown command", crash);
    }

    @Test
    void testGetType() {
        assertEquals(CommandType.UNKNOWN, command.getType(),
                "The getType() method must return UNKNOWN");
    }
}
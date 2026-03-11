package bg.sofia.uni.fmi.mjt.code.check.server.commands;

import bg.sofia.uni.fmi.mjt.code.check.contract.ServerResponse;
import bg.sofia.uni.fmi.mjt.code.check.contract.Status;
import bg.sofia.uni.fmi.mjt.code.check.server.repositories.CodeCheckContext;
import bg.sofia.uni.fmi.mjt.code.check.server.services.ValidationService;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommandExecutorTest {

    @Mock private SessionStore sessionStore;
    @Mock private CodeCheckContext context;
    @Mock private ValidationService validationService;
    @Mock private Logger logger;
    @Mock private Session session;
    @Mock private SocketChannel channel;

    private CommandExecutor executor;

    @BeforeEach
    void setUp() {
        executor = CommandExecutor.configure(sessionStore, context, logger);
    }

    private void setupHelpMocks() {
        when(session.key()).thenReturn(channel);
        when(context.validationService()).thenReturn(validationService);
    }

    @Test
    void testExecuteReturnsSuccessResponseForValidCommand() {
        setupHelpMocks();

        ServerResponse response = executor.execute("help", session);

        assertEquals(Status.OK, response.status(),
                "Status should be OK for a successfully executed help command");
    }

    @Test
    void testExecuteReturnsCorrectCommandTypeForExecutedCommand() {
        setupHelpMocks();

        ServerResponse response = executor.execute("help", session);

        assertEquals(CommandType.HELP, response.command(),
                "Response should contain the correct command type HELP");
    }

    @Test
    void testExecuteReturnsErrorResponseWhenCommandExecutionFails() {
        when(session.key()).thenReturn(channel);
        // Logout will call hasSession
        when(sessionStore.hasSession(channel)).thenThrow(new RuntimeException("Execution error"));

        ServerResponse response = executor.execute("logout", session);

        assertEquals(Status.ERROR, response.status(),
                "Status should be ERROR when the command execution throws an exception");
    }

    @Test
    void testExecuteReturnsExceptionMessageInResponseOnFailure() {
        String errorMessage = "Execution error";
        when(session.key()).thenReturn(channel);
        when(sessionStore.hasSession(channel)).thenThrow(new RuntimeException(errorMessage));

        ServerResponse response = executor.execute("logout", session);

        assertEquals(errorMessage, response.message(),
                "Response message should contain the exception message on failure");
    }

    @Test
    void testExecuteReturnsUnknownCommandTypeForInvalidInput() {
        // Unknown commands created by CommandFactory don't hit validation/session checks
        // until execute() is called, which CommandExecutor does.
        ServerResponse response = executor.execute("not-a-command", session);

        assertEquals(CommandType.UNKNOWN, response.command(),
                "Response command type should be UNKNOWN for unrecognized input strings");
    }
}
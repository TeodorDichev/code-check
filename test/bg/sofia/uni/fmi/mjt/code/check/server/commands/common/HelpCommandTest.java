package bg.sofia.uni.fmi.mjt.code.check.server.commands.common;

import bg.sofia.uni.fmi.mjt.code.check.contract.ServerResponse;
import bg.sofia.uni.fmi.mjt.code.check.contract.Status;
import bg.sofia.uni.fmi.mjt.code.check.server.commands.CommandType;
import bg.sofia.uni.fmi.mjt.code.check.server.entities.User;
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
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HelpCommandTest {

    @Mock
    private SessionStore sessionStore;
    @Mock
    private CodeCheckContext context;
    @Mock
    private ValidationService validationService;
    @Mock
    private Logger logger;
    @Mock
    private Session session;
    @Mock
    private SocketChannel channel;
    @Mock
    private User user;

    private HelpCommand command;
    private final List<String> args = List.of();

    @BeforeEach
    void setUp() {
        command = new HelpCommand(args);
        command.addDependencies(sessionStore, context);
        command.addSessionContext(session);
        command.addLogger(logger);
    }

    @Test
    void testExecuteSuccess() {
        when(context.validationService()).thenReturn(validationService);
        when(session.key()).thenReturn(channel);
        when(sessionStore.getUser(channel)).thenReturn(user);

        ServerResponse response = command.execute();

        assertEquals(Status.OK, response.status(),
                "Help command should return OK status when executed correctly");
        assertEquals("Help command", response.message(),
                "Help command should return the correct response message");
        assertEquals(user, response.user(),
                "Response should include the user currently in session");
        assertNotNull(response.data(),
                "Help command data (command descriptions) should not be null");

        // Verify it contains the actual help map
        Map<?, ?> data = (Map<?, ?>) response.data();
        assertEquals(4, data.size(),
                "Help command should return descriptions for exactly 4 command categories/groups");

        verify(validationService).validateListParamsCount(args, 0);
    }

    @Test
    void testExecuteValidationFails() {
        List<String> invalidArgs = List.of("extra_arg");
        HelpCommand invalidCommand = new HelpCommand(invalidArgs);
        invalidCommand.addDependencies(sessionStore, context);
        invalidCommand.addSessionContext(session);
        invalidCommand.addLogger(logger);

        RuntimeException validationEx = new RuntimeException("Invalid params count");
        when(context.validationService()).thenReturn(validationService);

        doThrow(validationEx).when(validationService).validateListParamsCount(invalidArgs, 0);

        ServerResponse response = invalidCommand.execute();

        assertEquals(Status.ERROR, response.status(),
                "Should return ERROR status if extra arguments are provided to the help command");

        verify(logger).logError(anyString(), any(Exception.class));
    }

    @Test
    void testGetType() {
        assertEquals(CommandType.HELP, command.getType(),
                "Command type must be HELP");
    }
}
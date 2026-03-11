package bg.sofia.uni.fmi.mjt.code.check.server.commands.account;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LogoutCommandTest {

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

    private LogoutCommand logoutCommand;
    private final List<String> args = List.of();

    @BeforeEach
    void setUp() {
        logoutCommand = new LogoutCommand(args);
        logoutCommand.addDependencies(sessionStore, context);
        logoutCommand.addSessionContext(session);
        logoutCommand.addLogger(logger);
    }

    private void setupAuthenticatedSession() {
        when(session.key()).thenReturn(channel);
        when(sessionStore.hasSession(channel)).thenReturn(true);
        when(sessionStore.getUser(channel)).thenReturn(user);
        when(context.validationService()).thenReturn(validationService);
    }

    @Test
    void testExecuteRemovesSessionFromStore() {
        setupAuthenticatedSession();

        logoutCommand.execute();

        verify(validationService).validateListParamsCount(args, 0);
        verify(sessionStore).remove(channel);
    }

    @Test
    void testExecuteReturnsOkStatusUponSuccessfulLogout() {
        setupAuthenticatedSession();

        ServerResponse response = logoutCommand.execute();

        assertEquals(Status.OK, response.status(),
                "Logout should return OK status when a valid session is removed");
    }

    @Test
    void testExecuteReturnsLogoutCommandType() {
        setupAuthenticatedSession();

        ServerResponse response = logoutCommand.execute();

        assertEquals(CommandType.LOGOUT, response.command(),
                "Response command type should be LOGOUT");
    }

    @Test
    void testExecuteReturnsErrorWhenValidationFails() {
        setupAuthenticatedSession();

        RuntimeException validationEx = new RuntimeException("Too many args");
        doThrow(validationEx).when(validationService).validateListParamsCount(args, 0);

        ServerResponse response = logoutCommand.execute();

        assertEquals(Status.ERROR, response.status(),
                "Should return ERROR status if parameter validation fails");
        verify(logger).logError(anyString(), any(Exception.class));
    }

    @Test
    void testExecuteReturnsErrorStatusWhenSessionStoreThrowsException() {
        setupAuthenticatedSession();
        doThrow(new RuntimeException("Store error")).when(sessionStore).remove(channel);

        ServerResponse response = logoutCommand.execute();

        assertEquals(Status.ERROR, response.status(),
                "Should return ERROR status if the session store fails to remove the channel");
        verify(logger).logError(anyString(), any(Exception.class));
    }

    @Test
    void testExecuteReturnsAuthenticationErrorWhenNoActiveSession() {
        when(session.key()).thenReturn(channel);
        when(sessionStore.hasSession(channel)).thenReturn(false);

        ServerResponse response = logoutCommand.execute();

        assertEquals(Status.ERROR, response.status(),
                "Logout should return ERROR if there is no active session for the channel");
    }

    @Test
    void testGetType() {
        assertEquals(CommandType.LOGOUT, logoutCommand.getType(),
                "Command type must be LOGOUT");
    }
}
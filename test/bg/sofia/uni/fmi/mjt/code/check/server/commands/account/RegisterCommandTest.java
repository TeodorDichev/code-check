package bg.sofia.uni.fmi.mjt.code.check.server.commands.account;

import bg.sofia.uni.fmi.mjt.code.check.contract.ServerResponse;
import bg.sofia.uni.fmi.mjt.code.check.contract.Status;
import bg.sofia.uni.fmi.mjt.code.check.server.commands.CommandType;
import bg.sofia.uni.fmi.mjt.code.check.server.entities.User;
import bg.sofia.uni.fmi.mjt.code.check.server.repositories.CodeCheckContext;
import bg.sofia.uni.fmi.mjt.code.check.server.repositories.UserRepository;
import bg.sofia.uni.fmi.mjt.code.check.server.services.PasswordService;
import bg.sofia.uni.fmi.mjt.code.check.server.services.ValidationService;
import bg.sofia.uni.fmi.mjt.code.check.server.services.hash.algorithm.Sha256Algorithm;
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
class RegisterCommandTest {

    @Mock
    private SessionStore sessionStore;
    @Mock
    private CodeCheckContext context;
    @Mock
    private ValidationService validationService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordService passwordService;
    @Mock
    private Logger logger;
    @Mock
    private Session session;
    @Mock
    private SocketChannel channel;

    private RegisterCommand registerCommand;
    private final List<String> args = List.of("newUser", "password123");

    @BeforeEach
    void setUp() {
        registerCommand = new RegisterCommand(args);
        registerCommand.addDependencies(sessionStore, context);
        registerCommand.addSessionContext(session);
        registerCommand.addLogger(logger);
    }

    private void prepareContext() {
        when(context.validationService()).thenReturn(validationService);
        when(session.key()).thenReturn(channel);
    }

    private void prepareSuccessStubs() {
        prepareContext();
        when(sessionStore.hasSession(channel)).thenReturn(false);
        when(context.passwordService()).thenReturn(passwordService);
        when(context.userRepository()).thenReturn(userRepository);
        when(passwordService.hashPassword(anyString(), any(Sha256Algorithm.class))).thenReturn("hashed");
    }

    @Test
    void testExecuteHashesPasswordAndAddsUser() {
        prepareSuccessStubs();

        registerCommand.execute();

        verify(validationService).validateListParamsCount(args, 2);
        verify(passwordService).hashPassword(anyString(), any(Sha256Algorithm.class));
        verify(userRepository).add(any(User.class));
        verify(sessionStore).register(any(Session.class));
    }

    @Test
    void testExecuteReturnsErrorWhenUserIsAlreadyLoggedIn() {
        prepareContext();
        when(sessionStore.hasSession(channel)).thenReturn(true);

        ServerResponse response = registerCommand.execute();

        assertEquals(Status.ERROR, response.status(),
                "Registration should fail if a user is already logged into the current session");
    }

    @Test
    void testExecuteReturnsErrorWhenRepositoryFails() {
        prepareSuccessStubs();
        doThrow(new RuntimeException("DB Error")).when(userRepository).add(any(User.class));

        ServerResponse response = registerCommand.execute();

        assertEquals(Status.ERROR, response.status(),
                "Should return ERROR status if the user repository fails to save the new user");
        verify(logger).logError(anyString(), any(Exception.class));
    }

    @Test
    void testExecuteReturnsOkStatusOnSuccess() {
        prepareSuccessStubs();

        ServerResponse response = registerCommand.execute();

        assertEquals(Status.OK, response.status(),
                "Should return OK status upon successful user registration and session creation");
    }

    @Test
    void testExecuteLogsErrorOnUnexpectedException() {
        when(context.validationService()).thenReturn(validationService);
        when(session.key()).thenThrow(new RuntimeException("Crash"));

        ServerResponse response = registerCommand.execute();

        assertEquals(Status.ERROR, response.status(),
                "Should return ERROR status when an unhandled exception occurs during registration");
        verify(logger).logError(anyString(), any(Exception.class));
    }

    @Test
    void testGetType() {
        assertEquals(CommandType.REGISTER, registerCommand.getType(),
                "Command type must be REGISTER");
    }
}
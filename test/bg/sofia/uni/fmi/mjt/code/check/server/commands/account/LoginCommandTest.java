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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginCommandTest {

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
    @Mock
    private User user;

    private LoginCommand loginCommand;
    private final List<String> args = List.of("username", "password");

    @BeforeEach
    void setUp() {
        loginCommand = new LoginCommand(args);
        loginCommand.addDependencies(sessionStore, context);
        loginCommand.addSessionContext(session);
        loginCommand.addLogger(logger);
    }

    private void prepareContext() {
        when(context.validationService()).thenReturn(validationService);
        when(session.key()).thenReturn(channel);
    }

    @Test
    void testExecuteReturnsErrorWhenUserIsAlreadyLoggedIn() {
        prepareContext();
        when(sessionStore.hasSession(channel)).thenReturn(true);

        ServerResponse response = loginCommand.execute();

        assertEquals(Status.ERROR, response.status(),
                "Login should fail if the current session is already authenticated");
    }

    @Test
    void testExecuteReturnsErrorWhenUserNotFound() {
        prepareContext();
        when(sessionStore.hasSession(channel)).thenReturn(false);
        when(context.userRepository()).thenReturn(userRepository);
        when(userRepository.get("username")).thenReturn(null);

        ServerResponse response = loginCommand.execute();

        assertEquals(Status.ERROR, response.status(),
                "Login should return ERROR status when the provided username does not exist");
    }

    @Test
    void testExecuteReturnsErrorWhenPasswordVerificationFails() {
        prepareContext();
        when(sessionStore.hasSession(channel)).thenReturn(false);
        when(context.userRepository()).thenReturn(userRepository);
        when(context.passwordService()).thenReturn(passwordService);
        when(userRepository.get("username")).thenReturn(user);
        when(user.passwordHash()).thenReturn("hashedPass");

        when(passwordService.checkPassword(anyString(), anyString(), any(Sha256Algorithm.class)))
                .thenReturn(false);

        ServerResponse response = loginCommand.execute();

        assertEquals(Status.ERROR, response.status(),
                "Login should return ERROR status when the password verification fails");
    }

    @Test
    void testExecuteRegistersSessionUponSuccessfulLogin() {
        prepareContext();
        when(sessionStore.hasSession(channel)).thenReturn(false);
        when(context.userRepository()).thenReturn(userRepository);
        when(context.passwordService()).thenReturn(passwordService);
        when(userRepository.get("username")).thenReturn(user);
        when(user.passwordHash()).thenReturn("hashedPass");

        when(passwordService.checkPassword(anyString(), anyString(), any(Sha256Algorithm.class)))
                .thenReturn(true);

        loginCommand.execute();

        verify(validationService).validateListParamsCount(args, 2);
        verify(sessionStore).register(any(Session.class));
    }

    @Test
    void testExecuteReturnsOkStatusUponSuccessfulLogin() {
        prepareContext();
        when(sessionStore.hasSession(channel)).thenReturn(false);
        when(context.userRepository()).thenReturn(userRepository);
        when(context.passwordService()).thenReturn(passwordService);
        when(userRepository.get("username")).thenReturn(user);
        when(user.passwordHash()).thenReturn("hashedPass");

        when(passwordService.checkPassword(anyString(), anyString(), any(Sha256Algorithm.class)))
                .thenReturn(true);

        ServerResponse response = loginCommand.execute();

        assertEquals(Status.OK, response.status(),
                "Login should return OK status upon successful credentials verification");
    }

    @Test
    void testExecuteLogsErrorOnUnexpectedException() {
        when(context.validationService()).thenReturn(validationService);
        when(session.key()).thenThrow(new RuntimeException("Unexpected"));

        ServerResponse response = loginCommand.execute();

        assertEquals(Status.ERROR, response.status(),
                "Login should return ERROR status when an unexpected exception is thrown");
        verify(logger).logError(anyString(), any(Exception.class));
    }

    @Test
    void testGetType() {
        assertEquals(CommandType.LOGIN, loginCommand.getType(),
                "Command type should be LOGIN");
    }
}
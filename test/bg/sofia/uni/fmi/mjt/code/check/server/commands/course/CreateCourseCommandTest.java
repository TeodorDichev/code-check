package bg.sofia.uni.fmi.mjt.code.check.server.commands.course;

import bg.sofia.uni.fmi.mjt.code.check.contract.ServerResponse;
import bg.sofia.uni.fmi.mjt.code.check.contract.Status;
import bg.sofia.uni.fmi.mjt.code.check.server.commands.CommandType;
import bg.sofia.uni.fmi.mjt.code.check.server.entities.Course;
import bg.sofia.uni.fmi.mjt.code.check.server.entities.User;
import bg.sofia.uni.fmi.mjt.code.check.server.repositories.CodeCheckContext;
import bg.sofia.uni.fmi.mjt.code.check.server.repositories.CourseRepository;
import bg.sofia.uni.fmi.mjt.code.check.server.repositories.UserRepository;
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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateCourseCommandTest {

    @Mock private SessionStore sessionStore;
    @Mock private CodeCheckContext context;
    @Mock private ValidationService validationService;
    @Mock private CourseRepository courseRepository;
    @Mock private UserRepository userRepository;
    @Mock private Logger logger;
    @Mock private Session session;
    @Mock private SocketChannel channel;
    @Mock private User user;

    private CreateCourseCommand command;
    private final List<String> args = List.of("ModernJava");

    @BeforeEach
    void setUp() {
        command = new CreateCourseCommand(args);
        command.addDependencies(sessionStore, context);
        command.addSessionContext(session);
        command.addLogger(logger);
    }

    @Test
    void testExecuteSuccess() {
        when(session.key()).thenReturn(channel);
        when(sessionStore.hasSession(channel)).thenReturn(true);
        when(sessionStore.getUser(channel)).thenReturn(user);
        when(user.username()).thenReturn("instructor");

        when(context.validationService()).thenReturn(validationService);
        when(context.courseRepository()).thenReturn(courseRepository);
        when(context.userRepository()).thenReturn(userRepository);

        ServerResponse response = command.execute();

        assertEquals(Status.OK, response.status(),
                "Response status should be OK when course is created successfully");
        assertInstanceOf(Course.class, response.data(),
                "Response should contain proper data");

        Course created = (Course) response.data();
        verify(courseRepository).add(any(Course.class));
        verify(userRepository).addAdministeredCourse(user, created.id());
    }

    @Test
    void testExecuteUnsafeCourseName() {
        when(session.key()).thenReturn(channel);
        when(sessionStore.hasSession(channel)).thenReturn(true);
        when(sessionStore.getUser(channel)).thenReturn(user);
        when(context.validationService()).thenReturn(validationService);

        RuntimeException unsafeEx = new RuntimeException("Illegal symbols");
        doThrow(unsafeEx).when(validationService).throwIfUnsafe("ModernJava",
                "Course name cannot be null, empty or contain illegal symbols");

        ServerResponse response = command.execute();

        assertEquals(Status.ERROR, response.status(),
                "Should return ERROR status if validation service deems the course name unsafe");
        verify(logger).logError(anyString(), any(Exception.class));
    }

    @Test
    void testExecuteValidationParamsCountFails() {
        when(session.key()).thenReturn(channel);
        when(sessionStore.hasSession(channel)).thenReturn(true);
        when(sessionStore.getUser(channel)).thenReturn(user);
        when(context.validationService()).thenReturn(validationService);

        RuntimeException valEx = new RuntimeException("Wrong params");
        doThrow(valEx).when(validationService).validateListParamsCount(args, 1);

        ServerResponse response = command.execute();

        assertEquals(Status.ERROR, response.status(),
                "Should return ERROR status if the number of arguments is incorrect");
        verify(logger).logError(anyString(), any(Exception.class));
    }

    @Test
    void testExecuteUnexpectedException() {
        when(session.key()).thenReturn(channel);
        when(sessionStore.hasSession(channel)).thenReturn(true);
        when(sessionStore.getUser(channel)).thenReturn(user);
        when(user.username()).thenReturn("instructor");
        when(context.validationService()).thenReturn(validationService);
        when(context.courseRepository()).thenReturn(courseRepository);

        doThrow(new RuntimeException("Lock fail")).when(courseRepository).add(any(Course.class));

        ServerResponse response = command.execute();

        assertEquals(Status.ERROR, response.status(),
                "Should return ERROR status when an unexpected runtime exception occurs during course addition");
        verify(logger).logError(anyString(), any(Exception.class));
    }

    @Test
    void testGetType() {
        assertEquals(CommandType.CREATE_COURSE, command.getType(),
                "Command type should match CREATE_COURSE");
    }
}
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
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JoinCourseCommandTest {

    @Mock private SessionStore sessionStore;
    @Mock private CodeCheckContext context;
    @Mock private ValidationService validationService;
    @Mock private CourseRepository courseRepository;
    @Mock private UserRepository userRepository;
    @Mock private Logger logger;
    @Mock private Session session;
    @Mock private SocketChannel channel;
    @Mock private Course course;
    @Mock private User user;

    private JoinCourseCommand command;
    private final List<String> args = List.of("Java", "secret_code");

    @BeforeEach
    void setUp() {
        command = new JoinCourseCommand(args);
        command.addDependencies(sessionStore, context);
        command.addSessionContext(session);
        command.addLogger(logger);
    }

    private void prepareBasicAuth() {
        when(session.key()).thenReturn(channel);
        when(sessionStore.hasSession(channel)).thenReturn(true);
        when(sessionStore.getUser(channel)).thenReturn(user);
    }

    @Test
    void testExecuteSuccess() {
        prepareBasicAuth();
        when(user.username()).thenReturn("tester");
        when(user.enrolledCourseIds()).thenReturn(Set.of());
        when(user.administeredCoursesIds()).thenReturn(Set.of());

        when(context.validationService()).thenReturn(validationService);
        when(context.courseRepository()).thenReturn(courseRepository);
        when(context.userRepository()).thenReturn(userRepository);

        when(courseRepository.getCourseByNameAndJoinString("Java", "secret_code")).thenReturn(course);
        when(course.id()).thenReturn("Java_id");

        ServerResponse response = command.execute();

        assertEquals(Status.OK, response.status(),
                "Join course should return OK status when credentials are correct and student is not enrolled");
        assertInstanceOf(Course.class, response.data(),
                "Response should contain proper data");
        verify(courseRepository).enrollStudent(any(Course.class), anyString());
        verify(userRepository).addEnrolledCourse(any(User.class), anyString());
    }

    @Test
    void testExecuteCourseDoesNotExist() {
        prepareBasicAuth();
        when(context.validationService()).thenReturn(validationService);
        when(context.courseRepository()).thenReturn(courseRepository);

        when(courseRepository.getCourseByNameAndJoinString("Java", "secret_code")).thenReturn(null);

        ServerResponse response = command.execute();

        assertEquals(Status.ERROR, response.status(),
                "Should return ERROR status if no course matches the provided name and join string");
        verify(logger).logError(anyString(), any(Exception.class));
    }

    @Test
    void testExecuteAlreadyEnrolled() {
        prepareBasicAuth();
        when(user.enrolledCourseIds()).thenReturn(Set.of("Java_id"));

        when(context.validationService()).thenReturn(validationService);
        when(context.courseRepository()).thenReturn(courseRepository);
        when(courseRepository.getCourseByNameAndJoinString("Java", "secret_code")).thenReturn(course);
        when(course.id()).thenReturn("Java_id");
        when(course.name()).thenReturn("Java");

        ServerResponse response = command.execute();

        assertEquals(Status.ERROR, response.status(),
                "Should return ERROR status if the student is already enrolled in the course");
    }

    @Test
    void testExecuteValidationFails() {
        prepareBasicAuth();
        when(context.validationService()).thenReturn(validationService);

        RuntimeException ex = new RuntimeException("Validation failed");
        doThrow(ex).when(validationService).validateListParamsCount(args, 2);

        ServerResponse response = command.execute();

        assertEquals(Status.ERROR, response.status(),
                "Should return ERROR status if the argument validation fails");
        verify(logger).logError(anyString(), any(Exception.class));
    }

    @Test
    void testGetType() {
        assertEquals(CommandType.JOIN_COURSE, command.getType(),
                "Command type must match JOIN_COURSE");
    }
}
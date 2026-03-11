package bg.sofia.uni.fmi.mjt.code.check.server.commands.course;

import bg.sofia.uni.fmi.mjt.code.check.contract.ServerResponse;
import bg.sofia.uni.fmi.mjt.code.check.contract.Status;
import bg.sofia.uni.fmi.mjt.code.check.server.commands.CommandType;
import bg.sofia.uni.fmi.mjt.code.check.server.entities.Course;
import bg.sofia.uni.fmi.mjt.code.check.server.entities.User;
import bg.sofia.uni.fmi.mjt.code.check.server.models.ListCoursesModel;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListCoursesCommandTest {

    @Mock private SessionStore sessionStore;
    @Mock private CodeCheckContext context;
    @Mock private ValidationService validationService;
    @Mock private CourseRepository courseRepository;
    @Mock private UserRepository userRepository;
    @Mock private Logger logger;
    @Mock private Session session;
    @Mock private SocketChannel channel;
    @Mock private Course course;
    @Mock private User mockUser;

    private ListCoursesCommand command;
    private final List<String> args = List.of();

    @BeforeEach
    void setUp() {
        command = new ListCoursesCommand(args);
        command.addDependencies(sessionStore, context);
        command.addSessionContext(session);
        command.addLogger(logger);
    }

    private void prepareAuth() {
        when(session.key()).thenReturn(channel);
        when(sessionStore.hasSession(channel)).thenReturn(true);
        when(sessionStore.getUser(channel)).thenReturn(mockUser);
        when(mockUser.username()).thenReturn("tester");
    }

    @Test
    void testExecuteSuccess() {
        prepareAuth();
        when(mockUser.administeredCoursesIds()).thenReturn(Set.of("admin_id"));
        when(mockUser.enrolledCourseIds()).thenReturn(Set.of("enroll_id"));

        when(context.validationService()).thenReturn(validationService);
        when(context.courseRepository()).thenReturn(courseRepository);

        when(courseRepository.get("admin_id")).thenReturn(course);
        when(courseRepository.get("enroll_id")).thenReturn(course);

        ServerResponse response = command.execute();

        assertEquals(Status.OK, response.status(),
                "The command should return OK status when listing courses for an authenticated user");
        assertInstanceOf(ListCoursesModel.class, response.data(),
                "The response data should be an instance of ListCoursesModel containing both admin and enrolled courses");
        verify(validationService).validateListParamsCount(args, 0);
    }

    @Test
    void testExecuteFixesInconsistency() {
        prepareAuth();
        when(mockUser.administeredCoursesIds()).thenReturn(Set.of("ghost_admin"));
        when(mockUser.enrolledCourseIds()).thenReturn(Set.of("ghost_enroll"));

        when(context.validationService()).thenReturn(validationService);
        when(context.courseRepository()).thenReturn(courseRepository);
        when(context.userRepository()).thenReturn(userRepository);

        // Simulate ghost courses that exist in User profile but not in CourseRepository
        when(courseRepository.get("ghost_admin")).thenReturn(null);
        when(courseRepository.get("ghost_enroll")).thenReturn(null);

        ServerResponse response = command.execute();

        assertEquals(Status.OK, response.status(),
                "The command should handle missing course entities gracefully and return OK");

        // Ensure the system cleans up the user's state when inconsistencies are found
        verify(userRepository).removeAdministeredCourse(mockUser, "ghost_admin");
        verify(userRepository).removeEnrolledCourse(mockUser, "ghost_enroll");
        verify(logger, times(2)).logWarning(anyString());
    }

    @Test
    void testExecuteExceptionLogging() {
        when(session.key()).thenReturn(channel);
        when(sessionStore.hasSession(channel)).thenReturn(true);
        when(sessionStore.getUser(channel)).thenReturn(mockUser);
        when(mockUser.username()).thenReturn("tester");

        when(context.validationService()).thenReturn(validationService);

        RuntimeException crash = new RuntimeException("Validation crash");
        doThrow(crash).when(validationService).validateListParamsCount(args, 0);

        ServerResponse response = command.execute();

        assertEquals(Status.ERROR, response.status(),
                "The command should return ERROR status when an unexpected exception occurs");
        verify(logger).logError(anyString(), any(Exception.class));
    }

    @Test
    void testGetType() {
        assertEquals(CommandType.LIST_COURSES, command.getType(),
                "The command type must be LIST_COURSES");
    }
}
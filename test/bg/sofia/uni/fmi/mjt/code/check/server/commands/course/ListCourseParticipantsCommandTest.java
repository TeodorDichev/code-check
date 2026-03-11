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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListCourseParticipantsCommandTest {

    @Mock private SessionStore sessionStore;
    @Mock private CodeCheckContext context;
    @Mock private ValidationService validationService;
    @Mock private CourseRepository courseRepository;
    @Mock private UserRepository userRepository;
    @Mock private Logger logger;
    @Mock private Session session;
    @Mock private SocketChannel channel;
    @Mock private Course course;
    @Mock private User student;
    @Mock private User admin;

    private ListCourseParticipantsCommand command;
    private final List<String> args = List.of("Java");

    @BeforeEach
    void setUp() {
        command = new ListCourseParticipantsCommand(args);
        command.addDependencies(sessionStore, context);
        command.addSessionContext(session);
        command.addLogger(logger);
    }

    private void prepareAuth() {
        when(session.key()).thenReturn(channel);
        when(sessionStore.hasSession(channel)).thenReturn(true);
        when(sessionStore.getUser(channel)).thenReturn(admin);
    }

    @Test
    void testExecuteSuccess() {
        prepareAuth();
        when(admin.getAdminCourseIdFromName("Java")).thenReturn("Java_id");

        when(context.validationService()).thenReturn(validationService);
        when(context.courseRepository()).thenReturn(courseRepository);
        when(context.userRepository()).thenReturn(userRepository);

        when(courseRepository.get("Java_id")).thenReturn(course);
        when(course.studentUsernames()).thenReturn(Set.of("student1"));
        when(userRepository.get("student1")).thenReturn(student);

        ServerResponse response = command.execute();

        assertEquals(Status.OK, response.status(),
                "Status should be OK when listing participants for a course administered by the user");
        assertTrue(((Set<?>) response.data()).contains(student),
                "The response data should contain the student objects belonging to the course");
        verify(validationService).throwIfUnsafe(anyString(), anyString());
    }

    @Test
    void testExecuteUnauthorized() {
        prepareAuth();
        when(admin.getAdminCourseIdFromName("Java")).thenReturn(null);
        when(context.validationService()).thenReturn(validationService);

        ServerResponse response = command.execute();

        assertEquals(Status.ERROR, response.status(),
                "Should return ERROR status if a student or non-admin tries to list course participants");
        assertTrue(response.message().contains("Only course administrators"),
                "Response message should explicitly state that only admins can access participant lists");
    }

    @Test
    void testHandleInconsistencyMissingStudent() {
        prepareAuth();
        when(admin.getAdminCourseIdFromName("Java")).thenReturn("Java_id");

        when(context.validationService()).thenReturn(validationService);
        when(context.courseRepository()).thenReturn(courseRepository);
        when(context.userRepository()).thenReturn(userRepository);

        when(courseRepository.get("Java_id")).thenReturn(course);
        when(course.id()).thenReturn("Java_id");
        when(course.studentUsernames()).thenReturn(Set.of("missingStudent"));
        when(userRepository.get("missingStudent")).thenReturn(null);

        ServerResponse response = command.execute();

        assertEquals(Status.OK, response.status(),
                "Should still return OK status even if some student records are missing from the repository");
        verify(courseRepository).removeStudent(any(Course.class), anyString());
        verify(logger).logWarning(anyString());
    }

    @Test
    void testExecuteUnexpectedException() {
        prepareAuth();
        when(context.validationService()).thenReturn(validationService);

        RuntimeException crash = new RuntimeException("Crash");
        doThrow(crash).when(validationService).validateListParamsCount(args, 1);

        ServerResponse response = command.execute();

        assertEquals(Status.ERROR, response.status(),
                "Should return ERROR status when an unhandled exception is caught during execution");
        verify(logger).logError(anyString(), any(Exception.class));
    }

    @Test
    void testGetType() {
        assertEquals(CommandType.LIST_COURSE_PARTICIPANTS, command.getType(),
                "The command type must return LIST_COURSE_PARTICIPANTS");
    }
}
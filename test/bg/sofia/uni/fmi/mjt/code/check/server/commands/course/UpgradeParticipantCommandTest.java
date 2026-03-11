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
class UpgradeParticipantCommandTest {

    @Mock private SessionStore sessionStore;
    @Mock private CodeCheckContext context;
    @Mock private ValidationService validationService;
    @Mock private CourseRepository courseRepository;
    @Mock private UserRepository userRepository;
    @Mock private Logger logger;
    @Mock private Session session;
    @Mock private SocketChannel channel;
    @Mock private Course course;
    @Mock private User targetUser;
    @Mock private User mockAdmin;

    private UpgradeParticipantCommand command;
    private final List<String> args = List.of("Java", "student123");

    @BeforeEach
    void setUp() {
        command = new UpgradeParticipantCommand(args);
        command.addDependencies(sessionStore, context);
        command.addSessionContext(session);
        command.addLogger(logger);
    }

    private void prepareAuth() {
        when(session.key()).thenReturn(channel);
        when(sessionStore.hasSession(channel)).thenReturn(true);
        when(sessionStore.getUser(channel)).thenReturn(mockAdmin);
    }

    @Test
    void testExecuteSuccess() {
        prepareAuth();
        when(mockAdmin.getAdminCourseIdFromName("Java")).thenReturn("Java_id");

        when(context.validationService()).thenReturn(validationService);
        when(context.courseRepository()).thenReturn(courseRepository);
        when(context.userRepository()).thenReturn(userRepository);

        when(courseRepository.get("Java_id")).thenReturn(course);
        when(userRepository.get("student123")).thenReturn(targetUser);
        when(targetUser.username()).thenReturn("student123");
        when(course.id()).thenReturn("Java_id");

        ServerResponse response = command.execute();

        assertEquals(Status.OK, response.status(),
                "Status should be OK when a valid admin upgrades an existing student to admin");
        assertInstanceOf(Course.class, response.data(),
                "Response should contain proper data");

        // Verify full transaction (Remove Student -> Add Admin)
        verify(userRepository).removeEnrolledCourse(any(User.class), anyString());
        verify(courseRepository).removeStudent(any(Course.class), anyString());
        verify(userRepository).addAdministeredCourse(any(User.class), anyString());
        verify(courseRepository).addAdmin(any(Course.class), anyString());
    }

    @Test
    void testExecuteTargetAlreadyAdminThrows() {
        prepareAuth();
        when(mockAdmin.getAdminCourseIdFromName("Java")).thenReturn("Java_id");

        when(context.validationService()).thenReturn(validationService);
        when(context.courseRepository()).thenReturn(courseRepository);
        when(context.userRepository()).thenReturn(userRepository);

        when(courseRepository.get("Java_id")).thenReturn(course);
        when(userRepository.get("student123")).thenReturn(targetUser);
        when(targetUser.username()).thenReturn("student123");

        RuntimeException alreadyExists = new RuntimeException("Already admin");
        doThrow(alreadyExists).when(validationService).throwEntityAlreadyExistsIfContained(any(), anyString(), anyString());

        ServerResponse response = command.execute();

        assertEquals(Status.ERROR, response.status(),
                "Should return ERROR status if the target user is already an administrator of the course");
        verify(logger).logError(anyString(), any(Exception.class));
    }

    @Test
    void testExecuteUnauthorized() {
        prepareAuth();
        // admin does not administer "Java"
        when(mockAdmin.getAdminCourseIdFromName("Java")).thenReturn(null);
        when(context.validationService()).thenReturn(validationService);
        when(context.courseRepository()).thenReturn(courseRepository);
        when(context.userRepository()).thenReturn(userRepository);

        ServerResponse response = command.execute();

        assertEquals(Status.ERROR, response.status(),
                "Should return ERROR status if the requester is not an administrator of the specified course");
        verify(logger).logError(anyString(), any(Exception.class));
    }

    @Test
    void testGetType() {
        assertEquals(CommandType.UPGRADE_PARTICIPANT, command.getType(),
                "The command type must return UPGRADE_PARTICIPANT");
    }
}
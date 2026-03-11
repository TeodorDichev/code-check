package bg.sofia.uni.fmi.mjt.code.check.server.commands.assignment;

import bg.sofia.uni.fmi.mjt.code.check.contract.ServerResponse;
import bg.sofia.uni.fmi.mjt.code.check.contract.Status;
import bg.sofia.uni.fmi.mjt.code.check.server.commands.CommandType;
import bg.sofia.uni.fmi.mjt.code.check.server.entities.Assignment;
import bg.sofia.uni.fmi.mjt.code.check.server.entities.Course;
import bg.sofia.uni.fmi.mjt.code.check.server.entities.Submission;
import bg.sofia.uni.fmi.mjt.code.check.server.entities.User;
import bg.sofia.uni.fmi.mjt.code.check.server.models.ListAdminAssignmentModel;
import bg.sofia.uni.fmi.mjt.code.check.server.models.ListStudentAssignmentModel;
import bg.sofia.uni.fmi.mjt.code.check.server.repositories.AssignmentRepository;
import bg.sofia.uni.fmi.mjt.code.check.server.repositories.CodeCheckContext;
import bg.sofia.uni.fmi.mjt.code.check.server.repositories.CourseRepository;
import bg.sofia.uni.fmi.mjt.code.check.server.repositories.SubmissionRepository;
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
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ViewAssignmentCommandTest {

    @Mock private SessionStore sessionStore;
    @Mock private CodeCheckContext context;
    @Mock private ValidationService validationService;
    @Mock private CourseRepository courseRepository;
    @Mock private AssignmentRepository assignmentRepository;
    @Mock private SubmissionRepository submissionRepository;
    @Mock private Logger logger;
    @Mock private Session session;
    @Mock private SocketChannel channel;
    @Mock private Course course;
    @Mock private Assignment assignment;
    @Mock private Submission submission;
    @Mock private User user;

    private ViewAssignmentCommand command;
    private final List<String> args = List.of("Java", "Lab01");

    @BeforeEach
    void setUp() {
        command = new ViewAssignmentCommand(args);
        command.addDependencies(sessionStore, context);
        command.addSessionContext(session);
        command.addLogger(logger);
    }

    private void prepareAuth() {
        when(session.key()).thenReturn(channel);
        when(sessionStore.hasSession(channel)).thenReturn(true);
        when(sessionStore.getUser(channel)).thenReturn(user);
        when(context.validationService()).thenReturn(validationService);
    }

    @Test
    void testExecuteAdminSuccess() {
        prepareAuth();
        when(user.getAdminCourseIdFromName("Java")).thenReturn("Java_ID");
        when(user.administeredCoursesIds()).thenReturn(Set.of("Java_ID"));

        when(context.courseRepository()).thenReturn(courseRepository);
        when(context.assignmentRepository()).thenReturn(assignmentRepository);

        when(courseRepository.get("Java_ID")).thenReturn(course);
        when(course.getAssignmentIdByName("Lab01")).thenReturn("LAB_ID");
        when(assignmentRepository.get("LAB_ID")).thenReturn(assignment);
        when(assignment.userToSubmissionId()).thenReturn(Map.of("u1", "s1"));

        ServerResponse response = command.execute();

        assertEquals(Status.OK, response.status(),
                "Admin should receive an OK status when viewing an assignment they administer");
        assertInstanceOf(ListAdminAssignmentModel.class, response.data(),
                "Response data for admin should be an instance of ListAdminAssignmentModel");
    }

    @Test
    void testExecuteStudentWithSubmissionSuccess() {
        prepareAuth();
        when(user.getAdminCourseIdFromName("Java")).thenReturn(null);
        when(user.getEnrollCourseIdFromName("Java")).thenReturn("Java_ID");
        when(user.administeredCoursesIds()).thenReturn(Set.of());
        when(user.username()).thenReturn("tester");

        when(context.courseRepository()).thenReturn(courseRepository);
        when(context.assignmentRepository()).thenReturn(assignmentRepository);
        when(context.submissionRepository()).thenReturn(submissionRepository);

        when(courseRepository.get("Java_ID")).thenReturn(course);
        when(course.getAssignmentIdByName("Lab01")).thenReturn("LAB_ID");
        when(assignmentRepository.get("LAB_ID")).thenReturn(assignment);
        when(assignment.userToSubmissionId()).thenReturn(Map.of("tester", "sub_123"));
        when(submissionRepository.get("sub_123")).thenReturn(submission);

        ServerResponse response = command.execute();

        assertEquals(Status.OK, response.status(),
                "Student should receive an OK status when viewing an assignment in a course they are enrolled in");
        assertInstanceOf(ListStudentAssignmentModel.class, response.data(),
                "Response data for student should be an instance of ListStudentAssignmentModel");
    }

    @Test
    void testHandleMissingAssignmentInconsistency() {
        prepareAuth();
        when(user.getAdminCourseIdFromName("Java")).thenReturn("Java_ID");
        when(user.username()).thenReturn("tester");

        when(context.courseRepository()).thenReturn(courseRepository);
        when(context.assignmentRepository()).thenReturn(assignmentRepository);

        when(courseRepository.get("Java_ID")).thenReturn(course);
        when(course.id()).thenReturn("Java_ID");
        when(course.getAssignmentIdByName("Lab01")).thenReturn("LAB_ID");
        when(assignmentRepository.get("LAB_ID")).thenReturn(null);

        ServerResponse response = command.execute();

        assertEquals(Status.ERROR, response.status(),
                "Should return ERROR status if there is an inconsistency between course and assignment repositories");
        verify(courseRepository).removeAssignment(course, "LAB_ID");
        verify(logger).logWarning(anyString());
    }

    @Test
    void testExecuteUnexpectedExceptionLogging() {
        prepareAuth();
        when(user.getAdminCourseIdFromName(anyString())).thenThrow(new RuntimeException("Crash"));

        ServerResponse response = command.execute();

        assertEquals(Status.ERROR, response.status(),
                "Should return ERROR status when an unhandled runtime exception occurs during execution");
        verify(logger).logError(anyString(), any(Exception.class));
    }

    @Test
    void testGetType() {
        assertEquals(CommandType.VIEW_ASSIGNMENT, command.getType(),
                "The command type must be VIEW_ASSIGNMENT");
    }
}
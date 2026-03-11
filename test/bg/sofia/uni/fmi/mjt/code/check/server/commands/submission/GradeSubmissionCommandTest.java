package bg.sofia.uni.fmi.mjt.code.check.server.commands.submission;

import bg.sofia.uni.fmi.mjt.code.check.contract.ServerResponse;
import bg.sofia.uni.fmi.mjt.code.check.contract.Status;
import bg.sofia.uni.fmi.mjt.code.check.server.commands.CommandType;
import bg.sofia.uni.fmi.mjt.code.check.server.entities.Assignment;
import bg.sofia.uni.fmi.mjt.code.check.server.entities.Course;
import bg.sofia.uni.fmi.mjt.code.check.server.entities.Submission;
import bg.sofia.uni.fmi.mjt.code.check.server.entities.User;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
@ExtendWith(MockitoExtension.class)
class GradeSubmissionCommandTest {

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
    @Mock private User mockAdmin;

    private GradeSubmissionCommand command;
    private final List<String> args = List.of("Java", "Lab1", "studentUser", "5.5", "Good");

    @BeforeEach
    void setUp() {
        command = new GradeSubmissionCommand(args);
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
        when(mockAdmin.username()).thenReturn("adminUser");

        when(context.validationService()).thenReturn(validationService);
        when(context.courseRepository()).thenReturn(courseRepository);
        when(context.assignmentRepository()).thenReturn(assignmentRepository);
        when(context.submissionRepository()).thenReturn(submissionRepository);

        when(courseRepository.get("Java_id")).thenReturn(course);
        when(course.getAssignmentIdByName("Lab1")).thenReturn("Asgn_id");
        when(assignmentRepository.get("Asgn_id")).thenReturn(assignment);
        when(assignment.userToSubmissionId()).thenReturn(Map.of("studentUser", "Sub_id"));
        when(submissionRepository.get("Sub_id")).thenReturn(submission);
        when(submission.id()).thenReturn("Sub_id");

        ServerResponse response = command.execute();

        assertEquals(Status.OK, response.status(),
                "Status should be OK when a valid admin grades an existing submission");

        assertInstanceOf(Submission.class, response.data(),
                "Response data must be a non-null Submission instance");

        verify(submission).grade(anyString(), anyDouble(), anyString());
        verify(submissionRepository).gradeSubmission(any(Submission.class), anyString(), anyDouble(), anyString());
    }

    @Test
    void testExecuteUnauthorized() {
        prepareAuth();
        when(mockAdmin.getAdminCourseIdFromName("Java")).thenReturn(null);
        when(context.validationService()).thenReturn(validationService);
        when(context.courseRepository()).thenReturn(courseRepository);

        ServerResponse response = command.execute();

        assertEquals(Status.ERROR, response.status(),
                "Should return ERROR status if the user is not authorized to grade in this course");
        verify(logger).logError(anyString(), any(Exception.class));
    }

    @Test
    void testExecuteValidationCrash() {
        when(session.key()).thenReturn(channel);
        when(sessionStore.hasSession(channel)).thenReturn(true);
        when(sessionStore.getUser(channel)).thenReturn(mockAdmin);

        when(context.validationService()).thenReturn(validationService);
        RuntimeException crash = new RuntimeException("Crash");
        doThrow(crash).when(validationService).validateListParamsCount(args, 5);

        ServerResponse response = command.execute();

        assertEquals(Status.ERROR, response.status(),
                "Should return ERROR status when validation service crashes");
        verify(logger).logError(anyString(), any(Exception.class));
    }

    @Test
    void testGetType() {
        assertEquals(CommandType.GRADE_SUBMISSION, command.getType(),
                "Command type should match GRADE_SUBMISSION");
    }
}
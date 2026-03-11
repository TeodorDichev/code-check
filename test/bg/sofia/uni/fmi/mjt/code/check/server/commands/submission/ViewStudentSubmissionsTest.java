package bg.sofia.uni.fmi.mjt.code.check.server.commands.submission;

import bg.sofia.uni.fmi.mjt.code.check.contract.ServerResponse;
import bg.sofia.uni.fmi.mjt.code.check.contract.Status;
import bg.sofia.uni.fmi.mjt.code.check.server.commands.CommandType;
import bg.sofia.uni.fmi.mjt.code.check.server.entities.Assignment;
import bg.sofia.uni.fmi.mjt.code.check.server.entities.Course;
import bg.sofia.uni.fmi.mjt.code.check.server.entities.Submission;
import bg.sofia.uni.fmi.mjt.code.check.server.entities.User;
import bg.sofia.uni.fmi.mjt.code.check.server.models.SubmissionView;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ViewStudentSubmissionsTest {

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

    private ViewStudentSubmissions command;
    private final List<String> args = List.of("Java", "Homework", "student123");

    @BeforeEach
    void setUp() {
        command = new ViewStudentSubmissions(args);
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
        when(context.assignmentRepository()).thenReturn(assignmentRepository);
        when(context.submissionRepository()).thenReturn(submissionRepository);

        when(courseRepository.get("Java_id")).thenReturn(course);
        when(course.getAssignmentIdByName("Homework")).thenReturn("asgn_id");
        when(assignmentRepository.get("asgn_id")).thenReturn(assignment);
        when(assignment.userToSubmissionId()).thenReturn(Map.of("student123", "sub_id"));
        when(submissionRepository.get("sub_id")).thenReturn(submission);
        when(submission.path()).thenReturn(null);

        ServerResponse response = command.execute();

        assertEquals(Status.OK, response.status(),
                "Status should be OK when an administrator views a valid student submission");

        assertInstanceOf(SubmissionView.class, response.data(),
                "Response data must be a non-null instance of SubmissionView upon successful retrieval");
    }

    @Test
    void testExecuteCourseDoesNotExist() {
        prepareAuth();
        when(mockAdmin.getAdminCourseIdFromName("Java")).thenReturn(null);

        when(context.validationService()).thenReturn(validationService);
        when(context.courseRepository()).thenReturn(courseRepository);

        RuntimeException ex = new RuntimeException("Course not found");
        doThrow(ex).when(validationService).throwEntityDoesNotExistIfNull(any(), anyString());

        ServerResponse response = command.execute();

        assertEquals(Status.ERROR, response.status(),
                "Should return ERROR status if the course does not exist or user is not an admin");
        verify(logger).logError(anyString(), any(Exception.class));
    }

    @Test
    void testExecuteIoExceptionInWalk() {
        prepareAuth();
        when(mockAdmin.getAdminCourseIdFromName("Java")).thenReturn("Java_id");

        when(context.validationService()).thenReturn(validationService);
        when(context.courseRepository()).thenReturn(courseRepository);
        when(context.assignmentRepository()).thenReturn(assignmentRepository);
        when(context.submissionRepository()).thenReturn(submissionRepository);

        when(courseRepository.get("Java_id")).thenReturn(course);
        when(course.getAssignmentIdByName("Homework")).thenReturn("asgn_id");
        when(assignmentRepository.get("asgn_id")).thenReturn(assignment);
        when(assignment.userToSubmissionId()).thenReturn(Map.of("student123", "sub_id"));
        when(submissionRepository.get("sub_id")).thenReturn(submission);

        // Triggers the IOException catch block in mapToView
        when(submission.path()).thenReturn("invalid/path/that/will/fail/walk/help/no/more/tests");

        ServerResponse response = command.execute();

        assertEquals(Status.OK, response.status(),
                "Status should remain OK even if file walking fails, allowing the admin to see at least metadata");
        verify(logger).logError(anyString(), any(Exception.class));
    }

    @Test
    void testGetType() {
        assertEquals(CommandType.VIEW_STUDENT_SUBMISSION, command.getType(),
                "Command type should match VIEW_STUDENT_SUBMISSION");
    }
}
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ViewSubmissionsCommandTest {

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

    private ViewSubmissionsCommand command;
    private final List<String> args = List.of("Java", "Lab1");

    @BeforeEach
    void setUp() {
        command = new ViewSubmissionsCommand(args);
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
        when(course.getAssignmentIdByName("Lab1")).thenReturn("asgn_id");
        when(assignmentRepository.get("asgn_id")).thenReturn(assignment);
        when(assignment.userToSubmissionId()).thenReturn(Map.of("user1", "sub_id"));
        when(submissionRepository.get("sub_id")).thenReturn(submission);
        when(submission.path()).thenReturn(null);

        ServerResponse response = command.execute();

        assertEquals(Status.OK, response.status(),
                "Status should be OK when an admin views all submissions for a valid assignment");

        assertInstanceOf(List.class, response.data(),
                "Response data should be a non-null List of submission views");

        List<?> data = (List<?>) response.data();
        assertEquals(1, data.size(),
                "The result list should contain exactly the number of submissions present in the assignment");
    }

    // https://www.baeldung.com/mockito-void-methods
    // https://stackoverflow.com/questions/28836778/usages-of-dothrow-doanswer-donothing-and-doreturn-in-mockito
    @Test
    void testExecuteAssignmentNotFound() {
        prepareAuth();
        when(mockAdmin.getAdminCourseIdFromName(anyString())).thenReturn("Java_id");
        when(context.validationService()).thenReturn(validationService);
        when(context.courseRepository()).thenReturn(courseRepository);
        when(courseRepository.get("Java_id")).thenReturn(course);

        when(context.assignmentRepository()).thenReturn(assignmentRepository);
        when(course.getAssignmentIdByName("Lab1")).thenReturn("asgn_id");
        when(assignmentRepository.get("asgn_id")).thenReturn(null);

        // First call for course validation passes, second for assignment validation fails
        doNothing().doThrow(new RuntimeException("Assignment missing"))
                .when(validationService).throwEntityDoesNotExistIfNull(any(), anyString());

        ServerResponse response = command.execute();

        assertEquals(Status.ERROR, response.status(),
                "Should return ERROR status when the specified assignment does not exist in the course");
        verify(logger).logError(anyString(), any(Exception.class));
    }

    @Test
    void testGetType() {
        assertEquals(CommandType.VIEW_SUBMISSIONS, command.getType(),
                "Command type must correctly return VIEW_SUBMISSIONS");
    }
}
package bg.sofia.uni.fmi.mjt.code.check.server.commands.submission;

import bg.sofia.uni.fmi.mjt.code.check.contract.ServerResponse;
import bg.sofia.uni.fmi.mjt.code.check.contract.Status;
import bg.sofia.uni.fmi.mjt.code.check.server.commands.CommandType;
import bg.sofia.uni.fmi.mjt.code.check.server.entities.Assignment;
import bg.sofia.uni.fmi.mjt.code.check.server.entities.Course;
import bg.sofia.uni.fmi.mjt.code.check.server.entities.Submission;
import bg.sofia.uni.fmi.mjt.code.check.server.entities.User;
import bg.sofia.uni.fmi.mjt.code.check.server.models.CompilationResult;
import bg.sofia.uni.fmi.mjt.code.check.server.repositories.AssignmentRepository;
import bg.sofia.uni.fmi.mjt.code.check.server.repositories.CodeCheckContext;
import bg.sofia.uni.fmi.mjt.code.check.server.repositories.CourseRepository;
import bg.sofia.uni.fmi.mjt.code.check.server.repositories.SubmissionRepository;
import bg.sofia.uni.fmi.mjt.code.check.server.services.CompilerService;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TestSubmissionsCommandTest {

    @Mock private SessionStore sessionStore;
    @Mock private CodeCheckContext context;
    @Mock private ValidationService validationService;
    @Mock private CourseRepository courseRepository;
    @Mock private AssignmentRepository assignmentRepository;
    @Mock private SubmissionRepository submissionRepository;
    @Mock private CompilerService compilerService;
    @Mock private Logger logger;
    @Mock private Session session;
    @Mock private SocketChannel channel;
    @Mock private Course course;
    @Mock private Assignment assignment;
    @Mock private Submission submission;
    @Mock private CompilationResult compilationResult;
    @Mock private User mockAdmin;

    private TestSubmissionsCommand command;
    private final List<String> args = List.of("Java", "Project");

    @BeforeEach
    void setUp() {
        command = new TestSubmissionsCommand(args);
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
    void testExecuteSuccessBulk() {
        prepareAuth();
        when(mockAdmin.getAdminCourseIdFromName(anyString())).thenReturn("Java_id");

        when(context.validationService()).thenReturn(validationService);
        when(context.courseRepository()).thenReturn(courseRepository);
        when(context.assignmentRepository()).thenReturn(assignmentRepository);
        when(context.submissionRepository()).thenReturn(submissionRepository);
        when(context.compilerService()).thenReturn(compilerService);

        when(courseRepository.get(anyString())).thenReturn(course);
        when(course.getAssignmentIdByName(anyString())).thenReturn("Asgn_id");
        when(assignmentRepository.get(anyString())).thenReturn(assignment);

        when(assignment.userToSubmissionId()).thenReturn(Map.of("s1", "sub1", "s2", "sub2"));
        when(submissionRepository.get(anyString())).thenReturn(submission);
        when(submission.path()).thenReturn("/path");
        when(compilerService.compileFiles(anyString())).thenReturn(compilationResult);

        ServerResponse response = command.execute();

        assertEquals(Status.OK, response.status(),
                "Status should be OK when bulk testing submissions for a valid assignment");

        // will be much better with dto, but next time maybe
        assertInstanceOf(Map.class, response.data(),
                "Successful bulk test execution must return a Map of results in the data field");

        Map<?, ?> results = (Map<?, ?>) response.data();
        assertEquals(2, results.size(),
                "The results map should contain entries for all students in the assignment");

        verify(compilerService, times(2)).compileFiles(anyString());
    }

    @Test
    void testExecuteUnauthorized() {
        prepareAuth();
        when(mockAdmin.getAdminCourseIdFromName(anyString())).thenReturn(null);
        when(context.validationService()).thenReturn(validationService);
        when(context.courseRepository()).thenReturn(courseRepository);

        ServerResponse response = command.execute();

        assertEquals(Status.ERROR, response.status(),
                "Should return ERROR status if the user does not have administrative rights for the course");
        verify(logger).logError(anyString(), any(Exception.class));
    }

    @Test
    void testExecuteValidationFailure() {
        when(session.key()).thenReturn(channel);
        when(sessionStore.hasSession(channel)).thenReturn(true);
        when(sessionStore.getUser(channel)).thenReturn(mockAdmin);

        when(context.validationService()).thenReturn(validationService);
        RuntimeException ex = new RuntimeException("Validation error");
        doThrow(ex).when(validationService).validateListParamsCount(any(), any(Integer.class));

        ServerResponse response = command.execute();

        assertEquals(Status.ERROR, response.status(),
                "Should return ERROR status if the input parameters count is incorrect");
        verify(logger).logError(anyString(), any(Exception.class));
    }

    @Test
    void testGetType() {
        assertEquals(CommandType.TEST_SUBMISSIONS, command.getType(),
                "Command type should match TEST_SUBMISSIONS");
    }
}
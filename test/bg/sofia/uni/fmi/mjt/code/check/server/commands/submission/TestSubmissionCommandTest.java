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
import java.nio.file.Path;
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
class TestSubmissionCommandTest {

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

    private TestSubmissionCommand command;
    private final List<String> args = List.of("Java", "Lab1", "student1");

    @BeforeEach
    void setUp() {
        command = new TestSubmissionCommand(args);
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
        when(context.compilerService()).thenReturn(compilerService);

        when(courseRepository.get("Java_id")).thenReturn(course);
        when(course.getAssignmentIdByName("Lab1")).thenReturn("Asgn_id");
        when(assignmentRepository.get("Asgn_id")).thenReturn(assignment);
        when(assignment.userToSubmissionId()).thenReturn(Map.of("student1", "Sub_id"));

        when(submissionRepository.get("Sub_id")).thenReturn(submission);
        when(submission.path()).thenReturn(".");

        when(compilerService.compileFiles(anyString())).thenReturn(compilationResult);

        ServerResponse response = command.execute();

        assertEquals(Status.OK, response.status(),
                "Status should be OK when a submission is successfully tested by an admin");

        assertInstanceOf(CompilationResult.class, response.data(),
                "Successful test execution must return a CompilationResult in the data field");

        verify(compilerService).compileFiles(anyString());
        verify(validationService).throwIfDirectoryDoesNotExist(any(Path.class));
    }

    @Test
    void testExecuteUnauthorized() {
        prepareAuth();
        when(mockAdmin.getAdminCourseIdFromName("Java")).thenReturn(null);
        when(context.validationService()).thenReturn(validationService);
        when(context.courseRepository()).thenReturn(courseRepository);

        ServerResponse response = command.execute();

        assertEquals(Status.ERROR, response.status(),
                "Should return ERROR status if the user is not an admin of the course");
        verify(logger).logError(anyString(), any(Exception.class));
    }

    @Test
    void testExecuteValidationCrash() {
        when(session.key()).thenReturn(channel);
        when(sessionStore.hasSession(channel)).thenReturn(true);
        when(sessionStore.getUser(channel)).thenReturn(mockAdmin);

        when(context.validationService()).thenReturn(validationService);
        RuntimeException ex = new RuntimeException("Crash");
        doThrow(ex).when(validationService).validateListParamsCount(args, 3);

        ServerResponse response = command.execute();

        assertEquals(Status.ERROR, response.status(),
                "Should return ERROR status if validation parameters count fails");
        verify(logger).logError(anyString(), any(Exception.class));
    }

    @Test
    void testGetType() {
        assertEquals(CommandType.TEST_SUBMISSION, command.getType(),
                "Command type must return TEST_SUBMISSION");
    }
}
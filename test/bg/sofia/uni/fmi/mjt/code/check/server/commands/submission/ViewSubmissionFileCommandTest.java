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
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
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
class ViewSubmissionFileCommandTest {

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

    @TempDir
    Path tempDir;

    private ViewSubmissionFileCommand command;
    private final List<String> args = List.of("Java", "Lab1", "student1", "Main.java");

    @BeforeEach
    void setUp() {
        command = new ViewSubmissionFileCommand(args);
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
    void testExecuteSuccess() throws IOException {
        prepareAuth();
        Path testFile = tempDir.resolve("Main.java");
        String content = "public class Main {}";
        Files.writeString(testFile, content);

        when(mockAdmin.getAdminCourseIdFromName("Java")).thenReturn("Java_id");
        when(context.validationService()).thenReturn(validationService);
        when(context.courseRepository()).thenReturn(courseRepository);
        when(context.assignmentRepository()).thenReturn(assignmentRepository);
        when(context.submissionRepository()).thenReturn(submissionRepository);

        when(courseRepository.get("Java_id")).thenReturn(course);
        when(course.getAssignmentIdByName("Lab1")).thenReturn("asgn_id");
        when(assignmentRepository.get("asgn_id")).thenReturn(assignment);
        when(assignment.userToSubmissionId()).thenReturn(Map.of("student1", "sub_id"));
        when(submissionRepository.get("sub_id")).thenReturn(submission);
        when(submission.path()).thenReturn(tempDir.toString());

        ServerResponse response = command.execute();

        assertEquals(Status.OK, response.status(),
                "Status should be OK when an admin successfully requests a valid file from a submission");

        assertInstanceOf(String.class, response.data(),
                "The response data should be a non-null String containing the file content");

        assertEquals(content, response.data(),
                "The returned data must exactly match the content of the file on disk");
    }

    @Test
    void testExecuteCourseNotFound() {
        prepareAuth();
        when(context.validationService()).thenReturn(validationService);
        when(context.courseRepository()).thenReturn(courseRepository);
        when(mockAdmin.getAdminCourseIdFromName("Java")).thenReturn(null);
        when(courseRepository.get(null)).thenReturn(null);

        doThrow(new RuntimeException("Fail")).when(validationService).throwEntityDoesNotExistIfNull(any(), anyString());

        ServerResponse response = command.execute();

        assertEquals(Status.ERROR, response.status(),
                "Should return ERROR status when the requested course cannot be found for the admin");

        verify(validationService).throwIfUnsafe(anyString(), anyString());
        verify(logger).logError(anyString(), any(Exception.class));
    }

    @Test
    void testExecuteFileNotFound() {
        prepareAuth();
        when(mockAdmin.getAdminCourseIdFromName("Java")).thenReturn("Java_id");
        when(context.validationService()).thenReturn(validationService);
        when(context.courseRepository()).thenReturn(courseRepository);
        when(context.assignmentRepository()).thenReturn(assignmentRepository);
        when(context.submissionRepository()).thenReturn(submissionRepository);

        when(courseRepository.get("Java_id")).thenReturn(course);
        when(course.getAssignmentIdByName("Lab1")).thenReturn("asgn_id");
        when(assignmentRepository.get("asgn_id")).thenReturn(assignment);
        when(assignment.userToSubmissionId()).thenReturn(Map.of("student1", "sub_id"));
        when(submissionRepository.get("sub_id")).thenReturn(submission);
        when(submission.path()).thenReturn(tempDir.toString());

        ServerResponse response = command.execute();

        assertEquals(Status.ERROR, response.status(),
                "Should return ERROR status if the specific file does not exist within the submission directory");
        verify(logger).logError(anyString(), any(Exception.class));
    }

    @Test
    void testGetType() {
        assertEquals(CommandType.VIEW_SUBMISSION_FILE, command.getType(),
                "Command type must correctly return VIEW_SUBMISSION_FILE");
    }
}
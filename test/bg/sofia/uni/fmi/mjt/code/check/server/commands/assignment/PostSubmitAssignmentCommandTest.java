package bg.sofia.uni.fmi.mjt.code.check.server.commands.assignment;

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
import bg.sofia.uni.fmi.mjt.code.check.server.services.UploadsService;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostSubmitAssignmentCommandTest {

    @Mock private SessionStore sessionStore;
    @Mock private CodeCheckContext context;
    @Mock private ValidationService validationService;
    @Mock private CourseRepository courseRepository;
    @Mock private AssignmentRepository assignmentRepository;
    @Mock private SubmissionRepository submissionRepository;
    @Mock private UploadsService uploadsService;
    @Mock private Logger logger;
    @Mock private Session session;
    @Mock private SocketChannel channel;
    @Mock private Course course;
    @Mock private Assignment assignment;
    @Mock private Path path;
    @Mock private User user;

    private PostSubmitAssignmentCommand command;
    private final List<String> args = List.of("Java", "Lab01", "Success", "Message");

    @BeforeEach
    void setUp() {
        command = new PostSubmitAssignmentCommand(args);
        command.addDependencies(sessionStore, context);
        command.addSessionContext(session);
        command.addLogger(logger);
    }

    private void prepareAuth() {
        when(session.key()).thenReturn(channel);
        when(sessionStore.hasSession(channel)).thenReturn(true);
        when(sessionStore.getUser(channel)).thenReturn(user);
    }

    private void prepareContext() {
        when(context.validationService()).thenReturn(validationService);
        when(context.uploadsService()).thenReturn(uploadsService);
        when(uploadsService.uploadsRoot()).thenReturn(path);
        when(path.resolve(anyString())).thenReturn(path);
    }

    @Test
    void testExecuteSuccess() {
        prepareAuth();
        prepareContext();
        when(user.username()).thenReturn("tester");
        when(user.getEnrollCourseIdFromName("Java")).thenReturn("Java_ID");

        when(context.courseRepository()).thenReturn(courseRepository);
        when(context.assignmentRepository()).thenReturn(assignmentRepository);
        when(context.submissionRepository()).thenReturn(submissionRepository);

        when(courseRepository.get("Java_ID")).thenReturn(course);
        when(course.getAssignmentIdByName("Lab01")).thenReturn("Assignment_ID");
        when(assignmentRepository.get("Assignment_ID")).thenReturn(assignment);
        when(assignment.id()).thenReturn("Assignment_ID");
        when(path.toString()).thenReturn("/uploads/tester");

        ServerResponse response = command.execute();

        assertEquals(Status.OK, response.status(),
                "Response status should be OK on successful submission post-processing");
        assertInstanceOf(Submission.class, response.data(),
                "Response should contain proper data");
        verify(submissionRepository).add(any(Submission.class));
        verify(assignmentRepository).addSubmission(any(), anyString(), anyString());
    }

    @Test
    void testExecuteNoMessageArgs() {
        command = new PostSubmitAssignmentCommand(List.of("Java", "Lab01"));
        command.addDependencies(sessionStore, context);
        command.addSessionContext(session);
        command.addLogger(logger);

        prepareAuth();
        prepareContext();
        when(user.username()).thenReturn("tester");
        when(user.getEnrollCourseIdFromName("Java")).thenReturn("Java_ID");

        when(context.courseRepository()).thenReturn(courseRepository);
        when(courseRepository.get("Java_ID")).thenReturn(course);
        when(course.getAssignmentIdByName("Lab01")).thenReturn("Assignment_ID");

        when(context.assignmentRepository()).thenReturn(assignmentRepository);
        when(assignmentRepository.get("Assignment_ID")).thenReturn(assignment);
        when(assignment.id()).thenReturn("Assignment_ID");
        when(context.submissionRepository()).thenReturn(submissionRepository);
        when(path.toString()).thenReturn("/uploads/tester");

        ServerResponse response = command.execute();

        assertEquals(Status.OK, response.status(),
                "Response status should be OK even if success message arguments are missing");
    }

    @Test
    void testExecuteHandlesExceptionInSubclassLogic() {
        prepareAuth();
        when(context.validationService()).thenReturn(validationService);
        when(context.uploadsService()).thenThrow(new RuntimeException("Logic Fail"));

        ServerResponse response = command.execute();

        assertEquals(Status.ERROR, response.status(),
                "Response status should be ERROR when an internal runtime exception occurs");
        verify(logger).logError(anyString(), any(Exception.class));
    }

    @Test
    void testExecuteEntityNotFoundCourse() {
        prepareAuth();
        prepareContext();
        when(user.getEnrollCourseIdFromName("Java")).thenReturn("Java_ID");
        when(context.courseRepository()).thenReturn(courseRepository);
        when(courseRepository.get("Java_ID")).thenReturn(null);

        doThrow(new RuntimeException("Course not found"))
                .when(validationService).throwEntityDoesNotExistIfNull(any(), anyString());

        ServerResponse response = command.execute();

        assertEquals(Status.ERROR, response.status(),
                "Response status should be ERROR when the specified course does not exist");
    }

    @Test
    void testGetType() {
        assertEquals(CommandType.POST_SUBMIT_ASSIGNMENT, command.getType(),
                "Command type should match POST_SUBMIT_ASSIGNMENT");
    }
}
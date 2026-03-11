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
class ViewMySubmissionCommandTest {

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
    @Mock private User mockUser;

    private ViewMySubmissionCommand command;
    private final List<String> args = List.of("IntroJava", "Lab1");

    @BeforeEach
    void setUp() {
        command = new ViewMySubmissionCommand(args);
        command.addDependencies(sessionStore, context);
        command.addSessionContext(session);
        command.addLogger(logger);
    }

    private void prepareAuth() {
        when(session.key()).thenReturn(channel);
        when(sessionStore.hasSession(channel)).thenReturn(true);
        when(sessionStore.getUser(channel)).thenReturn(mockUser);
        when(mockUser.username()).thenReturn("studentUser");
    }

    @Test
    void testExecuteSuccess() {
        prepareAuth();
        when(mockUser.getEnrollCourseIdFromName("IntroJava")).thenReturn("course_id");

        when(context.validationService()).thenReturn(validationService);
        when(context.courseRepository()).thenReturn(courseRepository);
        when(context.assignmentRepository()).thenReturn(assignmentRepository);
        when(context.submissionRepository()).thenReturn(submissionRepository);

        when(courseRepository.get("course_id")).thenReturn(course);
        when(course.getAssignmentIdByName("Lab1")).thenReturn("asgn_id");
        when(assignmentRepository.get("asgn_id")).thenReturn(assignment);
        when(assignment.userToSubmissionId()).thenReturn(Map.of("studentUser", "sub_id"));
        when(submissionRepository.get("sub_id")).thenReturn(submission);
        when(submission.path()).thenReturn(null);

        ServerResponse response = command.execute();

        assertEquals(Status.OK, response.status(),
                "The response status should be OK when viewing a valid submission owned by the user");

        assertInstanceOf(SubmissionView.class, response.data(),
                "The response data must be a non-null SubmissionView instance upon success");
    }

    @Test
    void testExecuteCourseNotEnrolled() {
        when(session.key()).thenReturn(channel);
        when(sessionStore.hasSession(channel)).thenReturn(true);
        when(sessionStore.getUser(channel)).thenReturn(mockUser);

        when(context.validationService()).thenReturn(validationService);
        when(context.courseRepository()).thenReturn(courseRepository);
        when(mockUser.getEnrollCourseIdFromName(anyString())).thenReturn(null);

        when(courseRepository.get(null)).thenReturn(null);

        RuntimeException notEnrolled = new RuntimeException("Course does not exist");
        doThrow(notEnrolled).when(validationService).throwEntityDoesNotExistIfNull(any(), anyString());

        ServerResponse response = command.execute();

        assertEquals(Status.ERROR, response.status(),
                "The response status should be ERROR if the student is not enrolled in the course");
        verify(logger).logError(anyString(), any(Exception.class));
        verify(validationService).throwIfUnsafe(anyString(), anyString());
    }

    @Test
    void testExecuteIoExceptionPath() {
        prepareAuth();
        when(mockUser.getEnrollCourseIdFromName("IntroJava")).thenReturn("course_id");

        when(context.validationService()).thenReturn(validationService);
        when(context.courseRepository()).thenReturn(courseRepository);
        when(context.assignmentRepository()).thenReturn(assignmentRepository);
        when(context.submissionRepository()).thenReturn(submissionRepository);

        when(courseRepository.get("course_id")).thenReturn(course);
        when(course.getAssignmentIdByName("Lab1")).thenReturn("asgn_id");
        when(assignmentRepository.get("asgn_id")).thenReturn(assignment);
        when(assignment.userToSubmissionId()).thenReturn(Map.of("studentUser", "sub_id"));
        when(submissionRepository.get("sub_id")).thenReturn(submission);

        when(submission.path()).thenReturn("invalid/path/that/does/not/exist/at/all");

        ServerResponse response = command.execute();

        assertEquals(Status.OK, response.status(),
                "The status should still be OK even if source files cannot be read (fallback to metadata only)");
        verify(logger).logError(anyString(), any(Exception.class));
    }

    @Test
    void testGetType() {
        assertEquals(CommandType.VIEW_MY_SUBMISSION, command.getType(),
                "The command type must return VIEW_MY_SUBMISSION");
    }
}
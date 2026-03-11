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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListAssignmentsCommandTest {

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

    private ListAssignmentsCommand command;
    private static final String COURSE_NAME = "Java";
    private static final String COURSE_ID = "Java_01";

    @BeforeEach
    void setUp() {
        command = new ListAssignmentsCommand(List.of(COURSE_NAME));
        command.addDependencies(sessionStore, context);
        command.addSessionContext(session);
        command.addLogger(logger);
    }

    private void prepareAuthenticatedSession() {
        when(session.key()).thenReturn(channel);
        when(sessionStore.hasSession(channel)).thenReturn(true);
        when(sessionStore.getUser(channel)).thenReturn(user);
        when(context.validationService()).thenReturn(validationService);
    }

    @Test
    void testExecuteReturnsErrorWhenNotPartOfCourse() {
        prepareAuthenticatedSession();
        when(user.getAdminCourseIdFromName(COURSE_NAME)).thenReturn(null);
        when(user.getEnrollCourseIdFromName(COURSE_NAME)).thenReturn(null);

        ServerResponse response = command.execute();

        assertEquals(Status.ERROR, response.status(),
                "Should return ERROR when user is neither admin nor enrolled in the course");
    }

    @Test
    void testExecuteAdminViewSuccess() {
        prepareAuthenticatedSession();
        when(user.getAdminCourseIdFromName(COURSE_NAME)).thenReturn(COURSE_ID);
        when(context.courseRepository()).thenReturn(courseRepository);
        when(context.assignmentRepository()).thenReturn(assignmentRepository);
        when(courseRepository.get(COURSE_ID)).thenReturn(course);
        when(course.assignmentIds()).thenReturn(Set.of("Task1"));
        when(assignmentRepository.get("Task1")).thenReturn(assignment);
        when(assignment.userToSubmissionId()).thenReturn(Map.of("u1", "s1"));

        ServerResponse response = command.execute();

        assertEquals(Status.OK, response.status(),
                "Admin view should return OK status");
        assertNotNull(response.data(),
                "Admin view response should contain assignment data");
    }

    @Test
    void testExecuteStudentViewWithSubmissionSuccess() {
        prepareAuthenticatedSession();
        when(user.getAdminCourseIdFromName(COURSE_NAME)).thenReturn(null);
        when(user.getEnrollCourseIdFromName(COURSE_NAME)).thenReturn(COURSE_ID);
        when(user.username()).thenReturn("student");

        when(context.courseRepository()).thenReturn(courseRepository);
        when(context.assignmentRepository()).thenReturn(assignmentRepository);
        when(context.submissionRepository()).thenReturn(submissionRepository);

        when(courseRepository.get(COURSE_ID)).thenReturn(course);
        when(course.assignmentIds()).thenReturn(Set.of("Task1"));
        when(assignmentRepository.get("Task1")).thenReturn(assignment);
        when(assignment.userToSubmissionId()).thenReturn(Map.of("student", "sub123"));
        when(submissionRepository.get("sub123")).thenReturn(submission);

        ServerResponse response = command.execute();

        assertEquals(Status.OK, response.status(),
                "Student view should return OK status");
        assertNotNull(response.data(),
                "Student view response should contain assignment and submission data");
    }

    @Test
    void testExecuteAdminHandlesMissingAssignmentInconsistency() {
        prepareAuthenticatedSession();
        when(user.getAdminCourseIdFromName(COURSE_NAME)).thenReturn(COURSE_ID);

        when(context.courseRepository()).thenReturn(courseRepository);
        when(context.assignmentRepository()).thenReturn(assignmentRepository);

        when(courseRepository.get(COURSE_ID)).thenReturn(course);
        when(course.id()).thenReturn(COURSE_ID);
        when(course.assignmentIds()).thenReturn(Set.of("DanglingID"));
        when(assignmentRepository.get("DanglingID")).thenReturn(null);

        command.execute();

        verify(courseRepository).removeAssignment(course, "DanglingID");
        verify(logger).logWarning(anyString());
    }

    @Test
    void testExecuteThrowsExceptionWhenCourseRepoReturnsNull() {
        prepareAuthenticatedSession();
        when(user.getAdminCourseIdFromName(COURSE_NAME)).thenReturn(COURSE_ID);
        when(context.courseRepository()).thenReturn(courseRepository);
        when(courseRepository.get(COURSE_ID)).thenReturn(null);

        ServerResponse response = command.execute();

        assertEquals(Status.ERROR, response.status(),
                "Should return ERROR status if the course repository cannot find the course by ID");
    }

    @Test
    void testExecuteHandlesUnexpectedExceptionAndLogsIt() {
        prepareAuthenticatedSession();
        when(user.getAdminCourseIdFromName(anyString())).thenThrow(new RuntimeException("Crash"));

        ServerResponse response = command.execute();

        assertEquals(Status.ERROR, response.status(),
                "Should return ERROR status when an unhandled exception occurs");
        verify(logger).logError(anyString(), any(Exception.class));
    }

    @Test
    void testGetType() {
        assertEquals(CommandType.LIST_ASSIGNMENTS, command.getType(),
                "Command type should be LIST_ASSIGNMENTS");
    }
}
package bg.sofia.uni.fmi.mjt.code.check.server.commands.assignment;

import bg.sofia.uni.fmi.mjt.code.check.contract.ServerResponse;
import bg.sofia.uni.fmi.mjt.code.check.contract.Status;
import bg.sofia.uni.fmi.mjt.code.check.server.commands.CommandType;
import bg.sofia.uni.fmi.mjt.code.check.server.entities.Assignment;
import bg.sofia.uni.fmi.mjt.code.check.server.entities.Course;
import bg.sofia.uni.fmi.mjt.code.check.server.entities.User;
import bg.sofia.uni.fmi.mjt.code.check.server.repositories.AssignmentRepository;
import bg.sofia.uni.fmi.mjt.code.check.server.repositories.CodeCheckContext;
import bg.sofia.uni.fmi.mjt.code.check.server.repositories.CourseRepository;
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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateAssignmentCommandTest {

    @Mock private SessionStore sessionStore;
    @Mock private CodeCheckContext context;
    @Mock private ValidationService validationService;
    @Mock private CourseRepository courseRepository;
    @Mock private AssignmentRepository assignmentRepository;
    @Mock private Logger logger;
    @Mock private Session session;
    @Mock private SocketChannel channel;
    @Mock private Course course;
    @Mock private User user;

    private CreateAssignmentCommand command;
    private final List<String> validArgs = List.of("Java", "Homework1", "Desc", getFutureDateString(7));

    @BeforeEach
    void setUp() {
        command = new CreateAssignmentCommand(validArgs);
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
    void testExecuteReturnsErrorWhenCourseDoesNotExist() {
        prepareAuthenticatedSession();
        when(user.getAdminCourseIdFromName("Java")).thenReturn("Java_123");
        when(context.courseRepository()).thenReturn(courseRepository);
        when(courseRepository.get("Java_123")).thenReturn(null);

        doThrow(new RuntimeException("Course does not exist"))
                .when(validationService)
                .throwEntityDoesNotExistIfNull(any(), anyString());

        ServerResponse response = command.execute();

        assertEquals(Status.ERROR, response.status(),
                "Response status should be ERROR when the course does not exist");
    }

    @Test
    void testExecuteReturnsErrorWhenAssignmentNameExists() {
        prepareAuthenticatedSession();
        when(user.getAdminCourseIdFromName("Java")).thenReturn("Java_123");
        when(context.courseRepository()).thenReturn(courseRepository);
        when(courseRepository.get("Java_123")).thenReturn(course);
        when(course.assignmentNames()).thenReturn(Set.of("Homework1"));

        doThrow(new RuntimeException("Assignment exists"))
                .when(validationService)
                .throwEntityAlreadyExistsIfContained(any(), anyString(), anyString());

        ServerResponse response = command.execute();

        assertEquals(Status.ERROR, response.status(),
                "Response status should be ERROR when an assignment with the same name already exists");
    }

    @Test
    void testExecuteSuccessfulCreation() {
        prepareAuthenticatedSession();
        when(user.getAdminCourseIdFromName("Java")).thenReturn("Java_123");
        when(context.courseRepository()).thenReturn(courseRepository);
        when(context.assignmentRepository()).thenReturn(assignmentRepository);
        when(courseRepository.get("Java_123")).thenReturn(course);
        when(course.id()).thenReturn("Java_123");
        when(course.name()).thenReturn("Java");
        when(course.assignmentNames()).thenReturn(Set.of());

        ServerResponse response = command.execute();

        assertEquals(Status.OK, response.status(),
                "Response status should be OK when creating a valid assignment");
        assertInstanceOf(Assignment.class, response.data(),
                "Response should contain proper data");
        verify(validationService).validateListParamsCount(validArgs, 4);
        verify(assignmentRepository).add(any(Assignment.class));
        verify(courseRepository).addAssignment(any(Course.class), anyString());
    }

    @Test
    void testExecuteReturnsErrorForInvalidDateFormat() {
        command = new CreateAssignmentCommand(List.of("Java", "H1", "D", "invalid"));
        command.addDependencies(sessionStore, context);
        command.addSessionContext(session);
        command.addLogger(logger);

        prepareAuthenticatedSession();
        when(user.getAdminCourseIdFromName("Java")).thenReturn("Java_123");
        when(context.courseRepository()).thenReturn(courseRepository);
        when(courseRepository.get("Java_123")).thenReturn(course);
        when(course.assignmentNames()).thenReturn(Set.of());

        ServerResponse response = command.execute();

        assertEquals(Status.ERROR, response.status(),
                "Response status should be ERROR when the date format is invalid");
        verify(logger).logError(anyString(), any(Exception.class));
    }

    @Test
    void testExecuteLogsErrorOnUnexpectedException() {
        prepareAuthenticatedSession();
        when(user.getAdminCourseIdFromName(anyString()))
                .thenThrow(new RuntimeException("Crash inside CreateAssignmentCommand"));

        ServerResponse response = command.execute();

        assertEquals(Status.ERROR, response.status(),
                "Response status should be ERROR when an unexpected exception occurs");
        verify(logger).logError(anyString(), any(Exception.class));
    }

    @Test
    void testExecuteReturnsErrorWhenDeadlineIsInThePast() {
        String pastDate = getFutureDateString(-1);
        command = new CreateAssignmentCommand(List.of("Java", "Homework1", "Desc", pastDate));
        command.addDependencies(sessionStore, context);
        command.addSessionContext(session);
        command.addLogger(logger);

        prepareAuthenticatedSession();
        when(user.getAdminCourseIdFromName("Java")).thenReturn("Java_123");
        when(context.courseRepository()).thenReturn(courseRepository);
        when(courseRepository.get("Java_123")).thenReturn(course);
        when(course.assignmentNames()).thenReturn(Set.of());

        ServerResponse response = command.execute();

        assertEquals(Status.ERROR, response.status(),
                "Response status should be ERROR when the provided deadline is in the past");
    }

    @Test
    void testExecuteSuccessfulWithNextWeekDeadline() {
        String nextWeekDate = getFutureDateString(7);
        command = new CreateAssignmentCommand(List.of("Java", "Homework1", "Desc", nextWeekDate));
        command.addDependencies(sessionStore, context);
        command.addSessionContext(session);
        command.addLogger(logger);

        prepareAuthenticatedSession();
        when(user.getAdminCourseIdFromName("Java")).thenReturn("Java_123");
        when(context.courseRepository()).thenReturn(courseRepository);
        when(context.assignmentRepository()).thenReturn(assignmentRepository);
        when(courseRepository.get("Java_123")).thenReturn(course);
        when(course.id()).thenReturn("Java_123");
        when(course.name()).thenReturn("Java");
        when(course.assignmentNames()).thenReturn(Set.of());

        ServerResponse response = command.execute();

        assertEquals(Status.OK, response.status(),
                "Response status should be OK when creating an assignment with a valid future deadline");
        verify(assignmentRepository).add(any(Assignment.class));
    }

    @Test
    void testGetType() {
        assertEquals(CommandType.CREATE_ASSIGNMENT, command.getType(),
                "Command type should match CREATE_ASSIGNMENT");
    }

    private String getFutureDateString(int daysToAdd) {
        return LocalDate.now().plusDays(daysToAdd).format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
    }
}
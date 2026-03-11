package bg.sofia.uni.fmi.mjt.code.check.server.commands.assignment;

import bg.sofia.uni.fmi.mjt.code.check.contract.ServerResponse;
import bg.sofia.uni.fmi.mjt.code.check.contract.Status;
import bg.sofia.uni.fmi.mjt.code.check.server.commands.CommandType;
import bg.sofia.uni.fmi.mjt.code.check.server.entities.Assignment;
import bg.sofia.uni.fmi.mjt.code.check.server.entities.Course;
import bg.sofia.uni.fmi.mjt.code.check.server.entities.User;
import bg.sofia.uni.fmi.mjt.code.check.server.exceptions.InvalidCommandException;
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
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubmitAssignmentCommandTest {

    @Mock private SessionStore sessionStore;
    @Mock private CodeCheckContext context;
    @Mock private ValidationService validationService;
    @Mock private CourseRepository courseRepository;
    @Mock private AssignmentRepository assignmentRepository;
    @Mock private Logger logger;
    @Mock private Session session;
    @Mock private SocketChannel channel;
    @Mock private Course course;
    @Mock private Assignment assignment;
    @Mock private User user;

    private SubmitAssignmentCommand command;
    private final List<String> args = List.of("Java", "Lab01", "file.zip");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy-HH-mm-ss");

    @BeforeEach
    void setUp() {
        command = new SubmitAssignmentCommand(args);
        command.addDependencies(sessionStore, context);
        command.addSessionContext(session);
        command.addLogger(logger);
    }

    private void prepareAuth() {
        when(session.key()).thenReturn(channel);
        when(sessionStore.hasSession(channel)).thenReturn(true);
        when(sessionStore.getUser(channel)).thenReturn(user);
        when(context.validationService()).thenReturn(validationService);
    }

    @Test
    void testExecuteSuccess() {
        prepareAuth();
        when(user.getEnrollCourseIdFromName("Java")).thenReturn("Java_01");
        String futureDeadline = LocalDateTime.now().plusDays(1).format(FORMATTER);

        when(context.courseRepository()).thenReturn(courseRepository);
        when(context.assignmentRepository()).thenReturn(assignmentRepository);

        when(courseRepository.get("Java_01")).thenReturn(course);
        when(course.assignmentNames()).thenReturn(Set.of("Lab01"));
        when(course.getAssignmentIdByName("Lab01")).thenReturn("LAB_ID");
        when(assignmentRepository.get("LAB_ID")).thenReturn(assignment);
        when(assignment.deadline()).thenReturn(futureDeadline);

        ServerResponse response = command.execute();

        assertEquals(Status.OK, response.status(),
                "Should return OK status for valid submission within deadline");
        assertInstanceOf(Assignment.class, response.data(),
                "Response should contain proper data");
        verify(validationService).validateListParamsCount(args, 3);
    }

    @Test
    void testExecuteValidationFailsDeadline() {
        prepareAuth();
        when(user.getEnrollCourseIdFromName("Java")).thenReturn("Java_01");
        String pastDeadline = LocalDateTime.now().minusDays(1).format(FORMATTER);

        when(context.courseRepository()).thenReturn(courseRepository);
        when(context.assignmentRepository()).thenReturn(assignmentRepository);

        when(courseRepository.get("Java_01")).thenReturn(course);
        when(course.assignmentNames()).thenReturn(Set.of("Lab01"));
        when(course.getAssignmentIdByName("Lab01")).thenReturn("LAB_ID");
        when(assignmentRepository.get("LAB_ID")).thenReturn(assignment);
        when(assignment.deadline()).thenReturn(pastDeadline);

        doThrow(new RuntimeException("Deadline passed")).when(validationService)
                .throwUnauthorizedIfBefore(any(), any());

        ServerResponse response = command.execute();

        assertEquals(Status.ERROR, response.status(),
                "Should return ERROR status if the submission deadline has passed");
        verify(logger).logError(anyString(), any(Exception.class));
    }

    @Test
    void testExecuteCourseDoesNotExist() {
        prepareAuth();
        when(user.getEnrollCourseIdFromName("Java")).thenReturn("Java_01");
        when(context.courseRepository()).thenReturn(courseRepository);
        when(courseRepository.get("Java_01")).thenReturn(null);

        doThrow(new RuntimeException("Course not found")).when(validationService)
                .throwEntityDoesNotExistIfNull(any(), anyString());

        ServerResponse response = command.execute();

        assertEquals(Status.ERROR, response.status(),
                "Should return ERROR status if the student is not enrolled in an existing course");
        verify(logger).logError(anyString(), any(Exception.class));
    }

    @Test
    void testGetDestinationFolderSuccess() {
        Path path = SubmitAssignmentCommand.getDestinationFolder("user1", "submit Java Lab01 file.zip", "archivezip");

        assertNotNull(path, "Destination path should not be null");
        assertEquals("Java", path.getName(0).toString(), "First path component should be the course name");
        assertEquals("Lab01", path.getName(1).toString(), "Second path component should be the assignment name");
        assertEquals("user1", path.getName(2).toString(), "Third path component should be the username");
        assertEquals("archivezip", path.getName(3).toString(), "Fourth path component should be the timestamped archive name");
    }

    @Test
    void testGetDestinationFolderRejectsDot() {
        assertThrows(InvalidCommandException.class, () ->
                        SubmitAssignmentCommand.getDestinationFolder("user1", "Java.1 Lab01 dummy", "archivezip"),
                "Should throw InvalidCommandException if course name contains a dot to prevent path traversal");
    }

    @Test
    void testGetDestinationFolderRejectsUnderscore() {
        assertThrows(InvalidCommandException.class, () ->
                        SubmitAssignmentCommand.getDestinationFolder("user_1", "Java Lab01 dummy", "archivezip"),
                "Should throw InvalidCommandException if username contains an underscore (internal delimiter)");
    }

    @Test
    void testGetDestinationFolderRejectsForwardSlash() {
        assertThrows(InvalidCommandException.class, () ->
                        SubmitAssignmentCommand.getDestinationFolder("user1", "Java/Course Lab01 dummy", "archivezip"),
                "Should throw InvalidCommandException if any component contains a forward slash");
    }

    @Test
    void testGetDestinationFolderRejectsBackslash() {
        assertThrows(InvalidCommandException.class, () ->
                        SubmitAssignmentCommand.getDestinationFolder("user1", "Java\\Course Lab01 dummy", "archivezip"),
                "Should throw InvalidCommandException if any component contains a backslash");
    }

    @Test
    void testGetDestinationFolderRejectsEmptyCourse() {
        assertThrows(InvalidCommandException.class, () ->
                        SubmitAssignmentCommand.getDestinationFolder("user1", "  Lab01 dummy", "archivezip"),
                "Should throw InvalidCommandException if course name is blank");
    }

    @Test
    void testGetDestinationFolderThrowsOnInvalidInput() {
        assertThrows(RuntimeException.class, () ->
                        SubmitAssignmentCommand.getDestinationFolder(null, null, null),
                "Should throw a RuntimeException or NullPointerException for null inputs");
    }

    @Test
    void testGetType() {
        assertEquals(CommandType.SUBMIT_ASSIGNMENT, command.getType(),
                "Command type must be SUBMIT_ASSIGNMENT");
    }
}
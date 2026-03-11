package bg.sofia.uni.fmi.mjt.code.check.server.commands.assignment;

import bg.sofia.uni.fmi.mjt.code.check.server.commands.AuthenticatedCommand;
import bg.sofia.uni.fmi.mjt.code.check.server.commands.CommandParser;
import bg.sofia.uni.fmi.mjt.code.check.server.commands.CommandType;
import bg.sofia.uni.fmi.mjt.code.check.contract.ServerResponse;

import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;

import bg.sofia.uni.fmi.mjt.code.check.server.entities.Assignment;
import bg.sofia.uni.fmi.mjt.code.check.server.entities.Course;
import bg.sofia.uni.fmi.mjt.code.check.server.exceptions.InvalidCommandException;
import bg.sofia.uni.fmi.mjt.code.check.contract.Status;

import java.time.LocalDateTime;

/**
 * A bit of a special command, only handles validation and pre-upload
 * Actual file upload works with FileStreams and is managed by other services
 * */
public class SubmitAssignmentCommand extends AuthenticatedCommand {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy-HH-mm-ss");

    private static final int EXPECTED_PARAMS_COUNT = 3;
    private static final int COURSE_NAME_INDEX = 0;
    private static final int ASSIGNMENT_NAME_INDEX = 1;

    private final List<String> args;

    public SubmitAssignmentCommand(List<String> args) {
        this.args = args;
    }

    @Override
    protected ServerResponse authenticatedExecute() {
        try {
            context.validationService().validateListParamsCount(args, EXPECTED_PARAMS_COUNT);
            String courseName = args.get(COURSE_NAME_INDEX);
            String assignmentName = args.get(ASSIGNMENT_NAME_INDEX);

            Assignment assignment = validateAndGetAssignment(courseName, assignmentName);
            validateDeadline(assignment);

            return new ServerResponse(CommandType.SUBMIT_ASSIGNMENT, Status.OK,
                    "Assignment created successfully", user, assignment);
        } catch (Exception e) {
            logger.logError("Exception occurred in submit assignment command", e);
            return new ServerResponse(CommandType.SUBMIT_ASSIGNMENT, Status.ERROR, e.getMessage(), user, null);
        }
    }

    private Assignment validateAndGetAssignment(String courseName, String assignmentName) {
        String courseId = user.getEnrollCourseIdFromName(courseName);
        Course course = context.courseRepository().get(courseId);
        context.validationService().throwEntityDoesNotExistIfNull(course, "Course does not exist");

        context.validationService().throwEntityDoesNotExistsIfNotContained(
                course.assignmentNames(), assignmentName, "Assignment with this name does not exist");

        Assignment assignment = context.assignmentRepository().get(course.getAssignmentIdByName(assignmentName));
        context.validationService().throwEntityDoesNotExistIfNull(assignment, "Assignment does not exist");

        return assignment;
    }

    private void validateDeadline(Assignment assignment) {
        LocalDateTime deadline = LocalDateTime.parse(assignment.deadline(), FORMATTER);
        context.validationService().throwUnauthorizedIfBefore(deadline, LocalDateTime.now());
    }

    @Override
    public CommandType getType() {
        return CommandType.SUBMIT_ASSIGNMENT;
    }

    public static Path getDestinationFolder(String username, String input, String archiveName) {
        List<String> args = CommandParser.getCommandArgs(input);

        if (args.size() !=  EXPECTED_PARAMS_COUNT) {
            throw new InvalidCommandException("Insufficient arguments to determine destination");
        }

        String courseName = args.get(COURSE_NAME_INDEX);
        String assignmentName = args.get(ASSIGNMENT_NAME_INDEX);

        // Manual validation for static context
        validatePathComponent(courseName, "Course name");
        validatePathComponent(assignmentName, "Assignment name");
        validatePathComponent(username, "Username");
        validatePathComponent(archiveName, "Archive name");

        return Path.of(courseName, assignmentName, username, archiveName);
    }

    private static final List<String> FORBIDDEN_SYMBOLS = List.of("/", "\\", "..");
    private static void validatePathComponent(String part, String label) {
        if (part == null || part.isBlank()) {
            throw new IllegalArgumentException(label + " cannot be null or empty");
        }

        // Check each forbidden string against the path component
        for (String symbol : FORBIDDEN_SYMBOLS) {
            if (part.contains(symbol)) {
                throw new IllegalArgumentException(label + " contains illegal symbol: " + symbol);
            }
        }
    }
}
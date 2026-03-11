package bg.sofia.uni.fmi.mjt.code.check.server.commands.submission;

import bg.sofia.uni.fmi.mjt.code.check.contract.Status;
import bg.sofia.uni.fmi.mjt.code.check.server.commands.AuthenticatedCommand;
import bg.sofia.uni.fmi.mjt.code.check.server.commands.CommandType;
import bg.sofia.uni.fmi.mjt.code.check.contract.ServerResponse;
import bg.sofia.uni.fmi.mjt.code.check.server.entities.Assignment;
import bg.sofia.uni.fmi.mjt.code.check.server.entities.Course;
import bg.sofia.uni.fmi.mjt.code.check.server.entities.Submission;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class ViewSubmissionFileCommand extends AuthenticatedCommand {
    private static final int EXPECTED_PARAMS_COUNT = 4;
    private static final int COURSE_NAME_INDEX = 0;
    private static final int ASSIGNMENT_NAME_INDEX = 1;
    private static final int STUDENT_USERNAME_INDEX = 2;
    private static final int TARGET_FILE_INDEX = 3;

    private final List<String> args;

    public ViewSubmissionFileCommand(List<String> args) {
        this.args = args;
    }

    @Override
    protected ServerResponse authenticatedExecute() {
        try {
            context.validationService().validateListParamsCount(args, EXPECTED_PARAMS_COUNT);
            String courseName = args.get(COURSE_NAME_INDEX);
            String assignmentName = args.get(ASSIGNMENT_NAME_INDEX);
            String studentUsername = args.get(STUDENT_USERNAME_INDEX);
            String targetFileName = args.get(TARGET_FILE_INDEX);

            context.validationService().throwIfUnsafe(courseName,
                    "Course name cannot be null, empty or contain illegal symbols");

            String courseId = user.getAdminCourseIdFromName(courseName);
            Course course = context.courseRepository().get(courseId);
            context.validationService().throwEntityDoesNotExistIfNull(course, "Course does not exist");

            Assignment assignment = getValidAssignment(course, assignmentName);
            Submission submission = getValidSubmission(assignment, studentUsername);

            Path filePath = resolveFilePath(Path.of(submission.path()), targetFileName);
            String content = Files.readString(filePath);

            return new ServerResponse(getType(), Status.OK, "File loaded", user, content);
        } catch (Exception e) {
            logger.logError("Exception occurred in view submission file command", e);
            return new ServerResponse(getType(), Status.ERROR, e.getMessage(), user, null);
        }
    }

    private Assignment getValidAssignment(Course course, String assignmentName) {
        String assignmentId = course.getAssignmentIdByName(assignmentName);
        Assignment assignment = context.assignmentRepository().get(assignmentId);
        context.validationService().throwEntityDoesNotExistIfNull(assignment, "Assignment does not exist");
        return assignment;
    }

    private Submission getValidSubmission(Assignment assignment, String studentUsername) {
        String submissionId = assignment.userToSubmissionId().get(studentUsername);
        Submission submission = context.submissionRepository().get(submissionId);
        context.validationService().throwEntityDoesNotExistIfNull(submission,
                "Submission does not exist");
        context.validationService().throwEntityDoesNotExistIfNull(submission.path(),
                "Path does not exist");
        return submission;
    }

    private Path resolveFilePath(Path root, String target) throws IOException {
        Path targetPath = Path.of(target);
        Path directPath = root.resolve(targetPath);

        if (Files.exists(directPath) && Files.isRegularFile(directPath)) {
            return directPath;
        }

        try (var stream = Files.walk(root)) {
            return stream.filter(Files::isRegularFile)
                    .filter(p -> p.endsWith(targetPath))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("File not found: " + target));
        }
    }

    @Override
    public CommandType getType() {
        return CommandType.VIEW_SUBMISSION_FILE;
    }
}
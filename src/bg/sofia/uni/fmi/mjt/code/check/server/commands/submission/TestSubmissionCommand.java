package bg.sofia.uni.fmi.mjt.code.check.server.commands.submission;

import bg.sofia.uni.fmi.mjt.code.check.contract.Status;
import bg.sofia.uni.fmi.mjt.code.check.server.commands.AuthenticatedCommand;
import bg.sofia.uni.fmi.mjt.code.check.server.commands.CommandType;
import bg.sofia.uni.fmi.mjt.code.check.contract.ServerResponse;
import bg.sofia.uni.fmi.mjt.code.check.server.entities.Assignment;
import bg.sofia.uni.fmi.mjt.code.check.server.entities.Course;
import bg.sofia.uni.fmi.mjt.code.check.server.entities.Submission;
import bg.sofia.uni.fmi.mjt.code.check.server.models.CompilationResult;

import java.nio.file.Path;
import java.util.List;

public class TestSubmissionCommand extends AuthenticatedCommand {
    private static final int EXPECTED_PARAMS_COUNT = 3;
    private static final int COURSE_NAME_INDEX = 0;
    private static final int ASSIGNMENT_NAME_INDEX = 1;
    private static final int STUDENT_USERNAME_INDEX = 2;

    private final List<String> args;

    public TestSubmissionCommand(List<String> args) {
        this.args = args;
    }

    @Override
    protected ServerResponse authenticatedExecute() {
        try {
            context.validationService().validateListParamsCount(args, EXPECTED_PARAMS_COUNT);
            String courseName = args.get(COURSE_NAME_INDEX);
            String assignmentName = args.get(ASSIGNMENT_NAME_INDEX);
            String studentUsername = args.get(STUDENT_USERNAME_INDEX);

            context.validationService().throwIfUnsafe(courseName,
                    "Course name cannot be null, empty or contain illegal symbols");

            String courseId = user.getAdminCourseIdFromName(courseName);
            Course course = context.courseRepository().get(courseId);
            context.validationService().throwEntityDoesNotExistIfNull(course, "Course does not exist");

            Assignment assignment = getValidAssignment(course, assignmentName);
            Submission submission = getValidSubmission(assignment, studentUsername);

            CompilationResult res = context.compilerService().compileFiles(submission.path());

            return new ServerResponse(CommandType.TEST_SUBMISSION, Status.OK,
                    "Submission tested successfully", user, res);
        } catch (Exception e) {
            logger.logError("Exception occurred in test submission command", e);
            return new ServerResponse(CommandType.TEST_SUBMISSION, Status.ERROR, e.getMessage(), user, null);
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
        context.validationService().throwEntityDoesNotExistIfNull(submission, "Submission does not exist");
        context.validationService().throwEntityDoesNotExistIfNull(submission.path(), "Path does not exist");
        context.validationService().throwIfDirectoryDoesNotExist(Path.of(submission.path()));
        return submission;
    }

    @Override
    public CommandType getType() {
        return CommandType.TEST_SUBMISSION;
    }
}
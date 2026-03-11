package bg.sofia.uni.fmi.mjt.code.check.server.commands.submission;

import bg.sofia.uni.fmi.mjt.code.check.server.commands.AuthenticatedCommand;
import bg.sofia.uni.fmi.mjt.code.check.server.commands.CommandType;
import bg.sofia.uni.fmi.mjt.code.check.server.entities.Assignment;
import bg.sofia.uni.fmi.mjt.code.check.server.entities.Course;
import bg.sofia.uni.fmi.mjt.code.check.server.entities.Submission;
import bg.sofia.uni.fmi.mjt.code.check.contract.ServerResponse;
import bg.sofia.uni.fmi.mjt.code.check.contract.Status;
import bg.sofia.uni.fmi.mjt.code.check.server.services.LockRegistry;

import java.util.List;

public class CommentSubmissionFileCommand extends AuthenticatedCommand {
    private static final int EXPECTED_PARAMS_COUNT = 5;
    private static final int COURSE_NAME_INDEX = 0;
    private static final int ASSIGNMENT_NAME_INDEX = 1;
    private static final int STUDENT_USERNAME_INDEX = 2;
    private static final int FILE_NAME_INDEX = 3;
    private static final int COMMENT_INDEX = 4;

    private final List<String> args;

    public CommentSubmissionFileCommand(List<String> args) {
        this.args = args;
    }

    @Override
    protected ServerResponse authenticatedExecute() {
        try {
            context.validationService().validateListParamsCount(args, EXPECTED_PARAMS_COUNT);
            String courseName = args.get(COURSE_NAME_INDEX);
            String assignmentName = args.get(ASSIGNMENT_NAME_INDEX);
            String studentUsername = args.get(STUDENT_USERNAME_INDEX);
            String fileName = args.get(FILE_NAME_INDEX);
            String comment = args.get(COMMENT_INDEX);

            context.validationService().throwIfUnsafe(courseName,
                    "Course name cannot be null, empty or contain illegal symbols");

            String courseId = user.getAdminCourseIdFromName(courseName);
            Course course = context.courseRepository().get(courseId);
            context.validationService().throwEntityDoesNotExistIfNull(course, "Course not found.");

            Assignment assignment = getValidAssignment(course, assignmentName);
            Submission submission = getValidSubmission(assignment, studentUsername);

            saveComment(submission, fileName, comment);

            return new ServerResponse(CommandType.COMMENT_SUBMISSION_FILE, Status.OK,
                    "File comment added successfully", user, submission);
        } catch (Exception e) {
            logger.logError("Exception occurred in comment submission file command", e);
            return new ServerResponse(CommandType.COMMENT_SUBMISSION_FILE, Status.ERROR, e.getMessage(), user, null);
        }
    }

    private Assignment getValidAssignment(Course course, String assignmentName) {
        String id = course.getAssignmentIdByName(assignmentName);
        context.validationService().throwEntityDoesNotExistIfNull(id, "Assignment not found in course.");

        Assignment assignment = context.assignmentRepository().get(id);
        context.validationService().throwEntityDoesNotExistIfNull(assignment, "Assignment file is missing.");
        return assignment;
    }

    private Submission getValidSubmission(Assignment assignment, String studentUsername) {
        String submissionId = assignment.userToSubmissionId().get(studentUsername);
        context.validationService().throwEntityDoesNotExistIfNull(submissionId,
                "No submission found for this student.");

        Submission submission = context.submissionRepository().get(submissionId);
        context.validationService().throwEntityDoesNotExistIfNull(submission,
                "Submission file is missing.");
        return submission;
    }

    private void saveComment(Submission submission, String fileName, String comment) {
        Object lock = LockRegistry.getLock(submission.id());
        try {
            synchronized (lock) {
                context.submissionRepository().addFileComment(submission, fileName, comment);
            }
        } finally {
            LockRegistry.cleanup(submission.id(), lock);
        }
    }

    @Override
    public CommandType getType() {
        return CommandType.COMMENT_SUBMISSION_FILE;
    }
}
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

public class GradeSubmissionCommand extends AuthenticatedCommand {
    private static final int EXPECTED_PARAMS_COUNT = 5;
    private static final int COURSE_NAME_INDEX = 0;
    private static final int ASSIGNMENT_NAME_INDEX = 1;
    private static final int STUDENT_USERNAME_INDEX = 2;
    private static final int GRADE_INDEX = 3;
    private static final int COMMENT_INDEX = 4;

    private final List<String> args;

    public GradeSubmissionCommand(List<String> args) {
        this.args = args;
    }

    @Override
    public ServerResponse authenticatedExecute() {
        try {
            context.validationService().validateListParamsCount(args, EXPECTED_PARAMS_COUNT);

            String courseName = args.get(COURSE_NAME_INDEX);
            String assignmentName = args.get(ASSIGNMENT_NAME_INDEX);
            String studentUsername = args.get(STUDENT_USERNAME_INDEX);
            double grade = Double.parseDouble(args.get(GRADE_INDEX));
            String comment = args.get(COMMENT_INDEX);

            context.validationService().throwIfUnsafe(courseName,
                    "Course name cannot be null, empty or contain illegal symbols");

            String courseId = user.getAdminCourseIdFromName(courseName);
            Course course = context.courseRepository().get(courseId);
            context.validationService().throwEntityDoesNotExistIfNull(course, "Course not found.");

            Assignment assignment = getValidAssignment(course, assignmentName);
            Submission submission = getValidSubmission(assignment, studentUsername);

            updateSubmissionGrade(submission, grade, comment);

            return new ServerResponse(CommandType.GRADE_SUBMISSION, Status.OK,
                    "Submission graded successfully", user, submission);
        } catch (Exception e) {
            logger.logError("Exception occurred in grade submission command", e);
            return new ServerResponse(CommandType.GRADE_SUBMISSION, Status.ERROR, e.getMessage(), user, null);
        }
    }

    private Assignment getValidAssignment(Course course, String assignmentName) {
        String aId = course.getAssignmentIdByName(assignmentName);
        context.validationService().throwEntityDoesNotExistIfNull(aId, "Assignment not found in course.");

        Assignment assignment = context.assignmentRepository().get(aId);
        context.validationService().throwEntityDoesNotExistIfNull(assignment, "Assignment data missing.");
        return assignment;
    }

    private Submission getValidSubmission(Assignment assignment, String studentUsername) {
        String subId = assignment.userToSubmissionId().get(studentUsername);
        context.validationService().throwEntityDoesNotExistIfNull(subId, "No submission found for this student.");

        Submission submission = context.submissionRepository().get(subId);
        context.validationService().throwEntityDoesNotExistIfNull(submission, "Submission record missing.");
        return submission;
    }

    private void updateSubmissionGrade(Submission submission, double grade, String comment) {
        Object lock = LockRegistry.getLock(submission.id());
        try {
            synchronized (lock) {
                submission.grade(user.username(), grade, comment);
                context.submissionRepository().gradeSubmission(submission, user.username(), grade, comment);
            }
        } finally {
            LockRegistry.cleanup(submission.id(), lock);
        }
    }

    @Override
    public CommandType getType() {
        return CommandType.GRADE_SUBMISSION;
    }
}
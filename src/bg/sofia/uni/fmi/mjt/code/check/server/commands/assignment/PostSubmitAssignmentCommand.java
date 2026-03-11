package bg.sofia.uni.fmi.mjt.code.check.server.commands.assignment;

import bg.sofia.uni.fmi.mjt.code.check.server.commands.AuthenticatedCommand;
import bg.sofia.uni.fmi.mjt.code.check.server.commands.CommandType;
import bg.sofia.uni.fmi.mjt.code.check.contract.ServerResponse;
import bg.sofia.uni.fmi.mjt.code.check.server.entities.Submission;
import bg.sofia.uni.fmi.mjt.code.check.server.services.LockRegistry;

import java.util.List;

import bg.sofia.uni.fmi.mjt.code.check.server.entities.Assignment;
import bg.sofia.uni.fmi.mjt.code.check.server.entities.Course;
import bg.sofia.uni.fmi.mjt.code.check.contract.Status;

import java.time.LocalDateTime;

/**
 * A bit of a special command, only handles validation and post-upload
 * Actual file upload works with FileStreams and is managed by other services
 * */
public class PostSubmitAssignmentCommand extends AuthenticatedCommand {
    private static final int COURSE_NAME_INDEX = 0;
    private static final int ASSIGNMENT_NAME_INDEX = 1;

    private static final String MESSAGE_DELIMITER = ";";
    private static final int MESSAGE_STARTING_INDEX = 2;

    private final List<String> args;

    public PostSubmitAssignmentCommand(List<String> args) {
        this.args = args;
    }

    @Override
    protected ServerResponse authenticatedExecute() {
        String message = getFormattedMessage();
        try {
            String courseName = args.get(COURSE_NAME_INDEX);
            String assignmentName = args.get(ASSIGNMENT_NAME_INDEX);

            context.validationService().throwIfDirectoryDoesNotExist(context.uploadsService().uploadsRoot()
                    .resolve(courseName).resolve(assignmentName).resolve(user.username()));

            String courseId = user.getEnrollCourseIdFromName(courseName);
            Course course = context.courseRepository().get(courseId);
            context.validationService().throwEntityDoesNotExistIfNull(course, "Course does not exist");

            String assignmentId = course.getAssignmentIdByName(assignmentName);
            Assignment assignment = context.assignmentRepository().get(assignmentId);
            context.validationService().throwEntityDoesNotExistIfNull(assignment, "Assignment does not exist");

            Submission submission = transaction(assignment, courseName, assignmentName);
            return new ServerResponse(CommandType.POST_SUBMIT_ASSIGNMENT, Status.OK, message, user, submission);
        } catch (Exception e) {
            logger.logError("Exception occurred in post-submit command", e);
            return new ServerResponse(CommandType.POST_SUBMIT_ASSIGNMENT, Status.ERROR,
                    message + " " + e.getMessage(), user, null);
        }
    }

    @Override
    public CommandType getType() {
        return CommandType.POST_SUBMIT_ASSIGNMENT;
    }

    private String getFormattedMessage() {
        if (args.size() <= MESSAGE_STARTING_INDEX) {
            return "";
        }
        return String.join(MESSAGE_DELIMITER, args.subList(MESSAGE_STARTING_INDEX, args.size()));
    }

    private Submission transaction(Assignment assignment, String courseName, String assignmentName) {
        String subId = Submission.createId(courseName, assignmentName, user.username());
        Object lock = LockRegistry.getLock(assignment.id());
        try {
            synchronized (lock) {
                Submission submission = new Submission(
                        subId,
                        context.uploadsService().uploadsRoot()
                                .resolve(courseName).resolve(assignmentName).resolve(user.username()).toString(),
                        assignment.id(),
                        user.username(),
                        LocalDateTime.now());

                context.submissionRepository().add(submission);
                context.assignmentRepository().addSubmission(assignment, user.username(), subId);
                return submission;
            }
        } finally {
            LockRegistry.cleanup(assignment.id(), lock);
        }
    }
}
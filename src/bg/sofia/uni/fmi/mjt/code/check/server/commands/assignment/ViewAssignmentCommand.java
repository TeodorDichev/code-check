package bg.sofia.uni.fmi.mjt.code.check.server.commands.assignment;

import bg.sofia.uni.fmi.mjt.code.check.server.commands.AuthenticatedCommand;
import bg.sofia.uni.fmi.mjt.code.check.server.commands.CommandType;
import bg.sofia.uni.fmi.mjt.code.check.server.entities.Assignment;
import bg.sofia.uni.fmi.mjt.code.check.server.entities.Course;
import bg.sofia.uni.fmi.mjt.code.check.server.entities.Submission;
import bg.sofia.uni.fmi.mjt.code.check.server.exceptions.EntityDoesNotExistException;
import bg.sofia.uni.fmi.mjt.code.check.server.models.ListAdminAssignmentModel;
import bg.sofia.uni.fmi.mjt.code.check.server.models.ListStudentAssignmentModel;
import bg.sofia.uni.fmi.mjt.code.check.contract.ServerResponse;
import bg.sofia.uni.fmi.mjt.code.check.contract.Status;
import bg.sofia.uni.fmi.mjt.code.check.server.services.LockRegistry;

import java.util.List;

public class ViewAssignmentCommand extends AuthenticatedCommand {
    private static final String SUCCESSFUL_MSG = "Assignment loaded successfully";
    private static final int EXPECTED_PARAMS_COUNT = 2;
    private static final int COURSE_NAME_INDEX = 0;
    private static final int ASSIGNMENT_NAME_INDEX = 1;

    private static final String ASSIGNMENT_MISSING_LOG =
            "System fixed inconsistency: Assignment %s missing from course %s while user %s tried to view it";
    private static final String SUBMISSION_MISSING_LOG =
            "System fixed inconsistency: Submission %s missing from assignment %s (user: %s)";

    private final List<String> args;

    public ViewAssignmentCommand(List<String> args) {
        this.args = args;
    }

    @Override
    protected ServerResponse authenticatedExecute() {
        try {
            context.validationService().validateListParamsCount(args, EXPECTED_PARAMS_COUNT);
            String courseName = args.get(COURSE_NAME_INDEX);
            String assignmentName = args.get(ASSIGNMENT_NAME_INDEX);

            String courseId = findCourseId(courseName);
            Course course = getValidCourse(courseId);
            String assignmentId = getValidAssignmentId(course, assignmentName);
            Assignment assignment = getValidAssignment(course, assignmentId);

            boolean isAdmin = user.administeredCoursesIds().contains(courseId);
            Object model = isAdmin ? getAdminModel(assignment) : getStudentModel(assignment);

            return new ServerResponse(CommandType.VIEW_ASSIGNMENT, Status.OK, SUCCESSFUL_MSG, user, model);
        } catch (Exception e) {
            logger.logError("Exception occurred in view assignment command", e);
            return new ServerResponse(CommandType.VIEW_ASSIGNMENT, Status.ERROR, e.getMessage(), user, null);
        }
    }

    private String findCourseId(String courseName) {
        String id = user.getAdminCourseIdFromName(courseName);
        if (id == null) {
            id = user.getEnrollCourseIdFromName(courseName);
        }

        if (id == null) {
            throw new EntityDoesNotExistException("You are not part of this course.");
        }
        return id;
    }

    private Course getValidCourse(String courseId) {
        Course course = context.courseRepository().get(courseId);
        context.validationService().throwEntityDoesNotExistIfNull(course, "Course no longer exists.");
        return course;
    }

    private String getValidAssignmentId(Course course, String assignmentName) {
        String id = course.getAssignmentIdByName(assignmentName);
        if (id == null) {
            throw new EntityDoesNotExistException("Assignment not found in this course.");
        }
        return id;
    }

    private Assignment getValidAssignment(Course course, String aId) {
        Assignment assignment = context.assignmentRepository().get(aId);
        if (assignment == null) {
            handleMissingAssignment(course, aId);
            throw new EntityDoesNotExistException("Assignment file is missing and has been removed.");
        }
        return assignment;
    }

    private void handleMissingAssignment(Course course, String aId) {
        Object lock = LockRegistry.getLock(course.id());
        try {
            synchronized (lock) {
                context.courseRepository().removeAssignment(course, aId);
            }
        } finally {
            LockRegistry.cleanup(course.id(), lock);
        }
        logger.logWarning(String.format(ASSIGNMENT_MISSING_LOG, aId, course.id(), user.username()));
    }

    private ListAdminAssignmentModel getAdminModel(Assignment assignment) {
        return new ListAdminAssignmentModel(assignment, assignment.userToSubmissionId().size());
    }

    private ListStudentAssignmentModel getStudentModel(Assignment assignment) {
        String subId = assignment.userToSubmissionId().get(user.username());
        Submission submission = null;

        if (subId != null) {
            submission = context.submissionRepository().get(subId);
            if (submission == null) {
                handleMissingSubmission(assignment, subId);
            }
        }

        return new ListStudentAssignmentModel(assignment, submission);
    }

    private void handleMissingSubmission(Assignment assignment, String subId) {
        Object lock = LockRegistry.getLock(assignment.id());
        try {
            synchronized (lock) {
                context.assignmentRepository().removeSubmission(assignment, user.username());
            }
        } finally {
            LockRegistry.cleanup(assignment.id(), lock);
        }
        logger.logWarning(String.format(SUBMISSION_MISSING_LOG, subId, assignment.id(), user.username()));
    }

    @Override
    public CommandType getType() {
        return CommandType.VIEW_ASSIGNMENT;
    }
}
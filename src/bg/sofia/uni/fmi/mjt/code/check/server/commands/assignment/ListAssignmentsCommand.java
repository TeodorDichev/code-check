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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ListAssignmentsCommand extends AuthenticatedCommand {
    private static final String SUCCESSFUL_MSG = "Assignments acquired successfully";
    private static final String ASSIGNMENT_MISSING_LOG =
            "System fixed inconsistency: Assignment %s missing from course %s";
    private static final String SUBMISSION_MISSING_LOG =
            "System fixed inconsistency: Submission %s missing from assignment %s (user: %s)";

    private static final int EXPECTED_PARAMS_COUNT = 1;
    private static final int COURSE_NAME_INDEX = 0;

    private final List<String> args;

    public ListAssignmentsCommand(List<String> args) {
        this.args = args;
    }

    @Override
    protected ServerResponse authenticatedExecute() {
        try {
            context.validationService().validateListParamsCount(args, EXPECTED_PARAMS_COUNT);
            String courseName = args.get(COURSE_NAME_INDEX);
            String adminCourseId = user.getAdminCourseIdFromName(courseName);
            if (adminCourseId != null) {
                return listAdminAssignments(adminCourseId);
            }

            String enrolledCourseId = user.getEnrollCourseIdFromName(courseName);
            if (enrolledCourseId != null) {
                return listStudentAssignments(enrolledCourseId);
            }

            return new ServerResponse(CommandType.LIST_ASSIGNMENTS, Status.ERROR,
                    "You are not part of this course: " + courseName, user, null);
        } catch (Exception e) {
            logger.logError("Exception occurred while listing assignments", e);
            return new ServerResponse(CommandType.LIST_ASSIGNMENTS, Status.ERROR, e.getMessage(), user, null);
        }
    }

    private ServerResponse listStudentAssignments(String courseId) {
        Course course = context.courseRepository().get(courseId);
        if (course == null) {
            throw new EntityDoesNotExistException("Course not found");
        }

        Set<ListStudentAssignmentModel> data = getStudentAssignments(course);
        return new ServerResponse(CommandType.LIST_ASSIGNMENTS, Status.OK, SUCCESSFUL_MSG, user, data);
    }

    private Set<ListStudentAssignmentModel> getStudentAssignments(Course course) {
        Set<ListStudentAssignmentModel> data = new HashSet<>();
        Set<String> ids = new HashSet<>(course.assignmentIds());

        for (String aId : ids) {
            Assignment assignment = context.assignmentRepository().get(aId);

            if (assignment == null) {
                handleMissingAssignment(course, aId);
                continue;
            }

            String subId = assignment.userToSubmissionId().get(user.username());
            Submission userSubmission = null;

            if (subId != null) {
                userSubmission = context.submissionRepository().get(subId);
                if (userSubmission == null) {
                    handleMissingSubmission(assignment, subId);
                }
            }
            data.add(new ListStudentAssignmentModel(assignment, userSubmission));
        }
        return data;
    }

    private ServerResponse listAdminAssignments(String courseId) {
        Course course = context.courseRepository().get(courseId);
        if (course == null) {
            throw new EntityDoesNotExistException("Course not found");
        }

        Set<ListAdminAssignmentModel> data = getAdminAssignments(course);
        return new ServerResponse(CommandType.LIST_ASSIGNMENTS, Status.OK, SUCCESSFUL_MSG, user, data);
    }

    private Set<ListAdminAssignmentModel> getAdminAssignments(Course course) {
        Set<ListAdminAssignmentModel> data = new HashSet<>();

        for (String aId : new HashSet<>(course.assignmentIds())) {
            Assignment assignment = context.assignmentRepository().get(aId);

            if (assignment == null) {
                handleMissingAssignment(course, aId);
                continue;
            }

            int submissionsCount = assignment.userToSubmissionId().size();
            data.add(new ListAdminAssignmentModel(assignment, submissionsCount));
        }
        return data;
    }

    private void handleMissingAssignment(Course course, String aId) {
        Object lock = LockRegistry.getLock(course.id());
        try {
            synchronized (lock) {
                context.courseRepository().removeAssignment(course, aId);
                logger.logWarning(String.format(ASSIGNMENT_MISSING_LOG, aId, course.id()));
            }
        } finally {
            LockRegistry.cleanup(course.id(), lock);
        }
    }

    private void handleMissingSubmission(Assignment assignment, String subId) {
        Object lock = LockRegistry.getLock(assignment.id());
        try {
            synchronized (lock) {
                context.assignmentRepository().removeSubmission(assignment, user.username());
                logger.logWarning(String.format(SUBMISSION_MISSING_LOG, subId, assignment.id(), user.username()));
            }
        } finally {
            LockRegistry.cleanup(assignment.id(), lock);
        }
    }

    @Override
    public CommandType getType() {
        return CommandType.LIST_ASSIGNMENTS;
    }
}
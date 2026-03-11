package bg.sofia.uni.fmi.mjt.code.check.server.commands.submission;

import bg.sofia.uni.fmi.mjt.code.check.contract.Status;
import bg.sofia.uni.fmi.mjt.code.check.server.commands.AuthenticatedCommand;
import bg.sofia.uni.fmi.mjt.code.check.server.commands.CommandType;
import bg.sofia.uni.fmi.mjt.code.check.contract.ServerResponse;
import bg.sofia.uni.fmi.mjt.code.check.server.entities.Assignment;
import bg.sofia.uni.fmi.mjt.code.check.server.entities.Course;
import bg.sofia.uni.fmi.mjt.code.check.server.entities.Submission;
import bg.sofia.uni.fmi.mjt.code.check.server.models.CompilationResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestSubmissionsCommand extends AuthenticatedCommand {
    private static final int EXPECTED_PARAMS_COUNT = 2;
    private static final int COURSE_NAME_INDEX = 0;
    private static final int ASSIGNMENT_NAME_INDEX = 1;

    private final List<String> args;

    public TestSubmissionsCommand(List<String> args) {
        this.args = args;
    }

    @Override
    protected ServerResponse authenticatedExecute() {
        try {
            context.validationService().validateListParamsCount(args, EXPECTED_PARAMS_COUNT);
            String courseName = args.get(COURSE_NAME_INDEX);
            String assignmentName = args.get(ASSIGNMENT_NAME_INDEX);

            context.validationService().throwIfUnsafe(courseName,
                    "Course name cannot be null, empty or contain illegal symbols");

            String courseId = user.getAdminCourseIdFromName(courseName);
            Course course = context.courseRepository().get(courseId);
            context.validationService().throwEntityDoesNotExistIfNull(course, "Course not found");

            Assignment assignment = getValidAssignment(course, assignmentName);
            Map<String, CompilationResult> results = runBulkCompilation(assignment);

            return new ServerResponse(CommandType.TEST_SUBMISSIONS, Status.OK,
                    "Bulk testing complete. Tested " + results.size() + " submissions.", user, results);
        } catch (Exception e) {
            logger.logError("Exception occurred in bulk test submissions command", e);
            return new ServerResponse(CommandType.TEST_SUBMISSIONS, Status.ERROR, e.getMessage(), user, null);
        }
    }

    private Assignment getValidAssignment(Course course, String assignmentName) {
        String assignmentId = course.getAssignmentIdByName(assignmentName);
        Assignment assignment = context.assignmentRepository().get(assignmentId);
        context.validationService().throwEntityDoesNotExistIfNull(assignment, "Assignment not found");
        return assignment;
    }

    private Map<String, CompilationResult> runBulkCompilation(Assignment assignment) {
        Map<String, CompilationResult> results = new HashMap<>();
        Map<String, String> userToSubId = assignment.userToSubmissionId();

        for (Map.Entry<String, String> entry : userToSubId.entrySet()) {
            Submission sub = context.submissionRepository().get(entry.getValue());
            if (sub != null && sub.path() != null) {
                results.put(entry.getKey(), context.compilerService().compileFiles(sub.path()));
            }
        }
        return results;
    }

    @Override
    public CommandType getType() {
        return CommandType.TEST_SUBMISSIONS;
    }
}
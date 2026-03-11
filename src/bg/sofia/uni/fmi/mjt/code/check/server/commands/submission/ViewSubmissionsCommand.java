package bg.sofia.uni.fmi.mjt.code.check.server.commands.submission;

import bg.sofia.uni.fmi.mjt.code.check.contract.Status;
import bg.sofia.uni.fmi.mjt.code.check.server.commands.AuthenticatedCommand;
import bg.sofia.uni.fmi.mjt.code.check.server.commands.CommandType;
import bg.sofia.uni.fmi.mjt.code.check.contract.ServerResponse;
import bg.sofia.uni.fmi.mjt.code.check.server.entities.Assignment;
import bg.sofia.uni.fmi.mjt.code.check.server.entities.Course;
import bg.sofia.uni.fmi.mjt.code.check.server.entities.Submission;
import bg.sofia.uni.fmi.mjt.code.check.server.models.SubmissionView;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ViewSubmissionsCommand extends AuthenticatedCommand {
    private static final int EXPECTED_PARAMS_COUNT = 2;
    private static final int COURSE_NAME_INDEX = 0;
    private static final int ASSIGNMENT_NAME_INDEX = 1;
    private static final String ACCEPTABLE_FILE_EXTENSION = ".java";

    private final List<String> args;

    public ViewSubmissionsCommand(List<String> args) {
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
            context.validationService().throwEntityDoesNotExistIfNull(course, "Course does not exist");

            Assignment assignment = getValidAssignment(course, assignmentName);
            List<SubmissionView> submissionViews = new ArrayList<>();
            Map<String, String> userToSubId = assignment.userToSubmissionId();

            for (String subId : userToSubId.values()) {
                Submission sub = context.submissionRepository().get(subId);
                if (sub != null) {
                    submissionViews.add(mapToView(sub));
                }
            }

            return new ServerResponse(getType(), Status.OK, "Submissions loaded successfully", user, submissionViews);
        } catch (Exception e) {
            logger.logError("Exception occurred in view submissions command", e);
            return new ServerResponse(getType(), Status.ERROR, e.getMessage(), user, null);
        }
    }

    private Assignment getValidAssignment(Course course, String assignmentName) {
        String assignmentId = course.getAssignmentIdByName(assignmentName);
        Assignment assignment = context.assignmentRepository().get(assignmentId);
        context.validationService().throwEntityDoesNotExistIfNull(assignment, "Assignment does not exist");
        return assignment;
    }

    private SubmissionView mapToView(Submission submission) {
        List<String> fileNames = List.of();
        if (submission.path() != null) {
            Path rootPath = Path.of(submission.path());
            int rootNameCount = rootPath.getNameCount();

            try (var stream = Files.walk(rootPath)) {
                fileNames = stream
                        .filter(Files::isRegularFile)
                        .filter(p -> p.toString().endsWith(ACCEPTABLE_FILE_EXTENSION))
                        .map(p -> p.subpath(rootNameCount, p.getNameCount()).toString())
                        .toList();
            } catch (IOException e) {
                logger.logError("Failed to map submission files for view", e);
            }
        }

        return new SubmissionView(
                submission.id(),
                submission.submittedBy(),
                submission.submittedOn(),
                submission.grade(),
                submission.comment(),
                fileNames,
                submission.fileComments()
        );
    }

    @Override
    public CommandType getType() {
        return CommandType.VIEW_SUBMISSIONS;
    }
}
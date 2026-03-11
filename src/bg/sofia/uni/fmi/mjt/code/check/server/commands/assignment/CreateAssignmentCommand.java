package bg.sofia.uni.fmi.mjt.code.check.server.commands.assignment;

import bg.sofia.uni.fmi.mjt.code.check.server.commands.AuthenticatedCommand;
import bg.sofia.uni.fmi.mjt.code.check.server.commands.CommandType;
import bg.sofia.uni.fmi.mjt.code.check.server.entities.Assignment;
import bg.sofia.uni.fmi.mjt.code.check.server.entities.Course;
import bg.sofia.uni.fmi.mjt.code.check.contract.ServerResponse;
import bg.sofia.uni.fmi.mjt.code.check.contract.Status;
import bg.sofia.uni.fmi.mjt.code.check.server.services.LockRegistry;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

public class CreateAssignmentCommand extends AuthenticatedCommand {
    private static final int EXPECTED_PARAMS_COUNT = 4;
    private static final int COURSE_NAME_INDEX = 0;
    private static final int ASSIGNMENT_NAME_INDEX = 1;
    private static final int ASSIGNMENT_DESCRIPTION_INDEX = 2;
    private static final int ASSIGNMENT_DEADLINE_INDEX = 3;

    private final List<String> args;

    public CreateAssignmentCommand(List<String> args) {
        this.args = args;
    }

    @Override
    protected ServerResponse authenticatedExecute() {
        try {
            context.validationService().validateListParamsCount(args, EXPECTED_PARAMS_COUNT);
            String courseName = args.get(COURSE_NAME_INDEX);
            String assignmentName = args.get(ASSIGNMENT_NAME_INDEX);
            String assignmentDesc = args.get(ASSIGNMENT_DESCRIPTION_INDEX);
            String assignmentDeadline = args.get(ASSIGNMENT_DEADLINE_INDEX);

            String courseId = user.getAdminCourseIdFromName(courseName);
            Course course = context.courseRepository().get(courseId);

            context.validationService().throwEntityDoesNotExistIfNull(course, "Course does not exist");
            context.validationService().throwEntityAlreadyExistsIfContained(course.assignmentNames(), assignmentName,
                    "Assignment with this name already exists");
            context.validationService().throwIfUnsafe(assignmentName,
                    "Assignment name is null, empty or contains forbidden symbols");

            Assignment assignment = transaction(course, assignmentName, assignmentDesc, assignmentDeadline);
            return new ServerResponse(CommandType.CREATE_ASSIGNMENT, Status.OK,
                    "Assignment created successfully", user, assignment);
        } catch (Exception e) {
            logger.logError("Exception occurred while user was trying to create assignment", e);
            return new ServerResponse(CommandType.CREATE_ASSIGNMENT, Status.ERROR, e.getMessage(), user, null);
        }
    }

    private static final String DATE_TIME_FORMAT = "dd-MM-yyyy";
    private LocalDateTime getDeadline(String assignmentDeadline) {
        try {
            LocalDate date = LocalDate.parse(assignmentDeadline, DateTimeFormatter.ofPattern(DATE_TIME_FORMAT));
            LocalDateTime deadline = LocalDateTime.of(date, LocalTime.MAX);

            if (deadline.isBefore(LocalDateTime.now())) {
                throw new IllegalArgumentException("Deadline cannot be in the past");
            }

            return deadline;
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Unable to parse deadline date string. Expected format: dd-MM-yyyy");
        }
    }

    private Assignment transaction(Course course,
                                   String assignmentName, String assignmentDesc, String deadline) {
        String assignmentId = Assignment.createId(assignmentName, course.name(), user.username());
        Object lock = LockRegistry.getLock(course.id());
        try {
            synchronized (lock) {
                Assignment assignmentToAdd = new Assignment(
                        assignmentId,
                        course.id(),
                        assignmentName,
                        assignmentDesc,
                        LocalDateTime.now(),
                        getDeadline(deadline)
                );

                context.assignmentRepository().add(assignmentToAdd);
                context.courseRepository().addAssignment(course, assignmentToAdd.id());
                return assignmentToAdd;
            }
        } finally {
            LockRegistry.cleanup(course.id(), lock);
        }
    }

    @Override
    public CommandType getType() {
        return CommandType.CREATE_ASSIGNMENT;
    }
}

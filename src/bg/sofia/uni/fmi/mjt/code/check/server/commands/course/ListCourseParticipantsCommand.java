package bg.sofia.uni.fmi.mjt.code.check.server.commands.course;

import bg.sofia.uni.fmi.mjt.code.check.server.commands.AuthenticatedCommand;
import bg.sofia.uni.fmi.mjt.code.check.server.commands.CommandType;
import bg.sofia.uni.fmi.mjt.code.check.server.entities.Course;
import bg.sofia.uni.fmi.mjt.code.check.server.entities.User;
import bg.sofia.uni.fmi.mjt.code.check.server.exceptions.UnauthorizedException;
import bg.sofia.uni.fmi.mjt.code.check.contract.ServerResponse;
import bg.sofia.uni.fmi.mjt.code.check.contract.Status;
import bg.sofia.uni.fmi.mjt.code.check.server.services.LockRegistry;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ListCourseParticipantsCommand extends AuthenticatedCommand {
    private static final String STUDENT_MISSING_LOG =
            "System fixed inconsistency: Student %s missing from system, removed from course %s";

    private static final int EXPECTED_PARAMS_COUNT = 1;
    private static final int COURSE_NAME_INDEX = 0;

    private final List<String> args;

    public ListCourseParticipantsCommand(List<String> args) {
        this.args = args;
    }

    @Override
    protected ServerResponse authenticatedExecute() {
        try {
            context.validationService().validateListParamsCount(args, EXPECTED_PARAMS_COUNT);
            String courseName = args.get(COURSE_NAME_INDEX);

            context.validationService().throwIfUnsafe(courseName,
                    "Course name cannot be null, empty or contain illegal symbols");

            String courseId = user.getAdminCourseIdFromName(courseName);
            if (courseId == null) {
                throw new UnauthorizedException("Only course administrators can view the participants list.");
            }

            Course course = context.courseRepository().get(courseId);
            context.validationService().throwEntityDoesNotExistIfNull(course, "Course data not found.");

            Set<User> participants = fetchAndCleanParticipants(course);

            return new ServerResponse(CommandType.LIST_COURSE_PARTICIPANTS,
                    Status.OK, "Participants acquired successfully", user, participants);
        } catch (Exception e) {
            logger.logError("Exception occurred in list participants command", e);
            return new ServerResponse(CommandType.LIST_COURSE_PARTICIPANTS, Status.ERROR, e.getMessage(), user, null);
        }
    }

    private Set<User> fetchAndCleanParticipants(Course course) {
        Set<User> validParticipants = new HashSet<>();
        Set<String> usernames = new HashSet<>(course.studentUsernames());

        for (String username : usernames) {
            User participant = context.userRepository().get(username);

            if (participant == null) {
                handleMissingStudent(course, username);
                continue;
            }
            validParticipants.add(participant);
        }

        return validParticipants;
    }

    private void handleMissingStudent(Course course, String username) {
        Object lock = LockRegistry.getLock(course.id());
        try {
            synchronized (lock) {
                context.courseRepository().removeStudent(course, username);
                logger.logWarning(String.format(STUDENT_MISSING_LOG, username, course.id()));
            }
        } finally {
            LockRegistry.cleanup(course.id(), lock);
        }
    }

    @Override
    public CommandType getType() {
        return CommandType.LIST_COURSE_PARTICIPANTS;
    }
}
package bg.sofia.uni.fmi.mjt.code.check.server.commands.course;

import bg.sofia.uni.fmi.mjt.code.check.server.commands.AuthenticatedCommand;
import bg.sofia.uni.fmi.mjt.code.check.server.commands.CommandType;
import bg.sofia.uni.fmi.mjt.code.check.server.entities.Course;
import bg.sofia.uni.fmi.mjt.code.check.server.entities.User;
import bg.sofia.uni.fmi.mjt.code.check.contract.ServerResponse;
import bg.sofia.uni.fmi.mjt.code.check.contract.Status;
import bg.sofia.uni.fmi.mjt.code.check.server.services.LockRegistry;

import java.util.List;

public class UpgradeParticipantCommand extends AuthenticatedCommand {
    private static final int EXPECTED_PARAMS_COUNT = 2;
    private static final int COURSE_NAME_INDEX = 0;
    private static final int USERNAME_INDEX = 1;

    private final List<String> args;

    public UpgradeParticipantCommand(List<String> args) {
        this.args = args;
    }

    @Override
    protected ServerResponse authenticatedExecute() {
        try {
            context.validationService().validateListParamsCount(args, EXPECTED_PARAMS_COUNT);
            String courseName = args.get(COURSE_NAME_INDEX);
            String targetUsername = args.get(USERNAME_INDEX);

            context.validationService().throwIfUnsafe(courseName,
                    "Course name cannot be null, empty or contain illegal symbols");

            String courseId = user.getAdminCourseIdFromName(courseName);
            Course course = context.courseRepository().get(courseId);
            User userToAdminister = context.userRepository().get(targetUsername);

            validate(course, userToAdminister);
            transaction(course, userToAdminister);
            // needed because user is cached
            sessionStore.updateUser(targetUsername, userToAdminister);

            return new ServerResponse(CommandType.UPGRADE_PARTICIPANT, Status.OK,
                    "User upgraded successfully", user, course);
        } catch (Exception e) {
            logger.logError("Exception occurred in upgrade participant command", e);
            return new ServerResponse(CommandType.UPGRADE_PARTICIPANT, Status.ERROR, e.getMessage(), user, null);
        }
    }

    private void validate(Course course, User userToAdminister) {
        context.validationService().throwEntityDoesNotExistIfNull(course, "Course does not exist");
        context.validationService().throwEntityDoesNotExistIfNull(userToAdminister, "This user does not exist");

        context.validationService().throwEntityAlreadyExistsIfContained(
                course.adminUsernames(), userToAdminister.username(), "This user already administers the course");

        context.validationService().throwEntityDoesNotExistsIfNotContained(
                course.studentUsernames(), userToAdminister.username(),
                "The course does not have this user as a participant");
    }

    /**
     * Updates two entities, we need two locks
     * Possible deadlock???
     * */
    private void transaction(Course course, User targetUser) {
        String firstId;
        String secondId;

        // Determine global order to prevent deadlocks
        if (course.id().compareTo(targetUser.username()) < 0) {
            firstId = course.id();
            secondId = targetUser.username();
        } else {
            firstId = targetUser.username();
            secondId = course.id();
        }
        Object firstLock = LockRegistry.getLock(firstId);
        Object secondLock = LockRegistry.getLock(secondId);
        try {
            synchronized (firstLock) {
                synchronized (secondLock) {
                    context.userRepository().removeEnrolledCourse(targetUser, course.id());
                    context.courseRepository().removeStudent(course, targetUser.username());

                    context.userRepository().addAdministeredCourse(targetUser, course.id());
                    context.courseRepository().addAdmin(course, targetUser.username());
                }
            }
        } finally { // Cleanup in reverse order
            LockRegistry.cleanup(secondId, secondLock);
            LockRegistry.cleanup(firstId, firstLock);
        }
    }

    @Override
    public CommandType getType() {
        return CommandType.UPGRADE_PARTICIPANT;
    }
}
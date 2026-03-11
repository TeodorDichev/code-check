package bg.sofia.uni.fmi.mjt.code.check.server.commands.course;

import bg.sofia.uni.fmi.mjt.code.check.server.commands.AuthenticatedCommand;
import bg.sofia.uni.fmi.mjt.code.check.server.commands.CommandType;
import bg.sofia.uni.fmi.mjt.code.check.server.entities.Course;
import bg.sofia.uni.fmi.mjt.code.check.server.exceptions.EntityDoesNotExistException;
import bg.sofia.uni.fmi.mjt.code.check.contract.ServerResponse;
import bg.sofia.uni.fmi.mjt.code.check.contract.Status;
import bg.sofia.uni.fmi.mjt.code.check.server.services.LockRegistry;

import java.util.List;

public class JoinCourseCommand extends AuthenticatedCommand {
    private static final int EXPECTED_PARAMS_COUNT = 2;
    private static final int COURSE_NAME_INDEX = 0;
    private static final int JOIN_STRING_INDEX = 1;

    private final List<String> args;

    public JoinCourseCommand(List<String> args) {
        this.args = args;
    }

    @Override
    protected ServerResponse authenticatedExecute() {
        try {
            context.validationService().validateListParamsCount(args, EXPECTED_PARAMS_COUNT);
            String courseName = args.get(COURSE_NAME_INDEX);
            String joinString = args.get(JOIN_STRING_INDEX);

            context.validationService().throwIfUnsafe(courseName,
                    "Course name cannot be null, empty or contain illegal symbols");

            Course course = context.courseRepository().getCourseByNameAndJoinString(courseName, joinString);
            validateCourseNotAlreadyJoined(course);

            transaction(course);

            return new ServerResponse(CommandType.JOIN_COURSE, Status.OK, "Course joined successfully", user, course);
        } catch (Exception e) {
            logger.logError("Exception occurred in join course command", e);
            return new ServerResponse(CommandType.JOIN_COURSE, Status.ERROR, e.getMessage(), user, null);
        }
    }

    private void validateCourseNotAlreadyJoined(Course course) {
        if (course == null) {
            throw new EntityDoesNotExistException("Course does not exist or invalid join code");
        }

        boolean courseJoined = user.enrolledCourseIds().contains(course.id());
        boolean courseAdministered = user.administeredCoursesIds().contains(course.id());

        if (courseJoined || courseAdministered) {
            throw new IllegalArgumentException(String.format(
                    "Course with name: %s already is already joined (as admin or student)", course.name()));
        }
    }

    private void transaction(Course course) {
        Object lock = LockRegistry.getLock(course.id());
        try {
            synchronized (lock) {
                context.userRepository().addEnrolledCourse(user, course.id());
                context.courseRepository().enrollStudent(course, user.username());
            }
        } finally {
            LockRegistry.cleanup(course.id(), lock);
        }
    }

    @Override
    public CommandType getType() {
        return CommandType.JOIN_COURSE;
    }
}
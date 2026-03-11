package bg.sofia.uni.fmi.mjt.code.check.server.commands.course;

import bg.sofia.uni.fmi.mjt.code.check.server.commands.AuthenticatedCommand;
import bg.sofia.uni.fmi.mjt.code.check.server.commands.CommandType;
import bg.sofia.uni.fmi.mjt.code.check.server.entities.Course;
import bg.sofia.uni.fmi.mjt.code.check.contract.ServerResponse;
import bg.sofia.uni.fmi.mjt.code.check.contract.Status;
import bg.sofia.uni.fmi.mjt.code.check.server.services.LockRegistry;
import bg.sofia.uni.fmi.mjt.code.check.server.services.UniqueStringGenerator;

import java.util.List;
import java.util.Set;

public class CreateCourseCommand extends AuthenticatedCommand {
    private static final int EXPECTED_PARAMS_COUNT = 1;
    private static final int COURSE_NAME_INDEX = 0;

    private final List<String> args;

    public CreateCourseCommand(List<String> args) {
        this.args = args;
    }

    @Override
    protected ServerResponse authenticatedExecute() {
        try {
            context.validationService().validateListParamsCount(args, EXPECTED_PARAMS_COUNT);
            String courseName = args.get(COURSE_NAME_INDEX);

            context.validationService().throwIfUnsafe(courseName,
                    "Course name cannot be null, empty or contain illegal symbols");
            context.validationService().throwEntityAlreadyExistsIfContained(user.administeredCoursesNames(),
                    courseName, "Course with this name already exists");

            String id = Course.createId(courseName, user.username());
            Course createdCourse = transaction(id, courseName);

            return new ServerResponse(CommandType.CREATE_COURSE, Status.OK,
                    "Course created successfully", user, createdCourse);
        } catch (Exception e) {
            logger.logError("Exception occurred in create course command", e);
            return new ServerResponse(CommandType.CREATE_COURSE, Status.ERROR, e.getMessage(), user, null);
        }
    }

    private Course transaction(String id, String courseName) {
        Object lock = LockRegistry.getLock(id);
        try {
            synchronized (lock) {
                Course newCourse = new Course(
                        id, courseName, UniqueStringGenerator.generate(),
                        Set.of(), Set.of(user.username()), Set.of()
                );

                context.courseRepository().add(newCourse);
                context.userRepository().addAdministeredCourse(user, id);
                return newCourse;
            }
        } finally {
            LockRegistry.cleanup(id, lock);
        }
    }

    @Override
    public CommandType getType() {
        return CommandType.CREATE_COURSE;
    }
}
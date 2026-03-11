package bg.sofia.uni.fmi.mjt.code.check.server.commands.course;

import bg.sofia.uni.fmi.mjt.code.check.server.commands.AuthenticatedCommand;
import bg.sofia.uni.fmi.mjt.code.check.server.commands.CommandType;
import bg.sofia.uni.fmi.mjt.code.check.server.entities.Course;
import bg.sofia.uni.fmi.mjt.code.check.server.models.ListCoursesModel;
import bg.sofia.uni.fmi.mjt.code.check.contract.ServerResponse;
import bg.sofia.uni.fmi.mjt.code.check.contract.Status;
import bg.sofia.uni.fmi.mjt.code.check.server.services.LockRegistry;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ListCoursesCommand extends AuthenticatedCommand {
    private static final String SYSTEM_FIX_MSG =
            "System silently fixed memory inconsistency with user: %s and course: %s";

    private static final int EXPECTED_PARAMS_COUNT = 0;

    private final List<String> args;

    public ListCoursesCommand(List<String> args) {
        this.args = args;
    }

    @Override
    protected ServerResponse authenticatedExecute() {
        Object lock = LockRegistry.getLock(user.username());
        try {
            context.validationService().validateListParamsCount(args, EXPECTED_PARAMS_COUNT);

            ListCoursesModel courses;
            // needed because we are removing data from in-memory user
            synchronized (lock) {
                courses = new ListCoursesModel(fetchAndCleanAdminCourses(), fetchAndCleanEnrolledCourses());
            }

            return new ServerResponse(CommandType.LIST_COURSES, Status.OK,
                    "Courses loaded successfully", user, courses);
        } catch (Exception e) {
            logger.logError("Exception occurred in list courses command", e);
            return new ServerResponse(CommandType.LIST_COURSES, Status.ERROR, e.getMessage(), user, null);
        } finally {
            LockRegistry.cleanup(user.username(), lock);
        }
    }

    private Set<Course> fetchAndCleanAdminCourses() {
        Set<String> courseIds = new HashSet<>(user.administeredCoursesIds());
        Set<Course> validCourses = new HashSet<>();

        for (String courseId : courseIds) {
            Course course = context.courseRepository().get(courseId);

            if (course == null) {
                context.userRepository().removeAdministeredCourse(user, courseId);
                logger.logWarning(String.format(SYSTEM_FIX_MSG, user.username(), courseId));
                continue;
            }
            validCourses.add(course);
        }
        return validCourses;
    }

    private Set<Course> fetchAndCleanEnrolledCourses() {
        Set<String> courseIds = new HashSet<>(user.enrolledCourseIds());
        Set<Course> validCourses = new HashSet<>();

        for (String courseId : courseIds) {
            Course course = context.courseRepository().get(courseId);

            if (course == null) {
                context.userRepository().removeEnrolledCourse(user, courseId);
                logger.logWarning(String.format(SYSTEM_FIX_MSG, user.username(), courseId));
                continue;
            }
            validCourses.add(course);
        }
        return validCourses;
    }

    @Override
    public CommandType getType() {
        return CommandType.LIST_COURSES;
    }
}
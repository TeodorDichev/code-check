package bg.sofia.uni.fmi.mjt.code.check.server.repositories;

import bg.sofia.uni.fmi.mjt.code.check.server.entities.Course;
import bg.sofia.uni.fmi.mjt.code.check.server.exceptions.EntityAlreadyExistsException;
import bg.sofia.uni.fmi.mjt.code.check.server.exceptions.EntityDoesNotExistException;
import bg.sofia.uni.fmi.mjt.code.check.server.services.ValidationService;
import bg.sofia.uni.fmi.mjt.code.check.server.storage.CourseStorage;

import java.util.List;

public class CourseRepository implements Repository<Course> {
    private static final String COURSE_ID_SEPARATOR = "_";

    private final CourseStorage storage;
    private final ValidationService validationService;

    public CourseRepository(CourseStorage storage, ValidationService validationService) {
        this.storage = storage;
        this.validationService = validationService;
    }

    @Override
    public Course get(String id) {
        validationService.throwIfNullOrEmpty(id, "Course id cannot be null or empty");
        return storage.load(id).orElse(null);
    }

    @Override
    public boolean contains(String id) {
        validationService.throwIfNullOrEmpty(id, "Course id cannot be null or empty");
        return storage.exists(id);
    }

    public boolean existsByName(String courseName) {
        validationService.throwIfNullOrEmpty(courseName, "Course name cannot be null or empty");
        return !storage.findAllIdsByPrefix(courseName).isEmpty();
    }

    @Override
    public Course add(Course entity) {
        validationService.throwIfNull(entity, "Course cannot be null");
        String id = entity.id();
        validationService.throwIfNull(id, "Course id cannot be null or empty");

        if (storage.exists(id)) {
            throw new EntityAlreadyExistsException("Course " + id + " already exists");
        }

        storage.save(id, entity);
        return entity;
    }

    public boolean isUserCourseAdmin(String username, String id) {
        Course course = get(id);
        if (course == null) {
            throw new EntityDoesNotExistException("Course " + id + " does not exist");
        }
        return course.adminUsernames().contains(username);
    }

    public Course getCourseByNameAndJoinString(String courseName, String joinString) {
        List<String> potentialIds = storage.findAllIdsByPrefix(courseName + COURSE_ID_SEPARATOR);

        for (String id : potentialIds) {
            Course course = get(id);
            if (course != null && course.joinString().equals(joinString)) {
                return course;
            }
        }

        throw new EntityDoesNotExistException("Invalid course credentials");
    }

    public void enrollStudent(Course course, String studentUsername) {
        validationService.throwIfNullOrEmpty(studentUsername, "Student username cannot be null or empty");
        validationService.throwIfNull(course, "Course cannot be null");
        course.addStudent(studentUsername);
        storage.save(course.id(), course);
    }

    public void removeStudent(Course course, String studentUsername) {
        validationService.throwIfNullOrEmpty(studentUsername, "Student username cannot be null or empty");
        validationService.throwIfNull(course, "Course cannot be null");
        course.removeStudent(studentUsername);
        storage.save(course.id(), course);
    }

    public void addAdmin(Course course, String username) {
        validationService.throwIfNullOrEmpty(username, "Admin username cannot be null or empty");
        validationService.throwIfNull(course, "Course cannot be null");
        course.addAdmin(username);
        storage.save(course.id(), course);
    }

    public void addAssignment(Course course, String id) {
        validationService.throwIfNullOrEmpty(id, "Assignment id cannot be null or empty");
        validationService.throwIfNull(course, "Course cannot be null");
        course.addAssignment(id);
        storage.save(course.id(), course);
    }

    public void removeAssignment(Course course, String id) {
        validationService.throwIfNullOrEmpty(id, "Assignment id cannot be null or empty");
        validationService.throwIfNull(course, "Course cannot be null");
        course.removeAssignment(id);
        storage.save(course.id(), course);
    }

    public void remove(String id) {
        validationService.throwIfNullOrEmpty(id, "Course id cannot be null or empty");
        storage.delete(id);
    }
}
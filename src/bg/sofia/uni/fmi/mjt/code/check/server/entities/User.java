package bg.sofia.uni.fmi.mjt.code.check.server.entities;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class User {
    private static final String FILE_SEPARATOR = "_";

    private final String username;
    private final String passwordHash;
    private final Set<String> enrolledCoursesIds;
    private final Set<String> administeredCoursesIds;

    public User(String username, String passwordHash) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.enrolledCoursesIds = ConcurrentHashMap.newKeySet();
        this.administeredCoursesIds = ConcurrentHashMap.newKeySet();
    }

    public String username() {
        return username;
    }

    public String passwordHash() {
        return passwordHash;
    }

    public Set<String> administeredCoursesIds() {
        return Collections.unmodifiableSet(administeredCoursesIds);
    }

    public Set<String> enrolledCourseIds() {
        return Collections.unmodifiableSet(enrolledCoursesIds);
    }

    public void enrollInCourse(String courseId) {
        enrolledCoursesIds.add(courseId);
    }

    public void removeEnrolledCourse(String courseId) {
        enrolledCoursesIds.remove(courseId);
    }

    public void administerCourse(String courseId) {
        administeredCoursesIds.add(courseId);
    }

    public void removeAdministeredCourse(String courseId) {
        administeredCoursesIds.remove(courseId);
    }

    private static final int COURSE_NAME_INDEX = 0;
    public Set<String> administeredCoursesNames() {
        return administeredCoursesIds.stream()
                .map(id -> id.split(FILE_SEPARATOR)[COURSE_NAME_INDEX])
                .collect(Collectors.toUnmodifiableSet());
    }

    public String getAdminCourseIdFromName(String courseName) {
        return administeredCoursesIds.stream()
                .filter(id -> id.split(FILE_SEPARATOR)[COURSE_NAME_INDEX].equals(courseName))
                .findFirst()
                .orElse(null);
    }

    public String getEnrollCourseIdFromName(String courseName) {
        return enrolledCoursesIds.stream()
                .filter(id -> id.split(FILE_SEPARATOR)[COURSE_NAME_INDEX].equals(courseName))
                .findFirst()
                .orElse(null);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(username, user.username);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(username);
    }
}

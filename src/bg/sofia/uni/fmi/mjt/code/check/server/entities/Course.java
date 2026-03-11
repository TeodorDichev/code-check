package bg.sofia.uni.fmi.mjt.code.check.server.entities;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class Course {
    private static final String FILE_SEPARATOR = "_";

    private final String id;
    private final String name;
    private final String joinString;
    private final Set<String> assignmentIds;
    private final Set<String> adminUsernames;
    private final Set<String> studentUsernames;

    public Course(String id, String name, String joinString,
                  Set<String> assignmentIds, Set<String> adminUsernames, Set<String> studentUsernames) {
        this.id = id;
        this.name = name;
        this.joinString = joinString;

        this.assignmentIds = ConcurrentHashMap.newKeySet();
        this.assignmentIds.addAll(assignmentIds);

        this.adminUsernames = ConcurrentHashMap.newKeySet();
        this.adminUsernames.addAll(adminUsernames);

        this.studentUsernames = ConcurrentHashMap.newKeySet();
        this.studentUsernames.addAll(studentUsernames);
    }

    private static final String ID_FORMAT = "%s_%s_%s";
    private static final DateTimeFormatter FILE_DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd-MM-yyyy-HH-mm-ss");
    public static String createId(String courseName, String creatorUsername) {
        return String.format(ID_FORMAT, courseName, creatorUsername, LocalDateTime.now().format(FILE_DATE_FORMAT));
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String joinString() {
        return joinString;
    }

    public Set<String> assignmentIds() {
        return Collections.unmodifiableSet(assignmentIds);
    }

    private static final int COURSE_NAME_INDEX = 0;
    public Set<String> assignmentNames() {
        return assignmentIds.stream()
                .map(id -> id.split(FILE_SEPARATOR)[COURSE_NAME_INDEX])
                .collect(Collectors.toUnmodifiableSet());
    }

    public String getAssignmentIdByName(String assignmentName) {
        return assignmentIds.stream()
                .filter(a -> a.split(FILE_SEPARATOR)[COURSE_NAME_INDEX].equals(assignmentName))
                .findFirst()
                .orElse(null);
    }

    public Set<String> adminUsernames() {
        return Collections.unmodifiableSet(adminUsernames);
    }

    public Set<String> studentUsernames() {
        return Collections.unmodifiableSet(studentUsernames);
    }

    public void addStudent(String studentUsername) {
        studentUsernames.add(studentUsername);
    }

    public void removeStudent(String studentUsername) {
        studentUsernames.remove(studentUsername);
    }

    public void addAdmin(String adminUsername) {
        adminUsernames.add(adminUsername);
    }

    public void addAssignment(String id) {
        assignmentIds.add(id);
    }

    public void removeAssignment(String id) {
        assignmentIds.remove(id);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Course course = (Course) o;
        return Objects.equals(id, course.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
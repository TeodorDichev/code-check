package bg.sofia.uni.fmi.mjt.code.check.server.entities;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class Assignment {
    private static final DateTimeFormatter FILE_DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd-MM-yyyy-HH-mm-ss");

    private final String id;
    private final String courseId;
    private final String name;
    private final String description;
    private final String assignedOn;
    private final String deadline;
    private final Map<String, String> userToSubmissionId;

    public Assignment(String id, String courseId, String name, String description,
                      LocalDateTime assignedOn, LocalDateTime deadline) {
        this.id = id;
        this.courseId = courseId;
        this.name = name;
        this.description = description;
        this.assignedOn = assignedOn.format(FILE_DATE_FORMAT);
        this.deadline = deadline.format(FILE_DATE_FORMAT);
        this.userToSubmissionId = new ConcurrentHashMap<>();
    }

    private static final String ID_FORMAT = "%s_%s_%s_%s";
    public static String createId(String assignmentName, String courseName, String username) {
        return String.format(ID_FORMAT, assignmentName, courseName, username,
                LocalDateTime.now().format(FILE_DATE_FORMAT));
    }

    public String id() {
        return id;
    }

    public String courseId() {
        return courseId;
    }

    public String name() {
        return name;
    }

    public String assignedOn() {
        return assignedOn;
    }

    public String deadline() {
        return deadline;
    }

    public Map<String, String> userToSubmissionId() {
        return Collections.unmodifiableMap(userToSubmissionId);
    }

    public String description() {
        return description;
    }

    public void removeSubmission(String username) {
        userToSubmissionId.remove(username);
    }

    public void addSubmission(String studentUsername, String subId) {
        userToSubmissionId.put(studentUsername, subId);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Assignment that = (Assignment) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
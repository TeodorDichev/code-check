package bg.sofia.uni.fmi.mjt.code.check.server.entities;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class Submission {
    private static final DateTimeFormatter FILE_DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd-MM-yyyy-HH-mm-ss");

    private final String id;
    private final String path;
    private final String assignmentId;
    private final String submittedBy;
    private final String submittedOn;

    private String gradedBy;
    private Double grade;
    private String comment;
    private final Map<String, String> fileComments;

    public Submission(String id, String path, String assignmentId, String submittedBy, LocalDateTime submittedOn) {
        this.id = id;
        this.path = path;
        this.assignmentId = assignmentId;
        this.submittedBy = submittedBy;
        this.submittedOn = submittedOn.format(FILE_DATE_FORMAT);
        this.fileComments = new ConcurrentHashMap<>();
    }

    public String id() {
        return id;
    }

    public String path() {
        return path;
    }

    public String assignmentId() {
        return assignmentId;
    }

    public String submittedBy() {
        return submittedBy;
    }

    public String submittedOn() {
        return submittedOn;
    }

    public String gradedBy() {
        return gradedBy;
    }

    public Double grade() {
        return grade;
    }

    public String comment() {
        return comment;
    }

    public Map<String, String> fileComments() {
        return Collections.unmodifiableMap(fileComments);
    }

    private static final String ID_FORMAT = "%s_%s_%s_%s";
    public static String createId(String courseName, String assignmentName, String studentUsername) {
        return String.format(ID_FORMAT, courseName, assignmentName,
                studentUsername, LocalDateTime.now().format(FILE_DATE_FORMAT));
    }

    public void grade(String gradedBy, double grade, String comment) {
        this.gradedBy = gradedBy;
        this.grade = grade;
        this.comment = comment;
    }

    public void addFileComment(String fileName, String fileComment) {
        this.fileComments.put(fileName, fileComment);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Submission that = (Submission) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
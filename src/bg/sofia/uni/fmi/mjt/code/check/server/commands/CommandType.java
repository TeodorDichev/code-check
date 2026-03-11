package bg.sofia.uni.fmi.mjt.code.check.server.commands;

public enum CommandType {
    REGISTER("register"),
    LOGIN("login"),
    LOGOUT("logout"),
    HELP("help"),
    CREATE_COURSE("create-course"),
    LIST_COURSES("list-courses"),
    JOIN_COURSE("join-course"),
    LIST_ASSIGNMENTS("list-assignments"),
    VIEW_ASSIGNMENT("view-assignment"),
    SUBMIT_ASSIGNMENT("submit-assignment"),
    POST_SUBMIT_ASSIGNMENT("post-submit-assignment"),
    VIEW_MY_SUBMISSION("view-my-submission"),
    VIEW_STUDENT_SUBMISSION("view-student-submission"),
    UPGRADE_PARTICIPANT("upgrade-participant"),
    LIST_COURSE_PARTICIPANTS("list-course-participants"),
    CREATE_ASSIGNMENT("create-assignment"),
    VIEW_SUBMISSIONS("view-submissions"),
    VIEW_SUBMISSION_FILE("view-submission-file"),
    TEST_SUBMISSION("test-submission"),
    TEST_SUBMISSIONS("test-submissions"),
    GRADE_SUBMISSION("grade-submission"),
    COMMENT_SUBMISSION_FILE("comment-submission-file"),
    UNKNOWN("unknown");

    private final String name;

    CommandType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
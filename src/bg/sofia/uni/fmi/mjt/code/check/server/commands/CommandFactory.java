package bg.sofia.uni.fmi.mjt.code.check.server.commands;

import bg.sofia.uni.fmi.mjt.code.check.server.commands.assignment.PostSubmitAssignmentCommand;
import bg.sofia.uni.fmi.mjt.code.check.server.commands.course.ListCoursesCommand;
import bg.sofia.uni.fmi.mjt.code.check.server.commands.submission.CommentSubmissionFileCommand;
import bg.sofia.uni.fmi.mjt.code.check.server.commands.assignment.CreateAssignmentCommand;
import bg.sofia.uni.fmi.mjt.code.check.server.commands.course.CreateCourseCommand;
import bg.sofia.uni.fmi.mjt.code.check.server.commands.submission.GradeSubmissionCommand;
import bg.sofia.uni.fmi.mjt.code.check.server.commands.common.HelpCommand;
import bg.sofia.uni.fmi.mjt.code.check.server.commands.course.JoinCourseCommand;
import bg.sofia.uni.fmi.mjt.code.check.server.commands.assignment.ListAssignmentsCommand;
import bg.sofia.uni.fmi.mjt.code.check.server.commands.course.ListCourseParticipantsCommand;
import bg.sofia.uni.fmi.mjt.code.check.server.commands.account.LoginCommand;
import bg.sofia.uni.fmi.mjt.code.check.server.commands.account.LogoutCommand;
import bg.sofia.uni.fmi.mjt.code.check.server.commands.account.RegisterCommand;
import bg.sofia.uni.fmi.mjt.code.check.server.commands.assignment.SubmitAssignmentCommand;
import bg.sofia.uni.fmi.mjt.code.check.server.commands.submission.TestSubmissionCommand;
import bg.sofia.uni.fmi.mjt.code.check.server.commands.submission.TestSubmissionsCommand;
import bg.sofia.uni.fmi.mjt.code.check.server.commands.common.UnknownCommand;
import bg.sofia.uni.fmi.mjt.code.check.server.commands.course.UpgradeParticipantCommand;
import bg.sofia.uni.fmi.mjt.code.check.server.commands.assignment.ViewAssignmentCommand;
import bg.sofia.uni.fmi.mjt.code.check.server.commands.submission.ViewMySubmissionCommand;
import bg.sofia.uni.fmi.mjt.code.check.server.commands.submission.ViewStudentSubmissions;
import bg.sofia.uni.fmi.mjt.code.check.server.commands.submission.ViewSubmissionFileCommand;
import bg.sofia.uni.fmi.mjt.code.check.server.commands.submission.ViewSubmissionsCommand;

import java.util.Arrays;
import java.util.List;

import static bg.sofia.uni.fmi.mjt.code.check.server.commands.CommandParser.getCommandArgs;
import static bg.sofia.uni.fmi.mjt.code.check.server.commands.CommandParser.getCommandString;

public class CommandFactory {

    public static Command of(String input) {
        List<String> args = getCommandArgs(input);
        return switch (getCommandType(getCommandString(input))) {
            case REGISTER -> new RegisterCommand(args);
            case LOGIN -> new LoginCommand(args);
            case LOGOUT -> new LogoutCommand(args);
            case HELP -> new HelpCommand(args);
            case CREATE_COURSE -> new CreateCourseCommand(args);
            case JOIN_COURSE -> new JoinCourseCommand(args);
            case LIST_COURSES -> new ListCoursesCommand(args);
            case LIST_COURSE_PARTICIPANTS -> new ListCourseParticipantsCommand(args);
            case UPGRADE_PARTICIPANT -> new UpgradeParticipantCommand(args);
            case CREATE_ASSIGNMENT -> new CreateAssignmentCommand(args);
            case LIST_ASSIGNMENTS -> new ListAssignmentsCommand(args);
            case VIEW_ASSIGNMENT -> new ViewAssignmentCommand(args);
            case SUBMIT_ASSIGNMENT -> new SubmitAssignmentCommand(args);
            case POST_SUBMIT_ASSIGNMENT -> new PostSubmitAssignmentCommand(args);
            case VIEW_MY_SUBMISSION -> new ViewMySubmissionCommand(args);
            case VIEW_STUDENT_SUBMISSION -> new ViewStudentSubmissions(args);
            case VIEW_SUBMISSIONS -> new ViewSubmissionsCommand(args);
            case GRADE_SUBMISSION -> new GradeSubmissionCommand(args);
            case COMMENT_SUBMISSION_FILE -> new CommentSubmissionFileCommand(args);
            case TEST_SUBMISSION -> new TestSubmissionCommand(args);
            case TEST_SUBMISSIONS -> new TestSubmissionsCommand(args);
            case VIEW_SUBMISSION_FILE -> new ViewSubmissionFileCommand(args);
            default -> new UnknownCommand();
        };
    }

    private static CommandType getCommandType(String command) {
        return Arrays.stream(CommandType.values())
                .filter(type -> command.equalsIgnoreCase(type.getName()))
                .findFirst()
                .orElse(CommandType.UNKNOWN);
    }
}
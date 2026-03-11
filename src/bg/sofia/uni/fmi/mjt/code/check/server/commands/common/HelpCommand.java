package bg.sofia.uni.fmi.mjt.code.check.server.commands.common;

import bg.sofia.uni.fmi.mjt.code.check.server.commands.CommandBase;
import bg.sofia.uni.fmi.mjt.code.check.server.commands.CommandType;
import bg.sofia.uni.fmi.mjt.code.check.server.entities.User;
import bg.sofia.uni.fmi.mjt.code.check.contract.ServerResponse;
import bg.sofia.uni.fmi.mjt.code.check.contract.Status;

import java.util.List;
import java.util.Map;

public class HelpCommand extends CommandBase {
    private static final int EXPECTED_PARAMS_COUNT = 0;

    // The whole this is a const don't take points please
    private static final Map<String, List<String>> HELP_DATA = Map.of(
            "Общи", List.of(
                    "register <user> <pass>",
                    "login <user> <pass>",
                    "logout",
                    "help",
                    "exit",
                    "create-course <name>",
                    "list-created-courses",
                    "list-joined-courses",
                    "join-course <name> <code hex>",
                    "list-assignments <name>",
                    "view-assignment <c-name> <a-name>"
            ),
            "За студенти", List.of(
                    "submit-assignment <c-name> <a-name> <path>",
                    "view-my-submission <c-name> <a-name>"
            ),
            "За администратори", List.of(
                    "upgrade-participant <c-name> <user>",
                    "list-course-participants <c-name>",
                    "create-assignment <c-name> <a-name> <description> <deadline>",
                    "view-submissions <c-name> <a-name>",
                    "view-student-submission <c-name> <a-name> <user>",
                    "view-submission-file <c-name> <a-name> <user> <file>",
                    "grade-submission <c-name> <a-name> <user> <grade> <comment>",
                    "test-submission <c-name> <a-name> <user>",
                    "test-submissions <c-name> <a-name>",
                    "comment-submission-file <c-name> <a-name> <user> <file> <comment>"
            ),
            "Бележки", List.of(
                    "Курсовете трябва да са с уникални имена за създателя.",
                    "Задачите трябва да са с уникални имена в рамките на курса."
            )
    );

    private final List<String> args;

    public HelpCommand(List<String> args) {
        this.args = args;
    }

    @Override
    public ServerResponse execute() {
        try {
            context.validationService().validateListParamsCount(args, EXPECTED_PARAMS_COUNT);

            User user = sessionStore.getUser(session.key());
            return new ServerResponse(CommandType.HELP, Status.OK, "Help command", user, HELP_DATA);
        } catch (Exception e) {
            logger.logError("Exception occurred in help command", e);
            return new ServerResponse(CommandType.HELP, Status.ERROR, e.getMessage(), null, null);
        }
    }

    @Override
    public CommandType getType() {
        return CommandType.HELP;
    }
}
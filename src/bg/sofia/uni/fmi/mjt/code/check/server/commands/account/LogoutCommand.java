package bg.sofia.uni.fmi.mjt.code.check.server.commands.account;

import bg.sofia.uni.fmi.mjt.code.check.server.commands.AuthenticatedCommand;
import bg.sofia.uni.fmi.mjt.code.check.server.commands.CommandType;
import bg.sofia.uni.fmi.mjt.code.check.contract.ServerResponse;
import bg.sofia.uni.fmi.mjt.code.check.contract.Status;

import java.util.List;

public class LogoutCommand extends AuthenticatedCommand {
    private static final int EXPECTED_PARAMS_COUNT = 0;
    private final List<String> args;

    public LogoutCommand(List<String> args) {
        this.args = args;
    }

    @Override
    protected ServerResponse authenticatedExecute() {
        try {
            context.validationService().validateListParamsCount(args, EXPECTED_PARAMS_COUNT);
            sessionStore.remove(session.key());
            return new ServerResponse(CommandType.LOGOUT, Status.OK,
                    "User successfully logged out", null, null);
        } catch (Exception e) {
            logger.logError("Exception occurred while user was trying to log out", e);
            return new ServerResponse(CommandType.LOGOUT, Status.ERROR, e.getMessage(), user, null);
        }
    }

    @Override
    public CommandType getType() {
        return CommandType.LOGOUT;
    }
}

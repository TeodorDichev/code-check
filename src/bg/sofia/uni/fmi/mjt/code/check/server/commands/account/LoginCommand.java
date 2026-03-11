package bg.sofia.uni.fmi.mjt.code.check.server.commands.account;

import bg.sofia.uni.fmi.mjt.code.check.server.commands.CommandBase;
import bg.sofia.uni.fmi.mjt.code.check.server.commands.CommandType;
import bg.sofia.uni.fmi.mjt.code.check.server.entities.User;
import bg.sofia.uni.fmi.mjt.code.check.contract.ServerResponse;
import bg.sofia.uni.fmi.mjt.code.check.contract.Status;
import bg.sofia.uni.fmi.mjt.code.check.server.services.hash.algorithm.Sha256Algorithm;
import bg.sofia.uni.fmi.mjt.code.check.server.session.Session;

import java.util.List;

public class LoginCommand extends CommandBase {
    private static final int EXPECTED_PARAMS_COUNT = 2;
    private static final int USERNAME_INDEX = 0;
    private static final int PASSWORD_INDEX = 1;

    private final List<String> args;

    public LoginCommand(List<String> args) {
        this.args = args;
    }

    @Override
    public ServerResponse execute() {
        try {
            context.validationService().validateListParamsCount(args, EXPECTED_PARAMS_COUNT);
            String username = args.get(USERNAME_INDEX);
            String passwordHash = args.get(PASSWORD_INDEX);

            if (sessionStore.hasSession(session.key())) {
                return new ServerResponse(CommandType.LOGIN, Status.ERROR,
                        "User already logged in.", session.user(), null);
            }

            User userByUsername = context.userRepository().get(username);
            if (userByUsername == null || !context.passwordService().checkPassword(
                    passwordHash, userByUsername.passwordHash(), new Sha256Algorithm())) {

                return new ServerResponse(CommandType.LOGIN, Status.ERROR,
                        "Invalid user credentials", null, null);
            }

            sessionStore.register(new Session(session.key(), userByUsername));
            return new ServerResponse(CommandType.LOGIN, Status.OK,
                    "User successfully logged in", userByUsername, null);
        } catch (Exception e) {
            logger.logError("Exception occurred while user was trying to log in", e);
            return new ServerResponse(CommandType.LOGIN, Status.ERROR, e.getMessage(), null, null);
        }
    }

    @Override
    public CommandType getType() {
        return CommandType.LOGIN;
    }
}
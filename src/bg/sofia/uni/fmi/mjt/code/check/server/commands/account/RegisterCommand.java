package bg.sofia.uni.fmi.mjt.code.check.server.commands.account;

import bg.sofia.uni.fmi.mjt.code.check.server.commands.CommandBase;
import bg.sofia.uni.fmi.mjt.code.check.server.commands.CommandType;
import bg.sofia.uni.fmi.mjt.code.check.server.entities.User;
import bg.sofia.uni.fmi.mjt.code.check.contract.ServerResponse;
import bg.sofia.uni.fmi.mjt.code.check.contract.Status;
import bg.sofia.uni.fmi.mjt.code.check.server.services.LockRegistry;
import bg.sofia.uni.fmi.mjt.code.check.server.services.hash.algorithm.Sha256Algorithm;
import bg.sofia.uni.fmi.mjt.code.check.server.session.Session;

import java.util.List;

public class RegisterCommand extends CommandBase {
    private static final int EXPECTED_PARAMS_COUNT = 2;
    private static final int USERNAME_INDEX = 0;
    private static final int PASSWORD_INDEX = 1;
    private final List<String> args;

    public RegisterCommand(List<String> args) {
        this.args = args;
    }

    @Override
    public ServerResponse execute() {
        try {
            context.validationService().validateListParamsCount(args, EXPECTED_PARAMS_COUNT);
            String username = args.get(USERNAME_INDEX);
            String rawPassword = args.get(PASSWORD_INDEX);
            if (sessionStore.hasSession(session.key())) {
                return new ServerResponse(CommandType.REGISTER, Status.ERROR,
                        "User already logged in.", session.user(), null);
            }

            String passwordHash = context.passwordService().hashPassword(rawPassword, new Sha256Algorithm());
            User newUser = new User(username, passwordHash);

            Object lock = LockRegistry.getLock(username);
            try {
                synchronized (lock) {
                    context.userRepository().add(newUser);
                }
            } finally {
                LockRegistry.cleanup(username, lock);
            }

            sessionStore.register(new Session(session.key(), newUser));
            return new ServerResponse(CommandType.REGISTER, Status.OK,
                    "User successfully registered", newUser, null);
        } catch (Exception e) {
            logger.logError("Exception occurred while user was trying to register", e);
            return new ServerResponse(CommandType.REGISTER, Status.ERROR, e.getMessage(), null, null);
        }
    }

    @Override
    public CommandType getType() {
        return CommandType.REGISTER;
    }
}

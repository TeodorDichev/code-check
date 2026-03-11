package bg.sofia.uni.fmi.mjt.code.check.server.commands;

import bg.sofia.uni.fmi.mjt.code.check.server.entities.User;
import bg.sofia.uni.fmi.mjt.code.check.contract.ServerResponse;
import bg.sofia.uni.fmi.mjt.code.check.contract.Status;

public abstract class AuthenticatedCommand extends CommandBase {

    protected User user;

    protected abstract ServerResponse authenticatedExecute();

    @Override
    public final ServerResponse execute() {
        if (!sessionStore.hasSession(session.key())) {
            logger.logInfo("User tried to execute authentication required command. (authExec)");

            return new ServerResponse(
                    CommandType.UNKNOWN,
                    Status.ERROR,
                    "You must log in in order to execute such commands.",
                    user,
                    null);
        }

        user = sessionStore.getUser(session.key());
        return authenticatedExecute();
    }
}
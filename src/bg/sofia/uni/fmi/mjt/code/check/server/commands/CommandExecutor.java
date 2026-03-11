package bg.sofia.uni.fmi.mjt.code.check.server.commands;

import bg.sofia.uni.fmi.mjt.code.check.contract.ServerResponse;
import bg.sofia.uni.fmi.mjt.code.check.contract.Status;
import bg.sofia.uni.fmi.mjt.code.check.server.repositories.CodeCheckContext;
import bg.sofia.uni.fmi.mjt.code.check.server.services.logging.Logger;
import bg.sofia.uni.fmi.mjt.code.check.server.session.Session;
import bg.sofia.uni.fmi.mjt.code.check.server.session.SessionStore;

public class CommandExecutor {
    private final SessionStore sessionStore;
    private final CodeCheckContext context;
    private final Logger logger;

    public static CommandExecutor configure(SessionStore sessionStore, CodeCheckContext context, Logger logger) {
        return new CommandExecutor(sessionStore, context, logger);
    }

    private CommandExecutor(SessionStore sessionStore, CodeCheckContext context, Logger logger) {
        this.sessionStore = sessionStore;
        this.context = context;
        this.logger = logger;
    }

    public ServerResponse execute(String input, Session session) {
        Command command = CommandFactory.of(input)
                .addDependencies(sessionStore, context)
                .addSessionContext(session)
                .addLogger(logger);
        try {
            return command.execute();
        } catch (Exception ex) {
            return new ServerResponse(command.getType(), Status.ERROR, ex.getMessage(),
                    sessionStore.getUser(session.key()), null);
        }
    }
}

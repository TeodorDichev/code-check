package bg.sofia.uni.fmi.mjt.code.check.server.commands;

import bg.sofia.uni.fmi.mjt.code.check.server.repositories.CodeCheckContext;
import bg.sofia.uni.fmi.mjt.code.check.server.services.logging.Logger;
import bg.sofia.uni.fmi.mjt.code.check.server.session.Session;
import bg.sofia.uni.fmi.mjt.code.check.server.session.SessionStore;

public abstract class CommandBase implements Command {
    protected SessionStore sessionStore;
    protected CodeCheckContext context;
    protected Session session;
    protected Logger logger;

    protected CommandBase() {
        sessionStore = null;
        context = null;
    }

    @Override
    public final Command addDependencies(SessionStore sessionStore, CodeCheckContext context) {
        this.context = context;
        this.sessionStore = sessionStore;
        return this;
    }

    @Override
    public final Command addSessionContext(Session session) {
        this.session = session;
        return this;
    }

    @Override
    public final Command addLogger(Logger logger) {
        this.logger = logger;
        return this;
    }
}

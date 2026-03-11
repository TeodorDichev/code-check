package bg.sofia.uni.fmi.mjt.code.check.server.commands;

import bg.sofia.uni.fmi.mjt.code.check.contract.ServerResponse;
import bg.sofia.uni.fmi.mjt.code.check.server.repositories.CodeCheckContext;
import bg.sofia.uni.fmi.mjt.code.check.server.services.logging.Logger;
import bg.sofia.uni.fmi.mjt.code.check.server.session.Session;
import bg.sofia.uni.fmi.mjt.code.check.server.session.SessionStore;

public interface Command {
    ServerResponse execute();

    Command addDependencies(SessionStore sessionStore, CodeCheckContext context);

    Command addSessionContext(Session session);

    Command addLogger(Logger logger);

    CommandType getType();
}

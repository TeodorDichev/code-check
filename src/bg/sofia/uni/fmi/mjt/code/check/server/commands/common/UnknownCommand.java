package bg.sofia.uni.fmi.mjt.code.check.server.commands.common;

import bg.sofia.uni.fmi.mjt.code.check.server.commands.CommandBase;
import bg.sofia.uni.fmi.mjt.code.check.server.commands.CommandType;
import bg.sofia.uni.fmi.mjt.code.check.server.entities.User;
import bg.sofia.uni.fmi.mjt.code.check.contract.ServerResponse;
import bg.sofia.uni.fmi.mjt.code.check.contract.Status;

public class UnknownCommand extends CommandBase {
    public UnknownCommand() {
    }

    @Override
    public ServerResponse execute() {
        try {
            User user = sessionStore.getUser(session.key());
            return new ServerResponse(CommandType.UNKNOWN, Status.ERROR, "Unknown command", user, null);
        } catch (Exception e) {
            logger.logError("Exception occurred in unknown command", e);
            return new ServerResponse(CommandType.UNKNOWN, Status.ERROR,
                    "Unknown command error: " + e.getMessage(), null, null);
        }
    }

    @Override
    public CommandType getType() {
        return CommandType.UNKNOWN;
    }
}
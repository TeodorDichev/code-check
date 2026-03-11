package bg.sofia.uni.fmi.mjt.code.check.contract;

import bg.sofia.uni.fmi.mjt.code.check.server.commands.CommandType;
import bg.sofia.uni.fmi.mjt.code.check.server.entities.User;

public record ServerResponse(
        CommandType command,
        Status status,
        String message,
        User user,
        Object data
) {
    public ServerResponse() {
        this(CommandType.UNKNOWN, Status.ERROR, "Unprocessed response", null, null);
    }
}
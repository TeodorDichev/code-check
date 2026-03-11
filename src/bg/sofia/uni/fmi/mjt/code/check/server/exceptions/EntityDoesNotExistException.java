package bg.sofia.uni.fmi.mjt.code.check.server.exceptions;

public class EntityDoesNotExistException extends RuntimeException {
    public EntityDoesNotExistException(String message) {
        super(message);
    }

    public EntityDoesNotExistException(String message, Throwable cause) {
        super(message, cause);
    }
}

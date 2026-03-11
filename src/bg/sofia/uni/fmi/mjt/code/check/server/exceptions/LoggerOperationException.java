package bg.sofia.uni.fmi.mjt.code.check.server.exceptions;

public class LoggerOperationException extends RuntimeException {
    public LoggerOperationException(String message) {
        super(message);
    }

    public LoggerOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}

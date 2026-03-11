package bg.sofia.uni.fmi.mjt.code.check.server.services.logging;

public interface Logger {
    void logInfo(String message);

    void logWarning(String message);

    void logError(String message, Exception e);

    void logError(String message);

    void logException(Exception e);

    void log(Severity severity, String message);
}
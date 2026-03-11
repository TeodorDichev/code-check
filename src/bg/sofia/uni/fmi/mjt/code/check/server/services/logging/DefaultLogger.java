package bg.sofia.uni.fmi.mjt.code.check.server.services.logging;

import bg.sofia.uni.fmi.mjt.code.check.server.exceptions.LoggerOperationException;
import bg.sofia.uni.fmi.mjt.code.check.server.services.logging.providers.ConsoleProvider;
import bg.sofia.uni.fmi.mjt.code.check.server.services.logging.providers.DefaultConsoleProvider;
import bg.sofia.uni.fmi.mjt.code.check.server.services.logging.providers.DefaultFileProvider;
import bg.sofia.uni.fmi.mjt.code.check.server.services.logging.providers.FileProvider;
import bg.sofia.uni.fmi.mjt.code.check.server.utils.Nullable;

import java.time.LocalDateTime;

public class DefaultLogger implements Logger {

    private final Severity consoleSeverity;
    private final Severity fileSeverity;

    private final ConsoleProvider consoleProvider;
    private final FileProvider fileProvider;

    private DefaultLogger(CodeCheckLoggerBuilder builder) {
        this.consoleSeverity = builder.consoleSeverity;
        this.fileSeverity = builder.fileSeverity;

        this.consoleProvider = Nullable.orDefault(builder.consoleProvider, new DefaultConsoleProvider());
        this.fileProvider = Nullable.orDefault(builder.fileProvider, new DefaultFileProvider());
    }

    public static CodeCheckLoggerBuilder configure() {
        return new CodeCheckLoggerBuilder();
    }

    public static DefaultLogger getDefaultLogger() {
        return new CodeCheckLoggerBuilder()
                .addConsoleLogging(Severity.INFO)
                .addFileLogging(Severity.INFO).build();
    }

    @Override
    public void logInfo(String message) {
        log(Severity.INFO, message);
    }

    @Override
    public void logWarning(String message) {
        log(Severity.WARN, message);
    }

    @Override
    public void logError(String message, Exception e) {
        logError((message));
        logException(e);
    }

    @Override
    public void logError(String message) {
        log(Severity.ERROR, message);
    }

    @Override
    public void logException(Exception e) {
        try {
            fileProvider.writeError(e);
        } catch (LoggerOperationException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void log(Severity severity, String message) {
        LocalDateTime now = LocalDateTime.now();

        if (fileSeverity.getValue() <= severity.getValue()) {
            logFile(severity, now, message);
        }

        if (consoleSeverity.getValue() <= severity.getValue()) {
            logConsole(severity, now, message);
        }
    }

    private String getMessage(Severity severity, LocalDateTime dateTime, String message) {
        return ("[" + severity.name() + "] [" + dateTime + "] " + message).trim() + System.lineSeparator();
    }

    private void logFile(Severity severity, LocalDateTime dateTime, String message) {
        try {
            fileProvider.write(getMessage(severity, dateTime, message));
        } catch (LoggerOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private void logConsole(Severity severity, LocalDateTime dateTime, String message) {
        try {
            consoleProvider.write(getMessage(severity, dateTime, message));
        } catch (LoggerOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public static class CodeCheckLoggerBuilder {

        private Severity consoleSeverity = Severity.NONE;
        private Severity fileSeverity = Severity.NONE;
        private ConsoleProvider consoleProvider;
        private FileProvider fileProvider;

        private CodeCheckLoggerBuilder() {
        }

        public CodeCheckLoggerBuilder addConsoleLogging(Severity severity) {
            consoleSeverity = severity;
            return this;
        }

        public CodeCheckLoggerBuilder addFileLogging(Severity severity) {
            fileSeverity = severity;
            return this;
        }

        public CodeCheckLoggerBuilder addConsoleLogging(Severity severity, ConsoleProvider provider) {
            consoleProvider = provider;
            return addConsoleLogging(severity);
        }

        public CodeCheckLoggerBuilder addFileLogging(Severity severity, FileProvider provider) {
            fileProvider = provider;
            return addFileLogging(severity);
        }

        public CodeCheckLoggerBuilder configureConsoleProvider(ConsoleProvider provider) {
            consoleProvider = provider;
            return this;
        }

        public CodeCheckLoggerBuilder configureFileProvider(FileProvider provider) {
            fileProvider = provider;
            return this;
        }

        public DefaultLogger build() {
            return new DefaultLogger(this);
        }
    }
}
package bg.sofia.uni.fmi.mjt.code.check.server.services.logging;

import bg.sofia.uni.fmi.mjt.code.check.server.exceptions.LoggerOperationException;
import bg.sofia.uni.fmi.mjt.code.check.server.services.logging.providers.ConsoleProvider;
import bg.sofia.uni.fmi.mjt.code.check.server.services.logging.providers.FileProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DefaultLoggerTest {

    @Mock private ConsoleProvider consoleProvider;
    @Mock private FileProvider fileProvider;

    private DefaultLogger.CodeCheckLoggerBuilder builder;

    @BeforeEach
    void setUp() {
        builder = DefaultLogger.configure();
    }

    @Test
    void testLogInfoGoesToFileButNotConsoleBasedOnThresholds() throws LoggerOperationException {
        DefaultLogger logger = builder
                .addConsoleLogging(Severity.ERROR, consoleProvider)
                .addFileLogging(Severity.INFO, fileProvider)
                .build();

        String msg = "Info level message";
        logger.logInfo(msg);

        verify(fileProvider).write(anyString());
        verify(consoleProvider, never()).write(anyString());
    }

    @Test
    void testLogWithNoneSeverityThresholdLogsEverything() throws LoggerOperationException {
        DefaultLogger logger = builder
                .addConsoleLogging(Severity.NONE, consoleProvider)
                .addFileLogging(Severity.NONE, fileProvider)
                .build();

        logger.log(Severity.INFO, "Message for NONE threshold");

        verify(consoleProvider, times(1)).write(anyString());
        verify(fileProvider, times(1)).write(anyString());
    }

    @Test
    void testLogExceptionWrapsLoggerOperationExceptionInRuntimeException() throws LoggerOperationException {
        DefaultLogger logger = builder.addFileLogging(Severity.INFO, fileProvider).build();
        Exception testEx = new Exception("Test");

        doThrow(new LoggerOperationException("I/O Error")).when(fileProvider).writeError(testEx);

        assertThrows(RuntimeException.class, () -> logger.logException(testEx),
                "Should wrap checked LoggerOperationException into RuntimeException for consistency");
    }

    @Test
    void testLogFileThrowsRuntimeExceptionOnProviderFailure() throws LoggerOperationException {
        DefaultLogger logger = builder.addFileLogging(Severity.INFO, fileProvider).build();

        doThrow(new LoggerOperationException("Disk Full")).when(fileProvider).write(anyString());

        assertThrows(RuntimeException.class, () -> logger.logInfo("test"),
                "Providing failure during file writing should result in a RuntimeException");
    }

    @Test
    void testLogConsoleThrowsRuntimeExceptionOnProviderFailure() throws LoggerOperationException {
        DefaultLogger logger = builder.addConsoleLogging(Severity.INFO, consoleProvider).build();

        doThrow(new LoggerOperationException("Console locked")).when(consoleProvider).write(anyString());

        assertThrows(RuntimeException.class, () -> logger.logInfo("test"),
                "Providing failure during console writing should result in a RuntimeException");
    }

    @Test
    void testDefaultLoggerStaticFactoryReturnsValidInstance() {
        DefaultLogger defaultLogger = DefaultLogger.getDefaultLogger();

        assertInstanceOf(DefaultLogger.class, defaultLogger,
                "getDefaultLogger() must return a non-null instance of DefaultLogger");
    }

    @Test
    void testBuilderMethodsReturnCorrectBuilderInstances() {
        DefaultLogger.CodeCheckLoggerBuilder result = builder.addConsoleLogging(Severity.WARN)
                .configureConsoleProvider(consoleProvider);

        assertInstanceOf(DefaultLogger.CodeCheckLoggerBuilder.class, result,
                "Builder methods should return the builder instance for chaining");
    }

    @Test
    void testLogInfoDelegatesCorrectMessage() throws LoggerOperationException {
        DefaultLogger logger = builder
                .addFileLogging(Severity.NONE, fileProvider)
                .addConsoleLogging(Severity.NONE, consoleProvider)
                .build();
        String msg = "System started";
        logger.logInfo(msg);
        verify(fileProvider).write(contains(msg));
        verify(consoleProvider).write(contains(msg));
    }

    @Test
    void testLogInfoDelegatesCorrectSeverityTag() throws LoggerOperationException {
        DefaultLogger logger = builder
                .addFileLogging(Severity.NONE, fileProvider)
                .addConsoleLogging(Severity.NONE, consoleProvider)
                .build();
        logger.logInfo("Any info");
        verify(fileProvider).write(contains("[INFO]"));
        verify(consoleProvider).write(contains("Any info"));
    }

    @Test
    void testLogErrorDelegatesCorrectMessage() throws LoggerOperationException {
        DefaultLogger logger = builder
                .addFileLogging(Severity.NONE, fileProvider)
                .addConsoleLogging(Severity.NONE, consoleProvider)
                .build();
        String msg = "Database connection failed";
        logger.logError(msg);
        verify(fileProvider).write(contains(msg));
        verify(consoleProvider).write(contains(msg));
    }

    @Test
    void testLogErrorDelegatesCorrectSeverityTag() throws LoggerOperationException {
        DefaultLogger logger = builder
                .addFileLogging(Severity.NONE, fileProvider)
                .addConsoleLogging(Severity.NONE, consoleProvider)
                .build();
        logger.logError("Any error");
        verify(fileProvider).write(contains("[ERROR]"));
        verify(consoleProvider).write(contains("Any error"));
    }

    @Test
    void testLogWarningDelegatesCorrectMessage() throws LoggerOperationException {
        DefaultLogger logger = builder
                .addFileLogging(Severity.NONE, fileProvider)
                .addConsoleLogging(Severity.NONE, consoleProvider)
                .build();
        String msg = "Careful!";
        logger.logWarning(msg);
        verify(fileProvider).write(contains(msg));
        verify(consoleProvider).write(contains(msg));
    }

    @Test
    void testLogWarningDelegatesCorrectSeverityTag() throws LoggerOperationException {
        DefaultLogger logger = builder
                .addFileLogging(Severity.NONE, fileProvider)
                .addConsoleLogging(Severity.NONE, consoleProvider)
                .build();
        logger.logWarning("Any message");
        verify(fileProvider).write(contains("[WARN]"));
        verify(consoleProvider).write(contains("Any message"));
    }

    @Test
    void testLogErrorWithExceptionLogsMessageToProviders() throws LoggerOperationException {
        DefaultLogger logger = builder
                .addConsoleLogging(Severity.ERROR, consoleProvider)
                .addFileLogging(Severity.ERROR, fileProvider)
                .build();

        logger.logError("Major failure", new Exception());

        verify(consoleProvider).write(contains("Major failure"));
        verify(fileProvider).write(contains("Major failure"));
    }

    @Test
    void testLogErrorWithExceptionLogsStacktraceToFile() throws LoggerOperationException {
        DefaultLogger logger = builder
                .addConsoleLogging(Severity.ERROR, consoleProvider)
                .addFileLogging(Severity.ERROR, fileProvider)
                .build();
        Exception testEx = new Exception("Root cause");
        logger.logError("Any message", testEx);
        verify(fileProvider).writeError(testEx);
    }
}
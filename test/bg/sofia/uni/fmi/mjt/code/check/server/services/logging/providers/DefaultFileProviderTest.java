package bg.sofia.uni.fmi.mjt.code.check.server.services.logging.providers;

import bg.sofia.uni.fmi.mjt.code.check.server.exceptions.LoggerOperationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultFileProviderTest {

    @TempDir
    Path tempDir;

    private DefaultFileProvider provider;
    private String logsPath;
    private String errorsPath;

    @BeforeEach
    void setUp() {
        logsPath = tempDir.resolve("logs").toString();
        errorsPath = tempDir.resolve("errors").toString();
        provider = new DefaultFileProvider(logsPath, errorsPath);
    }

    @Test
    void testWriteCreatesDirectoryAndFile() throws LoggerOperationException {
        String logMessage = "Log entry 1";
        provider.write(logMessage);

        Path expectedFile = Path.of(logsPath, LocalDate.now() + ".log");

        assertTrue(Files.exists(expectedFile), "The log file should be created automatically");
        assertFileContains(expectedFile, logMessage);
    }

    @Test
    void testWriteAppendsToExistingFile() throws LoggerOperationException {
        provider.write("First line");
        provider.write("Second line");

        Path expectedFile = Path.of(logsPath, LocalDate.now() + ".log");

        List<String> lines = getFileLines(expectedFile);
        assertTrue(lines.stream().anyMatch(l -> l.contains("First line")), "Should contain first entry");
        assertTrue(lines.stream().anyMatch(l -> l.contains("Second line")), "Should contain second entry");
    }

    @Test
    void testWriteErrorLogsToBothFiles() throws LoggerOperationException {
        Exception testEx = new RuntimeException("Crash!");
        provider.writeError(testEx);

        Path logFile = Path.of(logsPath, LocalDate.now() + ".log");
        Path errorFile = Path.of(errorsPath, LocalDate.now() + ".log");

        assertTrue(Files.exists(logFile), "Error should be logged to common logs");
        assertTrue(Files.exists(errorFile), "Error should be logged to error logs");

        assertFileContains(errorFile, "RuntimeException: Crash!");
        assertFileContains(errorFile, "at bg.sofia.uni.fmi.mjt.code.check");
    }

    // Helper methods to keep tests readable
    private void assertFileContains(Path path, String expected) {
        try {
            String content = Files.readString(path);
            assertTrue(content.contains(expected),
                    "File " + path + " should contain: [" + expected + "]");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private List<String> getFileLines(Path path) {
        try {
            return Files.readAllLines(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
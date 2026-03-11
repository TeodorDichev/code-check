package bg.sofia.uni.fmi.mjt.code.check.server.services.logging.providers;

import bg.sofia.uni.fmi.mjt.code.check.server.exceptions.LoggerOperationException;
import bg.sofia.uni.fmi.mjt.code.check.server.utils.Nullable;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class DefaultFileProvider implements FileProvider {
    private static final String DEFAULT_LOG_FILE_PATH = "./project/logs/common/";
    private static final String DEFAULT_ERROR_FILE_PATH = "./project/logs/errors/";
    private final String logFilePath;
    private final String errorFilePath;

    public DefaultFileProvider() {
        this(DEFAULT_LOG_FILE_PATH, DEFAULT_ERROR_FILE_PATH);
    }

    public DefaultFileProvider(String logFilePath, String errorFilePath) {
        this.logFilePath = Nullable.orDefault(logFilePath, DEFAULT_LOG_FILE_PATH);
        this.errorFilePath = Nullable.orDefault(errorFilePath, DEFAULT_ERROR_FILE_PATH);
    }

    @Override
    public void write(String str) {
        writeToFile(logFilePath, str);
    }

    @Override
    public void writeError(Exception e) {

        StringWriter writer = new StringWriter();
        writer.write("[" + LocalDateTime.now() + "] ");
        PrintWriter printer = new PrintWriter(writer);
        e.printStackTrace(printer);
        writer.write(System.lineSeparator());

        String log = writer.toString();
        writeToFile(errorFilePath, log);
        writeToFile(logFilePath, log);
    }

    private void writeToFile(String root, String str) {
        Path path = getPath(root);
        ensureCreated(path);

        try (Writer writer = Files.newBufferedWriter(
                path,
                StandardCharsets.UTF_8,
                StandardOpenOption.APPEND)) {
            writer.write(str);
            writer.flush();
        } catch (IOException e) {
            throw new LoggerOperationException(e.getMessage(), e);
        }
    }

    private void ensureCreated(Path path) {
        if (!Files.exists(path)) {
            try {
                Files.createDirectories(path.getParent().toAbsolutePath());
                Files.createFile(path.toAbsolutePath());
            } catch (IOException e) {
                throw new LoggerOperationException(e.getMessage(), e);
            }
        }
    }

    private Path getPath(String root) {
        return Path.of(root, LocalDate.now() + ".log");
    }
}
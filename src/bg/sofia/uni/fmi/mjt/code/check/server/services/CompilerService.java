package bg.sofia.uni.fmi.mjt.code.check.server.services;

import bg.sofia.uni.fmi.mjt.code.check.server.models.CompilationResult;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class CompilerService {
    private static final String ACCEPTABLE_FILE_EXTENSION = ".java";
    private static final int MAX_TIMEOUT = 10;

    private final ValidationService validationService;

    public CompilerService(ValidationService validationService) {
        this.validationService = validationService;
    }

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    public CompilationResult compileFiles(String submissionPath) {
        validationService.throwIfNullOrEmpty(submissionPath, "Path is null or empty");

        Future<CompilationResult> future = executor.submit(() -> {
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            List<String> files = collectJavaFiles(Path.of(submissionPath));

            if (files.isEmpty()) {
                return new CompilationResult(false, "No .java files found.");
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            int result = compiler.run(null, null, out, files.toArray(new String[0]));

            boolean isSuccess = result == 0;
            return new CompilationResult(isSuccess, isSuccess ? "Success" : out.toString(StandardCharsets.UTF_8));
        });

        try {
            return future.get(MAX_TIMEOUT, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            return new CompilationResult(false, "Timeout: Compilation took too long.");
        } catch (Exception e) {
            return new CompilationResult(false, "Error: " + e.getMessage());
        }
    }

    private List<String> collectJavaFiles(Path root) throws IOException {
        try (var stream = Files.walk(root)) {
            return stream.filter(Files::isRegularFile)
                    .map(Path::toString)
                    .filter(s -> s.endsWith(ACCEPTABLE_FILE_EXTENSION))
                    .toList();
        }
    }
}
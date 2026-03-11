package bg.sofia.uni.fmi.mjt.code.check.server.services;

import bg.sofia.uni.fmi.mjt.code.check.server.models.CompilationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CompilerServiceTest {

    @Mock
    private ValidationService validationService;

    private CompilerService compilerService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        compilerService = new CompilerService(validationService);
    }

    @Test
    void testCompileFilesDelegatesValidation() {
        compilerService.compileFiles(null);
        verify(validationService).throwIfNullOrEmpty(null, "Path is null or empty");
    }

    @Test
    void testCompileFilesNoJavaFilesFound() {
        CompilationResult result = compilerService.compileFiles(tempDir.toString());

        assertFalse(result.success());
        assertEquals("No .java files found.", result.message());
    }

    @Test
    void testCompileFilesWithInvalidPathTriggersCatchBlock() {
        // Use an illegal character to force a failure in Path.of or Files.walk
        CompilationResult result = compilerService.compileFiles("\0");

        assertFalse(result.success());
        assertTrue(result.message().startsWith("Error:"));
    }

    @Test
    void testCompileFilesWithJavaFilesExecutesProcess() throws IOException {
        // Create a dummy java file to trigger the ProcessBuilder branch
        Files.createFile(tempDir.resolve("Main.java"));

        CompilationResult result = compilerService.compileFiles(tempDir.toString());

        assertTrue(result.success());
    }

    @Test
    void testCollectJavaFilesHandlesEmptyDir() {
        CompilationResult result = compilerService.compileFiles(tempDir.toString());
        assertFalse(result.success());
        assertEquals("No .java files found.", result.message());
    }
}
package bg.sofia.uni.fmi.mjt.code.check.server.repositories;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class CodeCheckContextTest {

    @TempDir
    Path tempDir;

    @Mock
    private UserRepository mockUserRepo;

    @Test
    void testBuildThrowsExceptionWhenDataRootIsNull() {
        assertThrows(NullPointerException.class, () -> CodeCheckContext.builder().build(),
                "build() should throw NullPointerException if dataRoot is not set");
    }

    @Test
    void testBuildCreatesDirectory() {
        Path newPath = tempDir.resolve("new_data_root");

        CodeCheckContext.builder()
                .dataRoot(newPath)
                .build();

        assertTrue(Files.exists(newPath), "build() should create the dataRoot directory if it doesn't exist");
    }

    @Test
    void testBuildUsesDefaultComponentsWhenNoneProvided() {
        CodeCheckContext context = CodeCheckContext.builder()
                .dataRoot(tempDir)
                .build();

        assertNotNull(context.userRepository(), "UserRepository should be initialized by default");
        assertNotNull(context.courseRepository(), "CourseRepository should be initialized by default");
        assertNotNull(context.assignmentRepository(), "AssignmentRepository should be initialized by default");
        assertNotNull(context.submissionRepository(), "SubmissionRepository should be initialized by default");

        assertNotNull(context.validationService(), "ValidationService should be initialized by default");
        assertNotNull(context.uploadsService(), "UploadsService should be initialized by default");
        assertNotNull(context.compilerService(), "CompilerService should be initialized by default");
        assertNotNull(context.passwordService(), "PasswordService should be initialized by default");
    }

    @Test
    void testBuildUsesCustomInjectedRepository() {
        CodeCheckContext context = CodeCheckContext.builder()
                .dataRoot(tempDir)
                .userRepository(mockUserRepo)
                .build();

        assertEquals(mockUserRepo, context.userRepository(),
                "Context should use the provided repository instead of the default one");
    }
}
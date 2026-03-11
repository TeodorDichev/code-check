package bg.sofia.uni.fmi.mjt.code.check.server.services;

import bg.sofia.uni.fmi.mjt.code.check.server.exceptions.EntityAlreadyExistsException;
import bg.sofia.uni.fmi.mjt.code.check.server.exceptions.EntityDoesNotExistException;
import bg.sofia.uni.fmi.mjt.code.check.server.exceptions.InvalidCommandException;
import bg.sofia.uni.fmi.mjt.code.check.server.exceptions.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ValidationServiceTest {

    private ValidationService validationService;
    private final String msg = "error message";

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        validationService = new ValidationService();
    }

    @Test
    void testThrowIfNullOrEmptyThrowsOnNull() {
        assertThrows(IllegalArgumentException.class,
                () -> validationService.throwIfNullOrEmpty(null, msg),
                "Should throw IllegalArgumentException when the input string is null");
    }

    @Test
    void testThrowIfNullOrEmptyThrowsOnEmpty() {
        assertThrows(IllegalArgumentException.class,
                () -> validationService.throwIfNullOrEmpty("", msg),
                "Should throw IllegalArgumentException when the input string is empty");
    }

    @Test
    void testThrowIfNullOrEmptyDoesNotThrowOnValidString() {
        assertDoesNotThrow(() -> validationService.throwIfNullOrEmpty("valid", msg),
                "Should not throw when a valid non-empty string is provided");
    }

    @Test
    void testThrowIfUnsafeThrowsOnForbiddenSlash() {
        assertThrows(IllegalArgumentException.class,
                () -> validationService.throwIfUnsafe("path/to", msg),
                "Should throw when string contains a forward slash (/)");
    }

    @Test
    void testThrowIfUnsafeThrowsOnForbiddenBackslash() {
        assertThrows(IllegalArgumentException.class,
                () -> validationService.throwIfUnsafe("path\\to", msg),
                "Should throw when string contains a backslash (\\)");
    }

    @Test
    void testThrowIfUnsafeThrowsOnForbiddenDot() {
        assertThrows(IllegalArgumentException.class,
                () -> validationService.throwIfUnsafe("../file.txt", msg),
                "Should throw when string contains a dot (.) to prevent path traversal");
    }

    @Test
    void testThrowIfUnsafeThrowsOnForbiddenUnderscore() {
        assertThrows(IllegalArgumentException.class,
                () -> validationService.throwIfUnsafe("user_name", msg),
                "Should throw when string contains an underscore (_)");
    }

    @Test
    void testThrowIfUnsafeThrowsOnBlank() {
        assertThrows(IllegalArgumentException.class,
                () -> validationService.throwIfUnsafe("   ", msg),
                "Should throw when string is blank/whitespace only");
    }

    @Test
    void testThrowIfUnsafeDoesNotThrowOnSafeString() {
        assertDoesNotThrow(() -> validationService.throwIfUnsafe("JavaCourse2026", msg),
                "Should not throw for a safe alphanumeric string");
    }

    @Test
    void testThrowEntityDoesNotExistIfNullThrowsOnNull() {
        assertThrows(EntityDoesNotExistException.class,
                () -> validationService.throwEntityDoesNotExistIfNull(null, msg),
                "Should throw EntityDoesNotExistException when the object is null");
    }

    @Test
    void testThrowEntityDoesNotExistIfNullDoesNotThrowOnObject() {
        assertDoesNotThrow(() -> validationService.throwEntityDoesNotExistIfNull(new Object(), msg),
                "Should not throw when the object exists (is not null)");
    }

    @Test
    void testValidateListParamsCountThrowsOnIncorrectCount() {
        assertThrows(InvalidCommandException.class,
                () -> validationService.validateListParamsCount(List.of("a"), 2),
                "Should throw InvalidCommandException when list size does not match expected count");
    }

    @Test
    void testValidateListParamsCountThrowsOnNullList() {
        assertThrows(InvalidCommandException.class,
                () -> validationService.validateListParamsCount(null, 1),
                "Should throw InvalidCommandException when the parameter list is null");
    }

    @Test
    void testValidateListParamsCountDoesNotThrowOnCorrectCount() {
        assertDoesNotThrow(() -> validationService.validateListParamsCount(List.of("a", "b"), 2),
                "Should not throw when list size matches the required parameter count");
    }

    @Test
    void testThrowIfDirectoryDoesNotExistThrowsOnNonExistentPath() {
        Path nonExistent = tempDir.resolve("nope");
        assertThrows(EntityDoesNotExistException.class,
                () -> validationService.throwIfDirectoryDoesNotExist(nonExistent),
                "Should throw EntityDoesNotExistException for a path that does not exist on disk");
    }

    @Test
    void testThrowIfDirectoryDoesNotExistThrowsOnNull() {
        assertThrows(EntityDoesNotExistException.class,
                () -> validationService.throwIfDirectoryDoesNotExist(null),
                "Should throw EntityDoesNotExistException when the path provided is null");
    }

    @Test
    void testThrowIfDirectoryDoesNotExistsDoesNotThrowOnValidPath() {
        assertDoesNotThrow(() -> validationService.throwIfDirectoryDoesNotExist(tempDir),
                "Should not throw when the provided directory path exists");
    }

    @Test
    void testThrowEntityAlreadyExistsIfContainedThrowsOnMatch() {
        assertThrows(EntityAlreadyExistsException.class,
                () -> validationService.throwEntityAlreadyExistsIfContained(Set.of("a"), "a", msg),
                "Should throw EntityAlreadyExistsException when the ID is already present in the set");
    }

    @Test
    void testThrowEntityAlreadyExistsIfContainedDoesNotThrowOnNoMatch() {
        assertDoesNotThrow(() -> validationService.throwEntityAlreadyExistsIfContained(Set.of("a"), "b", msg),
                "Should not throw when the ID is unique and not in the set");
    }

    @Test
    void testThrowEntityDoesNotExistsIfNotContainedThrowsOnNoMatch() {
        assertThrows(EntityDoesNotExistException.class,
                () -> validationService.throwEntityDoesNotExistsIfNotContained(Set.of("a"), "b", msg),
                "Should throw EntityDoesNotExistException when the required ID is missing from the set");
    }

    @Test
    void testThrowEntityDoesNotExistsIfNotContainedDoesNotThrowOnMatch() {
        assertDoesNotThrow(() -> validationService.throwEntityDoesNotExistsIfNotContained(Set.of("a"), "a", msg),
                "Should not throw when the required ID is present in the set");
    }

    @Test
    void testThrowUnauthorizedIfBeforeThrowsWhenPastDeadline() {
        LocalDateTime deadline = LocalDateTime.now().minusDays(1);
        assertThrows(UnauthorizedException.class,
                () -> validationService.throwUnauthorizedIfBefore(deadline, LocalDateTime.now()),
                "Should throw UnauthorizedException when the current time is after the deadline");
    }

    @Test
    void testThrowUnauthorizedIfBeforeDoesNotThrowWhenBeforeDeadline() {
        LocalDateTime deadline = LocalDateTime.now().plusDays(1);
        assertDoesNotThrow(() -> validationService.throwUnauthorizedIfBefore(deadline, LocalDateTime.now()),
                "Should not throw when the submission is made before the deadline");
    }
}
package bg.sofia.uni.fmi.mjt.code.check.server.storage;

import bg.sofia.uni.fmi.mjt.code.check.server.entities.Assignment;
import com.google.gson.Gson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AssignmentStorageTest {

    @TempDir
    Path tempDir;

    private AssignmentStorage storage;

    @BeforeEach
    void setUp() {
        storage = new AssignmentStorage(tempDir, new Gson());
    }

    @Test
    void testAssignmentIsSavedInCorrectSubfolder() {
        Assignment assignment = new Assignment(
                "id1",
                "id2",
                "name1",
                "nqma",
                LocalDateTime.now(),
                LocalDateTime.now());

        storage.save("id1", assignment);
        Path expectedPath = tempDir.resolve("assignments").resolve("id1.json");

        assertTrue(Files.exists(expectedPath),
                "Assignment should be stored within the 'assignments' subdirectory");
    }
}
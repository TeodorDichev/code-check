package bg.sofia.uni.fmi.mjt.code.check.server.storage;

import bg.sofia.uni.fmi.mjt.code.check.server.entities.Submission;
import com.google.gson.Gson;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SubmissionStorageTest {
    @TempDir Path tempDir;

    @Test
    void testSubmissionIsSavedInSubmissionsSubfolder() {
        SubmissionStorage storage = new SubmissionStorage(tempDir, new Gson());
        Submission sub = new Submission(
                "id123",
                "na/mainata/si",
                "id456",
                "pakstoyo",
                LocalDateTime.now());

        storage.save("id123", sub);

        Path expectedPath = tempDir.resolve("submissions").resolve("id123.json");
        assertTrue(Files.exists(expectedPath), "Submission should be in the 'submissions' directory");
    }
}
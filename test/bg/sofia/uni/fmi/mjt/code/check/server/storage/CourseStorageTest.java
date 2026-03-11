package bg.sofia.uni.fmi.mjt.code.check.server.storage;

import bg.sofia.uni.fmi.mjt.code.check.server.entities.Course;
import com.google.gson.Gson;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CourseStorageTest {
    @TempDir Path tempDir;

    @Test
    void testCourseIsSavedInCoursesSubfolder() {
        CourseStorage storage = new CourseStorage(tempDir, new Gson());
        Course course = new Course(
                "MJT",
                "Modern Java Technologies",
                "nqma-kak-da-joinesh",
                Set.of(),
                Set.of(),
                Set.of());

        storage.save("MJT", course);

        Path expectedPath = tempDir.resolve("courses").resolve("MJT.json");
        assertTrue(Files.exists(expectedPath), "Course should be in the 'courses' directory");
    }
}
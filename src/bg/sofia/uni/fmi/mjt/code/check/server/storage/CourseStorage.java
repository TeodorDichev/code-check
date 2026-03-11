package bg.sofia.uni.fmi.mjt.code.check.server.storage;

import bg.sofia.uni.fmi.mjt.code.check.server.entities.Course;
import com.google.gson.Gson;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

public class CourseStorage extends AbstractFileStorage<Course> {
    private final Path coursesDir;
    private static final String COURSE_DIR = "courses";

    public CourseStorage(Path dataDir, Gson gson) {
        Path dir = dataDir.resolve(COURSE_DIR);
        super(dir, gson, Course.class);
        coursesDir = dir;
    }

    public List<String> findAllIdsByPrefix(String prefix) {
        try (Stream<Path> stream = Files.list(coursesDir)) {
            return stream
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .filter(name -> name.startsWith(prefix))
                    .map(name -> name.replace(JSON_EXTENSION, ""))
                    .toList();
        } catch (IOException e) {
            throw new RuntimeException("Error scanning courses directory", e);
        }
    }
}
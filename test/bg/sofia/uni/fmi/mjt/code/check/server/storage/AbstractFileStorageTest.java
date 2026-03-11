package bg.sofia.uni.fmi.mjt.code.check.server.storage;

import com.google.gson.Gson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AbstractFileStorageTest {

    @TempDir
    Path tempDir;

    private Gson gson;
    private TestStorage storage;

    // A simple entity for testing
    private record TestEntity(String name, int value) {}

    // Concrete implementation of the abstract class for testing
    private static class TestStorage extends AbstractFileStorage<TestEntity> {
        protected TestStorage(Path baseDir, Gson gson) {
            super(baseDir, gson, TestEntity.class);
        }
    }

    @BeforeEach
    void setUp() {
        gson = new Gson();
        storage = new TestStorage(tempDir, gson);
    }

    @Test
    void testSaveCreatesJsonFile() {
        TestEntity entity = new TestEntity("test", 123);
        storage.save("id1", entity);

        Path expectedPath = tempDir.resolve("id1.json");
        assertTrue(java.nio.file.Files.exists(expectedPath),
                "Save should create a physical .json file on the disk");
    }

    @Test
    void testLoadReturnsEmptyOptionalWhenFileMissing() {
        Optional<TestEntity> result = storage.load("non-existent");

        assertTrue(result.isEmpty(),
                "Loading a non-existent ID should return an empty Optional instead of throwing an exception");
    }

    @Test
    void testLoadReturnsCorrectEntity() {
        TestEntity original = new TestEntity("Persistence", 2026);
        storage.save("id2", original);

        Optional<TestEntity> loaded = storage.load("id2");

        assertTrue(loaded.isPresent(),
                "Loaded optional should contain a value for an existing ID");

        // Ensure the loaded data is of correct type and not null
        assertInstanceOf(TestEntity.class, loaded.get(),
                "The loaded data from disk should be an instance of TestEntity");

        assertEquals(original, loaded.get(),
                "The loaded entity data must be identical to the one originally saved");
    }

    @Test
    void testExistsReturnsTrueOnlyWhenFileExists() {
        storage.save("exists-check", new TestEntity("data", 1));

        assertTrue(storage.exists("exists-check"),
                "exists() should return true for a saved entity");
        assertFalse(storage.exists("does-not-exist"),
                "exists() should return false for an entity that has never been saved");
    }

    @Test
    void testDeleteRemovesFile() {
        storage.save("to-delete", new TestEntity("gone", 0));
        assertTrue(storage.exists("to-delete"), "Entity should exist before deletion");

        storage.delete("to-delete");

        assertFalse(storage.exists("to-delete"),
                "The file must be removed from the disk after calling delete()");
    }

    @Test
    void testDeleteDoesNotThrowWhenFileMissing() {
        assertDoesNotThrow(() -> storage.delete("never-existed"),
                "Deleting a non-existent file should be a no-op and should not throw exceptions");
    }

    @Test
    void testSaveCreatesDirectoriesAutomatically() {
        Path nestedDir = tempDir.resolve("deeply/nested/storage");
        TestStorage nestedStorage = new TestStorage(nestedDir, gson);

        nestedStorage.save("nested-id", new TestEntity("nest", 1));

        assertTrue(java.nio.file.Files.exists(nestedDir),
                "The storage service should automatically create the directory hierarchy if it doesn't exist");
    }
}
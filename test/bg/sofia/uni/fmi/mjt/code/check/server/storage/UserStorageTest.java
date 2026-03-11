package bg.sofia.uni.fmi.mjt.code.check.server.storage;

import bg.sofia.uni.fmi.mjt.code.check.server.entities.User;
import com.google.gson.Gson;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class UserStorageTest {
    @TempDir Path tempDir;

    @Test
    void testUserIsSavedInUsersSubfolder() {
        UserStorage storage = new UserStorage(tempDir, new Gson());
        User user = new User("stoyo", "mjt");

        storage.save("stoyo", user);

        Path expectedPath = tempDir.resolve("users").resolve("stoyo.json");
        assertTrue(Files.exists(expectedPath), "User should be in the 'users' directory");
    }
}
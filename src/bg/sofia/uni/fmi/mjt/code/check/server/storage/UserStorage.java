package bg.sofia.uni.fmi.mjt.code.check.server.storage;

import bg.sofia.uni.fmi.mjt.code.check.server.entities.User;
import com.google.gson.Gson;
import java.nio.file.Path;

public class UserStorage extends AbstractFileStorage<User> {
    private static final String USERS_DIR = "users";

    public UserStorage(Path dataDir, Gson gson) {
        super(dataDir.resolve(USERS_DIR), gson, User.class);
    }
}
package bg.sofia.uni.fmi.mjt.code.check.server.storage;

import com.google.gson.Gson;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public abstract class AbstractFileStorage<T> implements Storage<T> {
    private final Path baseDir;
    private final Gson gson;
    private final Class<T> type;
    protected static final String JSON_EXTENSION = ".json";

    protected AbstractFileStorage(Path baseDir, Gson gson, Class<T> type) {
        this.baseDir = baseDir;
        this.gson = gson;
        this.type = type;
    }

    protected Path resolvePath(String id) {
        return baseDir.resolve(id + JSON_EXTENSION);
    }

    @Override
    public Optional<T> load(String id) {
        Path path = resolvePath(id);
        if (!Files.exists(path)) {
            return Optional.empty();
        }
        try {
            String json = Files.readString(path);
            return Optional.of(gson.fromJson(json, type));
        } catch (IOException e) {
            throw new RuntimeException("Failed to load entity: " + id, e);
        }
    }

    @Override
    public void save(String id, T entity) {
        Path path = resolvePath(id);
        try {
            Files.createDirectories(baseDir);
            Files.writeString(path, gson.toJson(entity));
        } catch (IOException e) {
            throw new RuntimeException("Failed to save entity: " + id, e);
        }
    }

    @Override
    public boolean exists(String id) {
        return Files.exists(resolvePath(id));
    }

    @Override
    public void delete(String id) {
        try {
            Files.deleteIfExists(resolvePath(id));
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete entity: " + id, e);
        }
    }
}
package bg.sofia.uni.fmi.mjt.code.check.server.storage;

import java.util.Optional;

public interface Storage<T> {
    Optional<T> load(String id);

    void save(String id, T entity);

    boolean exists(String id);

    void delete(String id);
}
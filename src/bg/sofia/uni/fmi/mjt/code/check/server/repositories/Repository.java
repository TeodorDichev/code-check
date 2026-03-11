package bg.sofia.uni.fmi.mjt.code.check.server.repositories;

/**
 * A stateless repository interface that interacts directly with the storage layer.
 * Internal Maps were removed to ensure a "Single Source of Truth" on the disk,
 * minimize RAM usage, and prevent data inconsistency in a multithreaded environment.
 * *
 * The idea was: what if we have 500mb of users if we load them in RAM this may be
 * 1gb of RAM memory. Now we load then directly from the disk. Yes this is also not
 * optimal. But it can be much more easily optimized for example we group the users
 * by the first letter of their username.
 */
public interface Repository<T> {
    T get(String key);

    boolean contains(String key);

    T add(T entity);
}

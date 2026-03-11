package bg.sofia.uni.fmi.mjt.code.check.server.services;

import java.util.concurrent.ConcurrentHashMap;

public final class LockRegistry {

    private static final ConcurrentHashMap<String, Object> LOCKS =
            new ConcurrentHashMap<>();

    private LockRegistry() { }

    public static Object getLock(String id) {
        return LOCKS.computeIfAbsent(id, k -> new Object());
    }

    public static void cleanup(String id, Object lock) {
        LOCKS.remove(id, lock);
    }
}



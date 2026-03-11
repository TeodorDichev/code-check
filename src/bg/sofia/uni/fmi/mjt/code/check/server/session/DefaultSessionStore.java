package bg.sofia.uni.fmi.mjt.code.check.server.session;

import bg.sofia.uni.fmi.mjt.code.check.server.entities.User;

import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stateful part of my application
 * Utilizes cashing users for faster retrieval time
 * Breaks the single point of truth in my app but saves a lot of reading from the disk
 * */
public class DefaultSessionStore implements SessionStore {
    private final Map<SocketChannel, User> sessions;

    public DefaultSessionStore() {
        sessions = new ConcurrentHashMap<>();
    }

    @Override
    public boolean hasSession(SocketChannel channel) {
        return sessions.containsKey(channel);
    }

    @Override
    public User getUser(SocketChannel channel) {
        return sessions.get(channel);
    }

    @Override
    public void register(Session session) {
        sessions.put(session.key(), session.user());
    }

    @Override
    public void remove(SocketChannel channel) {
        sessions.remove(channel);
    }

    @Override
    public void updateUser(String username, User updatedUser) {
        if (username == null || updatedUser == null) {
            return;
        }

        sessions.replaceAll((channel, cachedUser) ->
                cachedUser.username().equals(username) ? updatedUser : cachedUser
        );
    }
}
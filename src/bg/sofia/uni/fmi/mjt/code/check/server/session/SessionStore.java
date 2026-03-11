package bg.sofia.uni.fmi.mjt.code.check.server.session;

import bg.sofia.uni.fmi.mjt.code.check.server.entities.User;

import java.nio.channels.SocketChannel;

public interface SessionStore {

    boolean hasSession(SocketChannel channel);

    User getUser(SocketChannel channel);

    void register(Session session);

    void remove(SocketChannel channel);

    void updateUser(String username, User updatedUser);
}
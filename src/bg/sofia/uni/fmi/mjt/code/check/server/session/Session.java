package bg.sofia.uni.fmi.mjt.code.check.server.session;

import bg.sofia.uni.fmi.mjt.code.check.server.entities.User;

import java.nio.channels.SocketChannel;

public record Session(SocketChannel key, User user) {
}
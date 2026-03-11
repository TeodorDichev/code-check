package bg.sofia.uni.fmi.mjt.code.check.server.session;

import java.nio.channels.SocketChannel;

/**
 * Represents one discrete file upload job that can be processed independently in a worker thread.
 * */
public record UploadTask(SocketChannel channel, ConnectionState data, String command) {
}

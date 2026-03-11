package bg.sofia.uni.fmi.mjt.code.check.server.exceptions;

public class ConnectionClosedDuringUpload extends RuntimeException {
    public ConnectionClosedDuringUpload(String message) {
        super(message);
    }

    public ConnectionClosedDuringUpload(String message, Throwable cause) {
        super(message, cause);
    }
}

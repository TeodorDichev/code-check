package bg.sofia.uni.fmi.mjt.code.check.server.services.logging.providers;

import bg.sofia.uni.fmi.mjt.code.check.server.exceptions.LoggerOperationException;

public interface FileProvider extends Provider {

    void writeError(Exception e) throws LoggerOperationException;
}

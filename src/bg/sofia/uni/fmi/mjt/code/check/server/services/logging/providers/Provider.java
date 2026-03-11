package bg.sofia.uni.fmi.mjt.code.check.server.services.logging.providers;

import bg.sofia.uni.fmi.mjt.code.check.server.exceptions.LoggerOperationException;

public interface Provider {
    void write(String str) throws LoggerOperationException;
}
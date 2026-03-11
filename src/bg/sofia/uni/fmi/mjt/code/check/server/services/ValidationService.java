package bg.sofia.uni.fmi.mjt.code.check.server.services;

import bg.sofia.uni.fmi.mjt.code.check.server.exceptions.EntityAlreadyExistsException;
import bg.sofia.uni.fmi.mjt.code.check.server.exceptions.EntityDoesNotExistException;
import bg.sofia.uni.fmi.mjt.code.check.server.exceptions.InvalidCommandException;
import bg.sofia.uni.fmi.mjt.code.check.server.exceptions.UnauthorizedException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public class ValidationService {
    /**
     * The idea is if they are include in your username when saving it as a json it may look like that
     * ../teo/iska/da/te/hackne and this may cause issues with the saving directory
     * */
    private static final List<String> FORBIDDEN_SYMBOLS = List.of("/", "\\", "..", "_");

    public void throwIfNullOrEmpty(String val, String exceptionMessage) {
        if (val == null || val.isEmpty()) {
            throw new IllegalArgumentException(exceptionMessage);
        }
    }

    public void throwIfUnsafe(String val, String exceptionMessage) {
        if (val == null || val.isBlank()) {
            throw new IllegalArgumentException(exceptionMessage);
        }

        for (String symbol : FORBIDDEN_SYMBOLS) {
            if (val.contains(symbol)) {
                throw new IllegalArgumentException(exceptionMessage);
            }
        }
    }

    public void throwIfNull(Object entity, String exceptionMessage) {
        if (entity == null) {
            throw new IllegalArgumentException(exceptionMessage);
        }
    }

    public void throwEntityDoesNotExistIfNull(Object entity, String exceptionMessage) {
        if (entity == null) {
            throw new EntityDoesNotExistException(exceptionMessage);
        }
    }

    public void validateListParamsCount(List<String> params, int expectedParamsCount) {
        if (params == null || params.size() != expectedParamsCount) {
            throw new InvalidCommandException("Incorrect numbers of params");
        }
    }

    public void throwIfFileDoesNotExist(Path path) {
        throwIfDirectoryDoesNotExist(path);

        if (!Files.isRegularFile(path)) {
            throw new IllegalArgumentException(String.format("Path [%s] is not a regular file",
                    path.toAbsolutePath()));
        }
    }

    public void throwIfDirectoryDoesNotExist(Path path) {
        if (path == null || !Files.exists(path)) {
            throw new EntityDoesNotExistException(String.format("Path [%s] does not exist",
                    path == null ? "null" : path.toAbsolutePath()));
        }
    }

    public <T> void throwEntityAlreadyExistsIfContained(Set<T> list, T object, String message) {
        if (list.contains(object)) {
            throw new EntityAlreadyExistsException(message);
        }
    }

    public <T> void throwEntityDoesNotExistsIfNotContained(Set<T> list, T object, String message) {
        if (!list.contains(object)) {
            throw new EntityDoesNotExistException(message);
        }
    }

    public void throwUnauthorizedIfBefore(LocalDateTime deadline, LocalDateTime now) {
        if (deadline.isBefore(now)) {
            throw new UnauthorizedException("You have passed the deadline!");
        }
    }
}

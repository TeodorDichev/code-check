package bg.sofia.uni.fmi.mjt.code.check.server.repositories;

import bg.sofia.uni.fmi.mjt.code.check.server.entities.User;
import bg.sofia.uni.fmi.mjt.code.check.server.exceptions.EntityAlreadyExistsException;
import bg.sofia.uni.fmi.mjt.code.check.server.services.ValidationService;
import bg.sofia.uni.fmi.mjt.code.check.server.storage.UserStorage;

public class UserRepository implements Repository<User> {
    private final UserStorage storage;
    private final ValidationService validationService;

    public UserRepository(UserStorage storage, ValidationService validationService) {
        this.storage = storage;
        this.validationService = validationService;
    }

    @Override
    public User get(String username) {
        validationService.throwIfUnsafe(username, "Username cannot be null or empty");
        return storage.load(username).orElse(null);
    }

    @Override
    public boolean contains(String username) {
        validationService.throwIfUnsafe(username, "Username cannot be null or empty");
        return storage.exists(username);
    }

    @Override
    public User add(User entity) {
        validationService.throwIfNull(entity, "User cannot be null");
        String username = entity.username();
        validationService.throwIfUnsafe(username, "Username cannot be null or empty");

        if (storage.exists(username)) {
            throw new EntityAlreadyExistsException("User " + username + " already exists");
        }

        storage.save(username, entity);
        return entity;
    }

    public void removeAdministeredCourse(User user, String courseId) {
        validationService.throwIfNull(user, "User cannot be null");
        user.removeAdministeredCourse(courseId);
        storage.save(user.username(), user);
    }

    public void removeEnrolledCourse(User user, String courseId) {
        validationService.throwIfNull(user, "User cannot be null");
        user.removeEnrolledCourse(courseId);
        storage.save(user.username(), user);
    }

    public void addAdministeredCourse(User user, String courseId) {
        validationService.throwIfNull(user, "User cannot be null");
        user.administerCourse(courseId);
        storage.save(user.username(), user);
    }

    public void addEnrolledCourse(User user, String courseId) {
        validationService.throwIfNull(user, "User cannot be null");
        user.enrollInCourse(courseId);
        storage.save(user.username(), user);
    }
}
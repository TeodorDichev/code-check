package bg.sofia.uni.fmi.mjt.code.check.server.repositories;

import bg.sofia.uni.fmi.mjt.code.check.server.entities.Assignment;
import bg.sofia.uni.fmi.mjt.code.check.server.exceptions.EntityAlreadyExistsException;
import bg.sofia.uni.fmi.mjt.code.check.server.services.ValidationService;
import bg.sofia.uni.fmi.mjt.code.check.server.storage.AssignmentStorage;

public class AssignmentRepository implements Repository<Assignment> {
    private final AssignmentStorage storage;
    private final ValidationService validationService;

    public AssignmentRepository(AssignmentStorage storage, ValidationService validationService) {
        this.storage = storage;
        this.validationService = validationService;
    }

    @Override
    public Assignment get(String id) {
        validationService.throwIfNullOrEmpty(id, "Assignment id cannot be null or empty");
        return storage.load(id).orElse(null);
    }

    @Override
    public boolean contains(String id) {
        validationService.throwIfNullOrEmpty(id, "Assignment id cannot be null or empty");
        return storage.exists(id);
    }

    @Override
    public Assignment add(Assignment entity) {
        validationService.throwIfNull(entity, "Assignment cannot be null");
        String id = entity.id();
        validationService.throwIfNullOrEmpty(id, "Assignment id cannot be null or empty");
        validationService.throwIfUnsafe(entity.name(), "Assignment name cannot be null or empty");
        validationService.throwIfNull(entity.assignedOn(), "Assignment assignedOn cannot be null");
        validationService.throwIfNull(entity.deadline(), "Assignment deadline cannot be null");

        if (storage.exists(id)) {
            throw new EntityAlreadyExistsException("Assignment with id " + id + " already exists");
        }

        storage.save(id, entity);
        return entity;
    }

    public void addSubmission(Assignment assignment, String studentUsername, String subId) {
        validationService.throwIfNull(assignment, "Assignment cannot be null");
        validationService.throwIfNullOrEmpty(subId, "Submission id cannot be null or empty");
        validationService.throwIfNullOrEmpty(studentUsername, "Username cannot be null or empty");

        assignment.addSubmission(studentUsername, subId);
        storage.save(assignment.id(), assignment);
    }

    public void removeSubmission(Assignment assignment, String username) {
        validationService.throwIfNull(assignment, "Assignment cannot be null");
        validationService.throwIfNullOrEmpty(username, "Username cannot be null or empty");

        assignment.removeSubmission(username);
        storage.save(assignment.id(), assignment);
    }

    public void remove(String id) {
        validationService.throwIfNullOrEmpty(id, "Assignment id cannot be null or empty");
        storage.delete(id);
    }
}
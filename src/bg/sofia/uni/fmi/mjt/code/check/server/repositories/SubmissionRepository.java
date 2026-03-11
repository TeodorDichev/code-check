package bg.sofia.uni.fmi.mjt.code.check.server.repositories;

import bg.sofia.uni.fmi.mjt.code.check.server.entities.Submission;
import bg.sofia.uni.fmi.mjt.code.check.server.exceptions.EntityAlreadyExistsException;
import bg.sofia.uni.fmi.mjt.code.check.server.exceptions.EntityDoesNotExistException;
import bg.sofia.uni.fmi.mjt.code.check.server.services.ValidationService;
import bg.sofia.uni.fmi.mjt.code.check.server.storage.SubmissionStorage;

import java.nio.file.Path;

public class SubmissionRepository implements Repository<Submission> {
    private final SubmissionStorage storage;
    private final ValidationService validationService;

    public SubmissionRepository(SubmissionStorage storage, ValidationService validationService) {
        this.storage = storage;
        this.validationService = validationService;
    }

    @Override
    public Submission get(String id) {
        validationService.throwIfNullOrEmpty(id, "Submission id cannot be null or empty");
        return storage.load(id).orElse(null);
    }

    @Override
    public boolean contains(String id) {
        validationService.throwIfNullOrEmpty(id, "Submission id cannot be null or empty");
        return storage.exists(id);
    }

    @Override
    public Submission add(Submission entity) {
        validationService.throwIfNull(entity, "Submission cannot be null");
        String id = entity.id();
        validationService.throwIfNullOrEmpty(id, "Submission id cannot be null or empty");

        if (storage.exists(id)) {
            throw new EntityAlreadyExistsException("Submission " + id + " already exists");
        }

        storage.save(id, entity);
        return entity;
    }

    public void remove(String id) {
        validationService.throwIfNullOrEmpty(id, "Submission id cannot be null or empty");
        if (!storage.exists(id)) {
            throw new EntityDoesNotExistException("Submission does not exist");
        }
        storage.delete(id);
    }

    public void gradeSubmission(Submission submission,
                                String graderUsername, double grade, String comment) {
        validationService.throwIfNull(submission, "Submission cannot be null");
        validationService.throwIfNullOrEmpty(graderUsername, "Submission graderUsername cannot be null or empty");
        validationService.throwIfNullOrEmpty(comment, "Submission comment cannot be null or empty");

        submission.grade(graderUsername, grade, comment);

        storage.save(submission.id(), submission);
    }

    public void addFileComment(Submission submission, String fileName, String comment) {
        validationService.throwIfNull(submission, "Submission, cannot be null");
        validationService.throwIfFileDoesNotExist(Path.of(submission.path()).resolve(fileName));
        submission.addFileComment(fileName, comment);
        storage.save(submission.id(), submission);
    }
}
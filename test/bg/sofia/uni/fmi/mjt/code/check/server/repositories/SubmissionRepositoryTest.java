package bg.sofia.uni.fmi.mjt.code.check.server.repositories;

import bg.sofia.uni.fmi.mjt.code.check.server.entities.Submission;
import bg.sofia.uni.fmi.mjt.code.check.server.exceptions.EntityAlreadyExistsException;
import bg.sofia.uni.fmi.mjt.code.check.server.exceptions.EntityDoesNotExistException;
import bg.sofia.uni.fmi.mjt.code.check.server.services.ValidationService;
import bg.sofia.uni.fmi.mjt.code.check.server.storage.SubmissionStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubmissionRepositoryTest {

    @Mock
    private SubmissionStorage storage;
    @Mock
    private ValidationService validationService;
    @Mock
    private Submission submission;

    private SubmissionRepository repository;

    @BeforeEach
    void setUp() {
        repository = new SubmissionRepository(storage, validationService);
    }

    @Test
    void testGetSuccess() {
        String id = "sub-123";
        when(storage.load(id)).thenReturn(Optional.of(submission));

        Submission result = repository.get(id);

        assertEquals(submission, result);
        verify(validationService).throwIfNullOrEmpty(id, "Submission id cannot be null or empty");
    }

    @Test
    void testGetReturnsNullWhenNotFound() {
        String id = "none";
        when(storage.load(id)).thenReturn(Optional.empty());

        assertNull(repository.get(id));
    }

    @Test
    void testContainsTrue() {
        String id = "sub-123";
        when(storage.exists(id)).thenReturn(true);

        assertTrue(repository.contains(id));
        verify(validationService).throwIfNullOrEmpty(id, "Submission id cannot be null or empty");
    }

    @Test
    void testContainsFalse() {
        String id = "sub-123";
        when(storage.exists(id)).thenReturn(false);

        assertFalse(repository.contains(id));
    }

    @Test
    void testAddSuccess() {
        String id = "sub-123";
        when(submission.id()).thenReturn(id);
        when(storage.exists(id)).thenReturn(false);

        Submission result = repository.add(submission);

        assertEquals(submission, result);
        verify(storage).save(id, submission);
        verify(validationService).throwIfNull(submission, "Submission cannot be null");
        verify(validationService).throwIfNullOrEmpty(id, "Submission id cannot be null or empty");
    }

    @Test
    void testAddThrowsIfAlreadyExists() {
        String id = "sub-123";
        when(submission.id()).thenReturn(id);
        when(storage.exists(id)).thenReturn(true);

        assertThrows(EntityAlreadyExistsException.class, () -> repository.add(submission));
    }

    @Test
    void testRemoveSuccess() {
        String id = "sub-123";
        when(storage.exists(id)).thenReturn(true);

        repository.remove(id);

        verify(storage).delete(id);
        verify(validationService).throwIfNullOrEmpty(id, "Submission id cannot be null or empty");
    }

    @Test
    void testRemoveThrowsIfDoesNotExist() {
        String id = "sub-123";
        when(storage.exists(id)).thenReturn(false);

        assertThrows(EntityDoesNotExistException.class, () -> repository.remove(id));
    }

    @Test
    void testGradeSubmissionSuccess() {
        String grader = "teacher1";
        double grade = 5.5;
        String comment = "Great job";
        String id = "sub-123";
        when(submission.id()).thenReturn(id);

        repository.gradeSubmission(submission, grader, grade, comment);

        verify(submission).grade(grader, grade, comment);
        verify(storage).save(id, submission);
        verify(validationService).throwIfNull(submission, "Submission cannot be null");
        verify(validationService).throwIfNullOrEmpty(grader, "Submission graderUsername cannot be null or empty");
        verify(validationService).throwIfNullOrEmpty(comment, "Submission comment cannot be null or empty");
    }

    @Test
    void testAddFileCommentSuccess() {
        String fileName = "Solution.java";
        String comment = "Logic error here";
        String id = "sub-123";
        String path = "storage/submissions/123";

        when(submission.id()).thenReturn(id);
        when(submission.path()).thenReturn(path);

        repository.addFileComment(submission, fileName, comment);

        verify(validationService).throwIfFileDoesNotExist(any(Path.class));
        verify(submission).addFileComment(fileName, comment);
        verify(storage).save(id, submission);
    }
}
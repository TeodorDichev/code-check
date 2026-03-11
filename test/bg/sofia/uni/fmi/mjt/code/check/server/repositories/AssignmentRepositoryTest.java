package bg.sofia.uni.fmi.mjt.code.check.server.repositories;

import bg.sofia.uni.fmi.mjt.code.check.server.entities.Assignment;
import bg.sofia.uni.fmi.mjt.code.check.server.exceptions.EntityAlreadyExistsException;
import bg.sofia.uni.fmi.mjt.code.check.server.services.ValidationService;
import bg.sofia.uni.fmi.mjt.code.check.server.storage.AssignmentStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssignmentRepositoryTest {
    @Mock private AssignmentStorage storage;
    @Mock private ValidationService validationService;
    @Mock private Assignment assignment;

    private AssignmentRepository repository;
    private final String assignmentId = "asgn-123";
    private final String student = "student1";

    @BeforeEach
    void setUp() {
        repository = new AssignmentRepository(storage, validationService);
    }

    @Test
    void testGetSuccess() {
        when(storage.load(assignmentId)).thenReturn(Optional.of(assignment));

        Assignment result = repository.get(assignmentId);

        assertNotNull(result,
                "Repository should return the assignment when it exists in storage");
        assertEquals(assignment, result,
                "The returned assignment should be the same instance loaded from storage");
        verify(validationService).throwIfNullOrEmpty(assignmentId, "Assignment id cannot be null or empty");
    }

    @Test
    void testContainsReturnsTrue() {
        when(storage.exists(assignmentId)).thenReturn(true);

        assertTrue(repository.contains(assignmentId),
                "Contains should return true if the assignment exists in storage");
        verify(validationService).throwIfNullOrEmpty(assignmentId, "Assignment id cannot be null or empty");
    }

    @Test
    void testAddSuccess() {
        when(assignment.id()).thenReturn(assignmentId);
        when(assignment.name()).thenReturn("Lab 1");

        when(assignment.assignedOn()).thenReturn(LocalDateTime.now().toString());
        when(assignment.deadline()).thenReturn(LocalDateTime.now().plusDays(7).toString());
        when(storage.exists(assignmentId)).thenReturn(false);

        Assignment result = repository.add(assignment);

        assertEquals(assignment, result,
                "Add should return the same assignment entity that was saved");
        verify(storage).save(assignmentId, assignment);
    }

    @Test
    void testAddThrowsWhenAlreadyExists() {
        when(assignment.id()).thenReturn(assignmentId);
        when(assignment.name()).thenReturn("Lab 1");
        when(assignment.assignedOn()).thenReturn(LocalDateTime.now().toString());
        when(assignment.deadline()).thenReturn(LocalDateTime.now().plusDays(7).toString());
        when(storage.exists(assignmentId)).thenReturn(true);

        assertThrows(EntityAlreadyExistsException.class, () -> repository.add(assignment),
                "Adding an assignment that already exists in storage should throw EntityAlreadyExistsException");
    }

    @Test
    void testAddSubmissionSuccess() {
        when(assignment.id()).thenReturn(assignmentId);

        String subId = "sub-456";
        repository.addSubmission(assignment, student, subId);

        verify(assignment).addSubmission(student, subId);
        verify(storage).save(assignmentId, assignment);
    }

    @Test
    void testRemoveSubmissionSuccess() {
        when(assignment.id()).thenReturn(assignmentId);

        repository.removeSubmission(assignment, student);

        verify(assignment).removeSubmission(student);
        verify(storage).save(assignmentId, assignment);
    }

    @Test
    void testRemoveSuccess() {
        repository.remove(assignmentId);

        verify(storage).delete(assignmentId);
        verify(validationService).throwIfNullOrEmpty(assignmentId, "Assignment id cannot be null or empty");
    }

    @Test
    void testGetThrowsWhenIdNull() {
        doThrow(new IllegalArgumentException("Validation failed")).when(validationService)
                .throwIfNullOrEmpty(null, "Assignment id cannot be null or empty");

        assertThrows(IllegalArgumentException.class, () -> repository.get(null),
                "Repository should propagate IllegalArgumentException when validationService fails for null ID");
    }
}
package bg.sofia.uni.fmi.mjt.code.check.server.repositories;

import bg.sofia.uni.fmi.mjt.code.check.server.entities.User;
import bg.sofia.uni.fmi.mjt.code.check.server.exceptions.EntityAlreadyExistsException;
import bg.sofia.uni.fmi.mjt.code.check.server.services.ValidationService;
import bg.sofia.uni.fmi.mjt.code.check.server.storage.UserStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserRepositoryTest {

    @Mock
    private UserStorage storage;
    @Mock
    private ValidationService validationService;
    @Mock
    private User user;

    private UserRepository repository;

    @BeforeEach
    void setUp() {
        repository = new UserRepository(storage, validationService);
    }

    @Test
    void testGetSuccess() {
        String username = "testUser";
        when(storage.load(username)).thenReturn(Optional.of(user));

        User result = repository.get(username);

        assertEquals(user, result);
        verify(validationService).throwIfUnsafe(username, "Username cannot be null or empty");
    }

    @Test
    void testGetReturnsNullWhenNotFound() {
        String username = "unknown";
        when(storage.load(username)).thenReturn(Optional.empty());

        assertNull(repository.get(username));
    }

    @Test
    void testContainsTrue() {
        String username = "testUser";
        when(storage.exists(username)).thenReturn(true);

        assertTrue(repository.contains(username));
        verify(validationService).throwIfUnsafe(username, "Username cannot be null or empty");
    }

    @Test
    void testContainsFalse() {
        String username = "testUser";
        when(storage.exists(username)).thenReturn(false);

        assertFalse(repository.contains(username));
    }

    @Test
    void testAddSuccess() {
        String username = "testUser";
        when(user.username()).thenReturn(username);
        when(storage.exists(username)).thenReturn(false);

        User result = repository.add(user);

        assertEquals(user, result);
        verify(storage).save(username, user);
        verify(validationService).throwIfNull(user, "User cannot be null");
        verify(validationService).throwIfUnsafe(username, "Username cannot be null or empty");
    }

    @Test
    void testAddThrowsIfAlreadyExists() {
        String username = "testUser";
        when(user.username()).thenReturn(username);
        when(storage.exists(username)).thenReturn(true);

        assertThrows(EntityAlreadyExistsException.class, () -> repository.add(user));
    }

    @Test
    void testRemoveAdministeredCourse() {
        String courseId = "course-123";
        String username = "adminUser";
        when(user.username()).thenReturn(username);

        repository.removeAdministeredCourse(user, courseId);

        verify(user).removeAdministeredCourse(courseId);
        verify(storage).save(username, user);
        verify(validationService).throwIfNull(user, "User cannot be null");
    }

    @Test
    void testRemoveEnrolledCourse() {
        String courseId = "course-123";
        String username = "studentUser";
        when(user.username()).thenReturn(username);

        repository.removeEnrolledCourse(user, courseId);

        verify(user).removeEnrolledCourse(courseId);
        verify(storage).save(username, user);
        verify(validationService).throwIfNull(user, "User cannot be null");
    }

    @Test
    void testAddAdministeredCourse() {
        String courseId = "course-123";
        String username = "adminUser";
        when(user.username()).thenReturn(username);

        repository.addAdministeredCourse(user, courseId);

        verify(user).administerCourse(courseId);
        verify(storage).save(username, user);
    }

    @Test
    void testAddEnrolledCourse() {
        String courseId = "course-123";
        String username = "studentUser";
        when(user.username()).thenReturn(username);

        repository.addEnrolledCourse(user, courseId);

        verify(user).enrollInCourse(courseId);
        verify(storage).save(username, user);
    }
}
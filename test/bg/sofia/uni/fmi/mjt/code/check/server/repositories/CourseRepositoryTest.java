package bg.sofia.uni.fmi.mjt.code.check.server.repositories;

import bg.sofia.uni.fmi.mjt.code.check.server.entities.Course;
import bg.sofia.uni.fmi.mjt.code.check.server.exceptions.EntityAlreadyExistsException;
import bg.sofia.uni.fmi.mjt.code.check.server.exceptions.EntityDoesNotExistException;
import bg.sofia.uni.fmi.mjt.code.check.server.services.ValidationService;
import bg.sofia.uni.fmi.mjt.code.check.server.storage.CourseStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseRepositoryTest {

    @Mock
    private CourseStorage storage;
    @Mock
    private ValidationService validationService;
    @Mock
    private Course course;

    private CourseRepository repository;

    @BeforeEach
    void setUp() {
        repository = new CourseRepository(storage, validationService);
    }

    @Test
    void testGetSuccess() {
        String id = "java-8";
        when(storage.load(id)).thenReturn(Optional.of(course));

        Course result = repository.get(id);

        assertEquals(course, result);
        verify(validationService).throwIfNullOrEmpty(id, "Course id cannot be null or empty");
    }

    @Test
    void testContainsTrue() {
        String id = "java-8";
        when(storage.exists(id)).thenReturn(true);

        assertTrue(repository.contains(id));
        verify(validationService).throwIfNullOrEmpty(id, "Course id cannot be null or empty");
    }

    @Test
    void testExistsByNameTrue() {
        String name = "Java";
        when(storage.findAllIdsByPrefix(name)).thenReturn(List.of("Java_1"));

        assertTrue(repository.existsByName(name));
        verify(validationService).throwIfNullOrEmpty(name, "Course name cannot be null or empty");
    }

    @Test
    void testExistsByNameFalse() {
        String name = "Empty";
        when(storage.findAllIdsByPrefix(name)).thenReturn(List.of());

        assertFalse(repository.existsByName(name));
    }

    @Test
    void testAddSuccess() {
        String id = "java-8";
        when(course.id()).thenReturn(id);
        when(storage.exists(id)).thenReturn(false);

        Course result = repository.add(course);

        assertEquals(course, result);
        verify(storage).save(id, course);
        verify(validationService).throwIfNull(id, "Course id cannot be null or empty");
    }

    @Test
    void testAddThrowsIfAlreadyExists() {
        String id = "java-8";
        when(course.id()).thenReturn(id);
        when(storage.exists(id)).thenReturn(true);

        assertThrows(EntityAlreadyExistsException.class, () -> repository.add(course));
    }

    @Test
    void testIsUserCourseAdminTrue() {
        String id = "java-8";
        String user = "admin1";
        when(storage.load(id)).thenReturn(Optional.of(course));
        when(course.adminUsernames()).thenReturn(Set.of(user));

        assertTrue(repository.isUserCourseAdmin(user, id));
    }

    @Test
    void testIsUserCourseAdminThrowsIfDoesNotExist() {
        when(storage.load("none")).thenReturn(Optional.empty());
        assertThrows(EntityDoesNotExistException.class, () -> repository.isUserCourseAdmin("user", "none"));
    }

    @Test
    void testGetCourseByNameAndJoinStringSuccess() {
        String name = "Java";
        String joinStr = "secret";
        String fullId = "Java_1";
        when(storage.findAllIdsByPrefix("Java_")).thenReturn(List.of(fullId));
        when(storage.load(fullId)).thenReturn(Optional.of(course));
        when(course.joinString()).thenReturn(joinStr);

        Course result = repository.getCourseByNameAndJoinString(name, joinStr);

        assertEquals(course, result);
    }

    @Test
    void testGetCourseByNameAndJoinStringThrowsWhenCredentialsInvalid() {
        String name = "Java";
        when(storage.findAllIdsByPrefix("Java_")).thenReturn(List.of());

        assertThrows(EntityDoesNotExistException.class, () -> repository.getCourseByNameAndJoinString(name, "wrong"));
    }

    @Test
    void testGetCourseByNameAndJoinStringSkipsNullCourse() {
        String name = "Java";
        String fullId = "Java_1";
        when(storage.findAllIdsByPrefix("Java_")).thenReturn(List.of(fullId));
        when(storage.load(fullId)).thenReturn(Optional.empty());

        assertThrows(EntityDoesNotExistException.class, () -> repository.getCourseByNameAndJoinString(name, "secret"));
    }

    @Test
    void testEnrollStudent() {
        String user = "s1";
        when(course.id()).thenReturn("c1");

        repository.enrollStudent(course, user);

        verify(course).addStudent(user);
        verify(storage).save("c1", course);
    }

    @Test
    void testRemoveStudent() {
        String user = "s1";
        when(course.id()).thenReturn("c1");

        repository.removeStudent(course, user);

        verify(course).removeStudent(user);
        verify(storage).save("c1", course);
    }

    @Test
    void testAddAdmin() {
        String user = "a1";
        when(course.id()).thenReturn("c1");

        repository.addAdmin(course, user);

        verify(course).addAdmin(user);
        verify(storage).save("c1", course);
    }

    @Test
    void testAddAssignment() {
        String asgnId = "asgn1";
        when(course.id()).thenReturn("c1");

        repository.addAssignment(course, asgnId);

        verify(course).addAssignment(asgnId);
        verify(storage).save("c1", course);
    }

    @Test
    void testRemoveAssignment() {
        String asgnId = "asgn1";
        when(course.id()).thenReturn("c1");

        repository.removeAssignment(course, asgnId);

        verify(course).removeAssignment(asgnId);
        verify(storage).save("c1", course);
    }

    @Test
    void testRemove() {
        String id = "c1";
        repository.remove(id);

        verify(storage).delete(id);
        verify(validationService).throwIfNullOrEmpty(id, "Course id cannot be null or empty");
    }
}
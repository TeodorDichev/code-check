package bg.sofia.uni.fmi.mjt.code.check.server.entities;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserTest {
    private User user;
    private final String courseName = "Java";

    @BeforeEach
    void setUp() {
        user = new User("testUser", "hashedPass");
    }

    @Test
    void testGetAdminCourseIdFromNameFound() {
        String courseId = "Java_admin1_2026-02-05";
        user.administerCourse(courseId);
        user.administerCourse("OtherCourse_admin1_2026");

        String result = user.getAdminCourseIdFromName(courseName);

        assertEquals(courseId, result,
                "Should return the full course ID when the prefix matches the course name");
    }

    @Test
    void testGetAdminCourseIdFromNameNotFound() {
        user.administerCourse("Python_admin1_2026");

        String result = user.getAdminCourseIdFromName(courseName);

        assertNull(result,
                "Should return null when no administered course starts with the given name");
    }

    @Test
    void testGetEnrollCourseIdFromNameFound() {
        String enrolledId = "Java_student1_2026";
        user.enrollInCourse(enrolledId);

        String result = user.getEnrollCourseIdFromName(courseName);

        assertEquals(enrolledId, result,
                "Should return the full enrolled course ID when the prefix matches");
    }

    @Test
    void testGetEnrollCourseIdFromNameNotFound() {
        user.enrollInCourse("Math_student1_2026");

        String result = user.getEnrollCourseIdFromName(courseName);

        assertNull(result,
                "Should return null when no enrolled course starts with the given name");
    }

    @Test
    void testCoursesNamesParsing() {
        user.administerCourse("Lab1_Misho_Dnes");
        user.administerCourse("Lab2_Misho_Dnes");
        Set<String> names = user.administeredCoursesNames();

        assertEquals(2, names.size(), "Should have exactly 2 assignment names");
        assertTrue(names.contains("Lab1"), "Assignment names should contain 'Lab1'");
        assertTrue(names.contains("Lab2"), "Assignment names should contain 'Lab2'");
    }
}
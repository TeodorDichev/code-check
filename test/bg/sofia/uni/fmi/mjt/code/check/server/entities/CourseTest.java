package bg.sofia.uni.fmi.mjt.code.check.server.entities;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CourseTest {
    private Course course;

    @BeforeEach
    void setUp() {
        String joinString = "hex-123";
        String courseName = "Java";
        String courseId = "Java_admin_01-01-2026";
        course = new Course(
                courseId,
                courseName,
                joinString,
                new HashSet<>(Set.of("Lab1_id1", "Lab2_id2")),
                new HashSet<>(Set.of("admin1")),
                new HashSet<>(Set.of("student1"))
        );
    }

    @Test
    void testAssignmentNamesParsing() {
        Set<String> names = course.assignmentNames();

        assertEquals(2, names.size(), "Should have exactly 2 assignment names");
        assertTrue(names.contains("Lab1"), "Assignment names should contain 'Lab1'");
        assertTrue(names.contains("Lab2"), "Assignment names should contain 'Lab2'");
    }

    @Test
    void testGetAssignmentIdByNameFound() {
        String id = course.getAssignmentIdByName("Lab1");
        assertEquals("Lab1_id1", id, "Should return the full ID for the matching assignment name");
    }

    @Test
    void testGetAssignmentIdByNameNotFound() {
        assertNull(course.getAssignmentIdByName("NonExistent"),
                "Should return null when assignment name does not exist");
    }

    @Test
    void testCreateIdFormat() {
        String generatedId = Course.createId("Math", "creator");

        // Format is %s_%s_%s (Name_User_Date)
        assertNotNull(generatedId, "Generated ID should not be null");
        assertTrue(generatedId.startsWith("Math_creator_"),
                "Generated ID should start with the course name and creator username");
    }
}
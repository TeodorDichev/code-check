package bg.sofia.uni.fmi.mjt.code.check.server.models;

import bg.sofia.uni.fmi.mjt.code.check.server.entities.Course;

import java.util.Set;

public record ListCoursesModel(Set<Course> administeredCourses, Set<Course> enrolledCourses) {
}

package bg.sofia.uni.fmi.mjt.code.check.server.models;

import bg.sofia.uni.fmi.mjt.code.check.server.entities.Assignment;
import bg.sofia.uni.fmi.mjt.code.check.server.entities.Submission;

public record ListStudentAssignmentModel(Assignment assignment, Submission submission) {
}

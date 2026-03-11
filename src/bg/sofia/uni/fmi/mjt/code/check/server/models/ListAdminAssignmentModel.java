package bg.sofia.uni.fmi.mjt.code.check.server.models;

import bg.sofia.uni.fmi.mjt.code.check.server.entities.Assignment;

public record ListAdminAssignmentModel(Assignment assignment, int numberOfSubmission) {
}

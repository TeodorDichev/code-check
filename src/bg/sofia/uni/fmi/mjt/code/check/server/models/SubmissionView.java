package bg.sofia.uni.fmi.mjt.code.check.server.models;

import java.util.List;
import java.util.Map;

public record SubmissionView(
        String id,
        String submittedBy,
        String submittedOn,
        Double grade,
        String comment,
        List<String> fileNames,
        Map<String, String> fileComments
) {

}

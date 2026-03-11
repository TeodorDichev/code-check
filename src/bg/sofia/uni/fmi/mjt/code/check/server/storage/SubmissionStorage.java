package bg.sofia.uni.fmi.mjt.code.check.server.storage;

import bg.sofia.uni.fmi.mjt.code.check.server.entities.Submission;
import com.google.gson.Gson;
import java.nio.file.Path;

public class SubmissionStorage extends AbstractFileStorage<Submission> {
    private static final String SUBMISSIONS_DIR = "submissions";

    public SubmissionStorage(Path dataDir, Gson gson) {
        super(dataDir.resolve(SUBMISSIONS_DIR), gson, Submission.class);
    }
}
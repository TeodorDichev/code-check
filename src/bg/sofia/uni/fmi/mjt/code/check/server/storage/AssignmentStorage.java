package bg.sofia.uni.fmi.mjt.code.check.server.storage;

import bg.sofia.uni.fmi.mjt.code.check.server.entities.Assignment;
import com.google.gson.Gson;
import java.nio.file.Path;

public class AssignmentStorage extends AbstractFileStorage<Assignment> {
    private static final String ASSIGNMENT_DIR = "assignments";

    public AssignmentStorage(Path dataDir, Gson gson) {
        super(dataDir.resolve(ASSIGNMENT_DIR), gson, Assignment.class);
    }
}
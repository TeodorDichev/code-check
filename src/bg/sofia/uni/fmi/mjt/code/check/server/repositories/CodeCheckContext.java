package bg.sofia.uni.fmi.mjt.code.check.server.repositories;

import bg.sofia.uni.fmi.mjt.code.check.server.services.CompilerService;
import bg.sofia.uni.fmi.mjt.code.check.server.services.PasswordService;
import bg.sofia.uni.fmi.mjt.code.check.server.services.UploadsService;
import bg.sofia.uni.fmi.mjt.code.check.server.services.ValidationService;
import bg.sofia.uni.fmi.mjt.code.check.server.storage.AssignmentStorage;
import bg.sofia.uni.fmi.mjt.code.check.server.storage.CourseStorage;
import bg.sofia.uni.fmi.mjt.code.check.server.storage.SubmissionStorage;
import bg.sofia.uni.fmi.mjt.code.check.server.storage.UserStorage;
import bg.sofia.uni.fmi.mjt.code.check.server.utils.Nullable;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public class CodeCheckContext {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final UploadsService uploadsService;
    private final CompilerService compilerService;
    private final PasswordService passwordService;
    private final ValidationService validationService;

    private CodeCheckContext(Builder builder) {
        this.userRepository = builder.userRepository;
        this.courseRepository = builder.courseRepository;
        this.assignmentRepository = builder.assignmentRepository;
        this.submissionRepository = builder.submissionRepository;
        this.uploadsService = builder.uploadsService;
        this.compilerService = builder.compilerService;
        this.passwordService = builder.passwordService;
        this.validationService = builder.validationService;
    }

    public static Builder builder() {
        return new Builder();
    }

    public UserRepository userRepository() {
        return userRepository;
    }

    public CourseRepository courseRepository() {
        return courseRepository;
    }

    public AssignmentRepository assignmentRepository() {
        return assignmentRepository;
    }

    public SubmissionRepository submissionRepository() {
        return submissionRepository;
    }

    public UploadsService uploadsService() {
        return uploadsService;
    }

    public CompilerService compilerService() {
        return compilerService;
    }

    public PasswordService passwordService() {
        return passwordService;
    }

    public ValidationService validationService() {
        return validationService;
    }

    public static class Builder {

        private Path dataRoot;
        private Gson gson = new GsonBuilder().setPrettyPrinting().create();

        private UserRepository userRepository;
        private CourseRepository courseRepository;
        private AssignmentRepository assignmentRepository;
        private SubmissionRepository submissionRepository;
        private UploadsService uploadsService;
        private CompilerService compilerService;
        private PasswordService passwordService;
        private ValidationService validationService;

        public Builder dataRoot(Path dataRoot) {
            this.dataRoot = dataRoot;
            return this;
        }

        public Builder gson(Gson gson) {
            this.gson = gson;
            return this;
        }

        public Builder userRepository(UserRepository repo) {
            this.userRepository = repo;
            return this;
        }

        public Builder courseRepository(CourseRepository repo) {
            this.courseRepository = repo;
            return this;
        }

        public Builder assignmentRepository(AssignmentRepository repo) {
            this.assignmentRepository = repo;
            return this;
        }

        public Builder submissionRepository(SubmissionRepository repo) {
            this.submissionRepository = repo;
            return this;
        }

        public Builder uploadsService(UploadsService service) {
            this.uploadsService = service;
            return this;
        }

        public Builder compilerService(CompilerService service) {
            this.compilerService = service;
            return this;
        }

        public Builder passwordService(PasswordService service) {
            this.passwordService = service;
            return this;
        }

        public Builder validationService(ValidationService service) {
            this.validationService = service;
            return this;
        }

        public CodeCheckContext build() {
            Objects.requireNonNull(dataRoot, "dataRoot must be set");
            createDirectory(dataRoot);

            validationService = Nullable.orDefault(validationService,
                    new ValidationService());

            userRepository = Nullable.orDefault(userRepository,
                    new UserRepository(new UserStorage(dataRoot, gson), validationService));
            courseRepository = Nullable.orDefault(courseRepository,
                    new CourseRepository(new CourseStorage(dataRoot, gson), validationService));
            assignmentRepository = Nullable.orDefault(assignmentRepository,
                    new AssignmentRepository(new AssignmentStorage(dataRoot, gson), validationService));
            submissionRepository = Nullable.orDefault(submissionRepository,
                    new SubmissionRepository(new SubmissionStorage(dataRoot, gson), validationService));
            uploadsService = Nullable.orDefault(uploadsService,
                    new UploadsService(dataRoot));
            compilerService = Nullable.orDefault(compilerService,
                    new CompilerService(validationService));
            passwordService = Nullable.orDefault(passwordService,
                    new PasswordService());

            return new CodeCheckContext(this);
        }

        private void createDirectory(Path path) {
            try {
                Files.createDirectories(path);
            } catch (IOException e) {
                throw new RuntimeException("Could not initialize directory: " + path, e);
            }
        }
    }
}
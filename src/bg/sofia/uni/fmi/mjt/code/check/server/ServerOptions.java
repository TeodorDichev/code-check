package bg.sofia.uni.fmi.mjt.code.check.server;

import bg.sofia.uni.fmi.mjt.code.check.server.repositories.CodeCheckContext;
import bg.sofia.uni.fmi.mjt.code.check.server.services.logging.DefaultLogger;
import bg.sofia.uni.fmi.mjt.code.check.server.services.logging.Logger;
import bg.sofia.uni.fmi.mjt.code.check.server.session.DefaultSessionStore;
import bg.sofia.uni.fmi.mjt.code.check.server.session.SessionStore;
import com.google.gson.Gson;

import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerOptions {

    private final int port;
    private final String host;
    private final int bufferSize;
    private final Gson gson;
    private final ExecutorService executor;
    private final CodeCheckContext context;
    private final Logger logger;
    private final SessionStore sessionStore;
    private final Path dataRoot;

    private ServerOptions(Builder builder) {
        this.port = builder.port;
        this.host = builder.host;
        this.bufferSize = builder.bufferSize;
        this.gson = builder.gson;
        this.executor = builder.executor;
        this.context = builder.context;
        this.logger = builder.logger;
        this.sessionStore = builder.sessionStore;
        this.dataRoot = builder.dataRoot;
    }

    public int port() {
        return port;
    }

    public String host() {
        return host;
    }

    public int bufferSize() {
        return bufferSize;
    }

    public Gson gson() {
        return gson;
    }

    public ExecutorService executor() {
        return executor;
    }

    public CodeCheckContext context() {
        return context;
    }

    public Logger logger() {
        return logger;
    }

    public SessionStore sessionStore() {
        return sessionStore;
    }

    public Path dataRoot() {
        return dataRoot;
    }

    private static final int BUFFER_SIZE = 8192;
    private static final String HOST = "0.0.0.0";

    public static Builder builder(int port) {
        return new Builder(port);
    }

    public static class Builder {
        private final int port;
        private String host = HOST;
        private int bufferSize = BUFFER_SIZE;
        private Gson gson = new Gson();
        private ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        private CodeCheckContext context;
        private Logger logger = DefaultLogger.getDefaultLogger();
        private SessionStore sessionStore = new DefaultSessionStore();
        private Path dataRoot = Path.of("./data");

        private Builder(int port) {
            this.port = port;
        }

        public Builder host(String host) {
            this.host = host;
            return this;
        }

        public Builder bufferSize(int bufferSize) {
            this.bufferSize = bufferSize;
            return this;
        }

        public Builder gson(Gson gson) {
            this.gson = gson;
            return this;
        }

        public Builder executor(ExecutorService executor) {
            this.executor = executor;
            return this;
        }

        public Builder context(CodeCheckContext context) {
            this.context = context;
            return this;
        }

        public Builder logger(Logger logger) {
            this.logger = logger;
            return this;
        }

        public Builder sessionStore(SessionStore sessionStore) {
            this.sessionStore = sessionStore;
            return this;
        }

        public Builder dataRoot(Path dataRoot) {
            this.dataRoot = dataRoot;
            return this;
        }

        public ServerOptions build() {
            return new ServerOptions(this);
        }
    }
}
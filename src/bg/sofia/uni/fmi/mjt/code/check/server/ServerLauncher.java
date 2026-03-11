package bg.sofia.uni.fmi.mjt.code.check.server;

import bg.sofia.uni.fmi.mjt.code.check.Config;
import bg.sofia.uni.fmi.mjt.code.check.server.repositories.CodeCheckContext;
import bg.sofia.uni.fmi.mjt.code.check.server.services.logging.DefaultLogger;
import bg.sofia.uni.fmi.mjt.code.check.server.services.logging.Severity;
import bg.sofia.uni.fmi.mjt.code.check.server.services.logging.providers.DefaultConsoleProvider;
import bg.sofia.uni.fmi.mjt.code.check.server.services.logging.providers.DefaultFileProvider;
import bg.sofia.uni.fmi.mjt.code.check.server.session.DefaultSessionStore;

import java.nio.file.Path;
import java.util.Scanner;
import java.util.concurrent.Executors;

public class ServerLauncher {

    private static final String STOP_COMMAND = "stop";
    private static final int BUFFER_SIZE = 8192;
    private static final String HOST = "0.0.0.0";
    private static final Path DATA_DIR = Path.of("project", "data");

    void main() {
        CodeCheckServer server = new CodeCheckServer(configureServerOptions());

        // Start server in a separate platform thread
        Thread serverThread = Thread.ofPlatform().start(server);
        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                String input = scanner.nextLine().trim();
                if (STOP_COMMAND.equalsIgnoreCase(input)) {
                    server.stop();
                    break;
                }
            }
        }

        // Wait for server thread to finish
        try {
            serverThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private ServerOptions configureServerOptions() {
        return ServerOptions.builder(Config.port())
                .host(HOST)
                .bufferSize(BUFFER_SIZE)
                .context(CodeCheckContext.builder() // with default repositories and services
                        .dataRoot(DATA_DIR)
                        .build())
                .executor(Executors.newVirtualThreadPerTaskExecutor())
                .logger(DefaultLogger
                        .configure()
                        .addConsoleLogging(Severity.INFO)
                        .configureConsoleProvider(new DefaultConsoleProvider())
                        .addFileLogging(Severity.INFO)
                        .configureFileProvider(new DefaultFileProvider())
                        .build())
                .sessionStore(new DefaultSessionStore())
                .dataRoot(DATA_DIR)
                .build();
    }
}
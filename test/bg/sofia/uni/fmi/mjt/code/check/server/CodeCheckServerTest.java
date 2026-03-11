package bg.sofia.uni.fmi.mjt.code.check.server;

import bg.sofia.uni.fmi.mjt.code.check.server.repositories.CodeCheckContext;
import bg.sofia.uni.fmi.mjt.code.check.server.services.UploadsService;
import bg.sofia.uni.fmi.mjt.code.check.server.services.logging.Logger;
import bg.sofia.uni.fmi.mjt.code.check.server.session.ConnectionState;
import bg.sofia.uni.fmi.mjt.code.check.server.session.SessionStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;


import java.util.concurrent.ExecutorService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CodeCheckServerTest {

    private CodeCheckServer server;
    private ServerOptions options;

    private ExecutorService mockExecutor;

    /**
     * Note on Mocking Strategy:
     * * We use manual mocking in the @BeforeEach (setUp) method rather than @Mock annotations because:
     * It ensures a 100% "clean slate" for every test by manually re-instantiating
     * mocks, preventing potential state leakage or "strange errors" common with JDK NIO resources.
     * It avoids the "magic" of @InjectMocks, which can fail or behave unpredictably
     * with complex constructors or multiple dependencies.
     * It allows us to mock only what is necessary for the current test context,
     * avoiding the need to stub unused deep dependencies required by annotation-based injection.
     * * and I don't get errors ;)
     */
    @BeforeEach
    void setUp() {
        Logger mockLogger = mock(Logger.class);
        mockExecutor = mock(ExecutorService.class);
        SessionStore mockSessionStore = mock(SessionStore.class);
        UploadsService mockUploadsService = mock(UploadsService.class);
        CodeCheckContext mockContext = mock(CodeCheckContext.class);

        when(mockContext.uploadsService()).thenReturn(mockUploadsService);

        options = ServerOptions.builder(8080)
                .host("localhost")
                .logger(mockLogger)
                .executor(mockExecutor)
                .sessionStore(mockSessionStore)
                .context(mockContext)
                .bufferSize(1024)
                .build();

        server = new CodeCheckServer(options);
    }

    @Test
    void testServerInitializationStartsWorkers() {
        // We start the server in a separate thread because run() blocks
        Thread serverThread = new Thread(server);
        serverThread.start();

        try {
            // submit worker tasks to the executor
            Thread.sleep(50);
            server.stop();
            serverThread.join(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Verify that the 4 upload workers were submitted to the executor
        verify(mockExecutor, atLeast(4)).submit(any(Runnable.class));
    }

    @Test
    void testConnectionStateInitializationWithCorrectBufferSize() {
        int expectedBufferSize = options.bufferSize(); // 1024

        ConnectionState state = new ConnectionState(expectedBufferSize);

        assertNotNull(state.commandBuffer(), "Command buffer should not be null");
        assertNotNull(state.fileBuffer(), "File buffer should not be null");
        assertEquals(expectedBufferSize, state.commandBuffer().capacity(),
                "Command buffer capacity should match configured buffer size");
        assertEquals(expectedBufferSize, state.fileBuffer().capacity(),
                "File buffer capacity should match configured buffer size");
    }
}
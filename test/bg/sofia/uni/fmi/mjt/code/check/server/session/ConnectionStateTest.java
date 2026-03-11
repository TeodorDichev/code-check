package bg.sofia.uni.fmi.mjt.code.check.server.session;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ConnectionStateTest {

    private ConnectionState state;

    @BeforeEach
    void setUp() {
        int bufferSize = 1024;
        state = new ConnectionState(bufferSize);
    }

    @Test
    void testSetPendingUploadTransitionsState() {
        state.setPendingUpload("submit-assignment course1 task1");

        assertTrue(state.isAwaitingFileUpload(), "State should be awaiting file after pending upload is set");
        assertEquals("submit-assignment course1 task1", state.getPendingCommand());
    }

    @Test
    void testClearPendingUploadResetsState() {
        state.setPendingUpload("command");
        state.clearPendingUpload();

        assertFalse(state.isAwaitingFileUpload(), "State should no longer be awaiting after clear");
        assertNull(state.getPendingCommand(), "Pending command should be nullified");
    }

    @Test
    void testStartFileUploadSetsActiveReceivingState() {
        FileChannel mockChannel = mock(FileChannel.class);
        Path path = Path.of("test.zip");

        state.startFileUpload(mockChannel, path, 500L);

        assertTrue(state.isReceivingFile(), "Should be in receiving mode");
        assertEquals(500L, state.getRemainingBytes(), "Remaining bytes should equal total size initially");
    }

    @Test
    void testAddBytesReceivedDecrementsRemaining() {
        state.startFileUpload(mock(FileChannel.class), Path.of("test.zip"), 1000L);

        state.addBytesReceived(400L);

        assertEquals(600L, state.getRemainingBytes(), "Remaining bytes should decrease by amount received");
    }

    @Test
    void testAddBytesReceivedCapsAtExpectedSize() {
        state.startFileUpload(mock(FileChannel.class), Path.of("test.zip"), 100L);

        state.addBytesReceived(150L); // More than expected

        assertEquals(0L, state.getRemainingBytes(), "Remaining bytes should not go negative");
        assertTrue(state.isUploadComplete(), "Upload should be considered complete if bytes received >= expected");
    }

    @Test
    void testIsUploadCompleteOnlyReturnsTrueWhenReceivingAndFinished() {
        state.startFileUpload(mock(FileChannel.class), Path.of("test.zip"), 100L);
        assertFalse(state.isUploadComplete(), "Should not be complete yet");

        state.addBytesReceived(100L);
        assertTrue(state.isUploadComplete(), "Should be complete now");
    }

    // https://www.baeldung.com/junit5-assertall-vs-multiple-assertions
    @Test
    void testClearUploadClosesChannelAndResetsAllFields() throws IOException {
        FileChannel mockChannel = mock(FileChannel.class);
        state.setPendingUpload("cmd");
        state.startFileUpload(mockChannel, Path.of("test.zip"), 100L);

        state.clearUpload();

        assertAll("Verify total reset",
                () -> assertFalse(state.isReceivingFile()),
                () -> assertFalse(state.isAwaitingFileUpload()),
                () -> assertNull(state.getPendingCommand()),
                () -> assertNull(state.getCurrentFilePath()),
                () -> assertNull(state.getCurrentFileChannel()),
                () -> assertEquals(0L, state.getRemainingBytes())
        );
        verify(mockChannel).close();
    }

    @Test
    void testAddBytesReceivedIgnoresNegativeValues() {
        state.startFileUpload(mock(FileChannel.class), Path.of("test.zip"), 100L);

        state.addBytesReceived(-50L); // can it even happen -\_(--)_/-

        assertEquals(100L, state.getRemainingBytes(), "Negative counts should be ignored");
    }
}
package bg.sofia.uni.fmi.mjt.code.check.server.services;

import bg.sofia.uni.fmi.mjt.code.check.server.entities.User;
import bg.sofia.uni.fmi.mjt.code.check.server.exceptions.ConnectionClosedDuringUpload;
import bg.sofia.uni.fmi.mjt.code.check.server.session.ConnectionState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UploadsServiceTest {

    @TempDir
    Path tempDir;

    private UploadsService uploadsService;

    private SocketChannel mockChannel;
    private ConnectionState mockState;
    private User mockUser;

    /**
     * Using programmatic mocks for these to ensure fresh state if needed
     * This Mocking strategy is better explained in server tests
     */
    @BeforeEach
    void setUp() {
        uploadsService = new UploadsService(tempDir);
        mockChannel = mock(SocketChannel.class);
        mockState = mock(ConnectionState.class);
        mockUser = mock(User.class);
    }

    @Test
    void testIsFileUploadCommandReturnsTrueForSubmit() {
        assertTrue(uploadsService.isFileUploadCommand("submit-assignment course task path"));
    }

    @Test
    void testIsFileUploadCommandReturnsFalseForOther() {
        assertFalse(uploadsService.isFileUploadCommand("login user pass"));
    }

    @Test
    void testStartUploadInitializesFileState() throws IOException {
        String fileName = "solution.zip";
        String command = "submit-assignment course1 lab1 path";

        // Protocol: int (nameLength), byte[] (name), int (size)
        ByteBuffer data = ByteBuffer.allocate(100);
        data.putInt(fileName.length());
        data.put(fileName.getBytes());
        data.putInt(1024);
        data.flip();

        when(mockUser.username()).thenReturn("tester");
        when(mockChannel.read(any(ByteBuffer.class))).thenAnswer(invocation -> {
            ByteBuffer buffer = invocation.getArgument(0);
            int count = Math.min(buffer.remaining(), data.remaining());
            buffer.put(data.array(), data.position(), count);
            data.position(data.position() + count);
            return count;
        });

        uploadsService.startUpload(mockChannel, mockState, mockUser, command);

        verify(mockState).startFileUpload(any(FileChannel.class), any(Path.class), anyLong());
        verify(mockState).setOriginalCommand(command);
    }

    @Test
    void testProcessIncrementalUploadThrowsWhenConnectionClosed() throws IOException {
        when(mockState.fileBuffer()).thenReturn(ByteBuffer.allocate(10));
        when(mockChannel.read(any(ByteBuffer.class))).thenReturn(-1);

        assertThrows(ConnectionClosedDuringUpload.class,
                () -> uploadsService.processIncrementalUpload(mockChannel, mockState));
    }

    @Test
    void testBuildFinalResultExtractsOnlyJavaFiles() throws IOException {
        Path archivePath = createTestZip(List.of("Solution.java", "ignore.txt"));
        when(mockState.getCurrentFilePath()).thenReturn(archivePath);

        uploadsService.buildFinalResult(mockState);

        assertTrue(Files.exists(tempDir.resolve("Solution.java")), "Java file should be extracted");
        assertFalse(Files.exists(tempDir.resolve("ignore.txt")), "Non-java files should not be extracted");
    }

    @Test
    void testBuildFinalResultDeletesArchiveAfterProcessing() throws IOException {
        Path archivePath = createTestZip(List.of("Solution.java"));
        when(mockState.getCurrentFilePath()).thenReturn(archivePath);

        uploadsService.buildFinalResult(mockState);

        assertFalse(Files.exists(archivePath), "The source ZIP archive should be deleted from the system");
    }

    @Test
    void testProcessIncrementalUploadReturnsStatusFromState() throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(10).put("data".getBytes());
        buffer.flip();

        when(mockState.fileBuffer()).thenReturn(buffer);
        when(mockState.getRemainingBytes()).thenReturn(10L);
        when(mockState.getCurrentFileChannel()).thenReturn(mock(FileChannel.class));
        when(mockState.isUploadComplete()).thenReturn(true);
        when(mockChannel.read(any(ByteBuffer.class))).thenReturn(0);

        boolean isDone = uploadsService.processIncrementalUpload(mockChannel, mockState);

        assertTrue(isDone);
    }

    private Path createTestZip(List<String> fileNames) throws IOException {
        Path archivePath = tempDir.resolve("test_" + System.nanoTime() + ".zip");
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(archivePath))) {
            for (String fileName : fileNames) {
                zos.putNextEntry(new ZipEntry(fileName));
                zos.write("content".getBytes());
                zos.closeEntry();
            }
        }
        return archivePath;
    }
}
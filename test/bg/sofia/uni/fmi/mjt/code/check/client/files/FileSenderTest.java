package bg.sofia.uni.fmi.mjt.code.check.client.files;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FileSenderTest {

    @TempDir
    Path tempDir;

    @Test
    void testIsSubmitCommand() {
        String command = "submit-assignment MJT Lab01 path/to/file";
        assertTrue(FileSender.isSubmitCommand(command),
                "Should recognize a valid submit-assignment command");
    }

    @Test
    void testExtractPathWithQuotes() {
        String command = "submit-assignment MJT \"Lab 01\" \"C:/My Path/Solution\"";
        Path expected = Path.of("C:/My Path/Solution");

        assertEquals(expected, FileSender.extractPath(command),
                "Should correctly extract the path even if it contains spaces and is quoted");
    }

    @Test
    void testExtractPathThrowsOnInsufficientArgs() {
        String tooShort = "submit-assignment part1 part2";
        assertThrows(IllegalArgumentException.class, () -> FileSender.extractPath(tooShort),
                "Should throw when the command is missing the path argument at index 3");
    }

    @Test
    void testSendPathProtocolAndCleanup() throws IOException {
        SocketChannel mockChannel = mock(SocketChannel.class);

        // Updating buffer position to prevent infinite while(buffer.hasRemaining()) loops
        when(mockChannel.write(any(ByteBuffer.class))).thenAnswer(invocation -> {
            ByteBuffer buf = invocation.getArgument(0);
            int written = buf.remaining();
            buf.position(buf.limit()); // basically simulates reading all
            return written;
        });

        Path root = tempDir.resolve("testFile.txt");
        Files.writeString(root, "Small content");

        assertDoesNotThrow(() -> FileSender.sendPath(mockChannel, root),
                "The sending process should complete without throwing exceptions");

        // Check for minimum expected interactions write len,command,file...
        verify(mockChannel, atLeast(4)).write(any(ByteBuffer.class));

        try (Stream<Path> files = Files.list(tempDir)) {
            boolean tempFilesExist = files.anyMatch(p -> p.getFileName().toString().startsWith("upload_"));
            assertFalse(tempFilesExist,
                    "Temporary zip file should be deleted after the method completes to prevent disk clutter");
        }
    }
}
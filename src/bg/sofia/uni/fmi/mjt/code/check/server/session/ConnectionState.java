package bg.sofia.uni.fmi.mjt.code.check.server.session;

import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;

/**
 * One instance per client connection (SocketChannel). Holds all mutable per-connection state, such as:
 * <p>
 *  Command buffer for reading incremental commands.
 *  File upload state: currentFileChannel, currentFilePath, expectedFileSize, bytesReceived
 *  Original command for the upload (so you know which course/assignment to associate the file with).
 *  Thread-safe access via synchronization.
 *  Lives attached to the SelectionKey (key.attach(connectionState))
 * </p>
 * <p>
 *  Command buffer holds incoming bytes for text-based commands from the client (e.g., login, submit-assignment ...).
 *  File buffer holds incoming bytes for binary file data (ZIP archives).
 *  Keeps file transfers independent of command processing.
 * </p>
 * <p>
 *  Upload flow: Command validated → awaitingFileUpload=true → Client sends file → startFileUpload → receivingFile=true
 * </p>
 * */
public class ConnectionState {
    private final Object lock = new Object();

    // File upload state
    private boolean receivingFile;
    private FileChannel currentFileChannel;
    private Path currentFilePath;
    private long expectedFileSize;
    private long bytesReceived;
    private String originalCommand;

    // Pre-upload validation state
    private boolean awaitingFileUpload;  // True after command validation passes
    private String pendingCommand;       // The validated command waiting for file data

    private final ByteBuffer commandBuffer;
    private final ByteBuffer fileBuffer;

    public ConnectionState(int bufferSize) {
        this.commandBuffer = ByteBuffer.allocate(bufferSize);
        this.fileBuffer = ByteBuffer.allocate(bufferSize);
        this.receivingFile = false;
        this.awaitingFileUpload = false;
    }

    public ByteBuffer commandBuffer() {
        return commandBuffer;
    }

    public ByteBuffer fileBuffer() {
        return fileBuffer;
    }

    /**
     * Called when command validation passes and we're ready to receive file data.
     * This sets a flag so the next read from the socket knows to expect binary file data.
     */
    public void setPendingUpload(String command) {
        synchronized (lock) {
            this.awaitingFileUpload = true;
            this.pendingCommand = command;
        }
    }

    /**
     * Check if we're waiting for the client to send file data after successful validation.
     */
    public boolean isAwaitingFileUpload() {
        synchronized (lock) {
            return awaitingFileUpload;
        }
    }

    /**
     * Get the command that was validated and is now waiting for file upload.
     */
    public String getPendingCommand() {
        synchronized (lock) {
            return pendingCommand;
        }
    }

    /**
     * Clear the pending upload state (called when startFileUpload is triggered).
     */
    public void clearPendingUpload() {
        synchronized (lock) {
            this.awaitingFileUpload = false;
            this.pendingCommand = null;
        }
    }

    /**
     * Called when binary file data starts arriving from the client.
     * Transitions from "awaiting" state to "receiving" state.
     */
    public void startFileUpload(FileChannel channel, Path path, long size) {
        synchronized (lock) {
            this.currentFileChannel = channel;
            this.currentFilePath = path;
            this.expectedFileSize = Math.max(size, 0L);
            this.bytesReceived = 0L;
            this.receivingFile = true;
            // Keep awaitingFileUpload as-is; will be cleared by clearPendingUpload()
        }
    }

    public void addBytesReceived(long count) {
        synchronized (lock) {
            if (count > 0) {
                bytesReceived += count;
                if (bytesReceived > expectedFileSize) {
                    bytesReceived = expectedFileSize;
                }
            }
        }
    }

    public boolean isUploadComplete() {
        synchronized (lock) {
            return receivingFile && bytesReceived >= expectedFileSize;
        }
    }

    /**
     * Clears both the active file upload state AND any pending upload state.
     * Called when upload completes, fails, or connection closes.
     */
    public void clearUpload() {
        synchronized (lock) {
            receivingFile = false;
            expectedFileSize = 0L;
            bytesReceived = 0L;
            currentFilePath = null;
            originalCommand = null;

            // Also clear pending state
            awaitingFileUpload = false;
            pendingCommand = null;

            if (currentFileChannel != null) {
                try {
                    currentFileChannel.close();
                } catch (Exception ignored) {
                    // ignored
                }
                currentFileChannel = null;
            }
        }
    }

    public boolean isReceivingFile() {
        synchronized (lock) {
            return receivingFile;
        }
    }

    public long getRemainingBytes() {
        synchronized (lock) {
            return expectedFileSize - bytesReceived;
        }
    }

    public Path getCurrentFilePath() {
        synchronized (lock) {
            return currentFilePath;
        }
    }

    public FileChannel getCurrentFileChannel() {
        synchronized (lock) {
            return currentFileChannel;
        }
    }

    public void setOriginalCommand(String command) {
        synchronized (lock) {
            this.originalCommand = command;
        }
    }

    public String getOriginalCommand() {
        synchronized (lock) {
            return originalCommand;
        }
    }

    public void setReceivingFile(boolean b) {
        synchronized (lock) {
            receivingFile = b;
        }
    }

    public void setAwaitingFileUpload(boolean b) {
        synchronized (lock) {
            awaitingFileUpload = b;
        }
    }
}
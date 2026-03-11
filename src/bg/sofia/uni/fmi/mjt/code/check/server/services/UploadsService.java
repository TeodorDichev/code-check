package bg.sofia.uni.fmi.mjt.code.check.server.services;

import bg.sofia.uni.fmi.mjt.code.check.server.commands.CommandParser;
import bg.sofia.uni.fmi.mjt.code.check.server.commands.assignment.SubmitAssignmentCommand;
import bg.sofia.uni.fmi.mjt.code.check.server.entities.User;
import bg.sofia.uni.fmi.mjt.code.check.server.exceptions.ConnectionClosedDuringUpload;
import bg.sofia.uni.fmi.mjt.code.check.server.session.ConnectionState;
import bg.sofia.uni.fmi.mjt.code.check.server.session.UploadTask;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.function.BiConsumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class UploadsService {
    private static final String UPLOADS_DIR = "uploads";
    private static final int INT_BYTES = Integer.BYTES;
    private final Path uploadsRoot;

    public UploadsService(Path dataDir) {
        this.uploadsRoot = dataDir.resolve(UPLOADS_DIR);
    }

    public Path uploadsRoot() {
        return uploadsRoot;
    }

    private static final String SUBMIT_ASSIGNMENT = "submit-assignment";
    public boolean isFileUploadCommand(String command) {
        return command != null && CommandParser.getCommandString(command).equals(SUBMIT_ASSIGNMENT);
    }

    private static final String ACCEPTABLE_ARCHIVE_EXTENSION = ".zip";
    public void startUpload(SocketChannel channel, ConnectionState data, User user, String command) throws IOException {
        int nameLength = readInt(channel);
        byte[] nameBytes = new byte[nameLength];
        readFully(channel, ByteBuffer.wrap(nameBytes));
        String archiveName = new String(nameBytes, StandardCharsets.UTF_8).trim();
        if (!archiveName.endsWith(ACCEPTABLE_ARCHIVE_EXTENSION)) {
            archiveName += ACCEPTABLE_ARCHIVE_EXTENSION;
        }

        long archiveSize = readInt(channel);
        Path finalPath = uploadsRoot.resolve(
                SubmitAssignmentCommand.getDestinationFolder(user.username(), command, archiveName));

        clearUserDirectory(finalPath.getParent());
        Files.createDirectories(finalPath.getParent());

        FileChannel fileChannel = FileChannel.open(finalPath,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE);

        data.startFileUpload(fileChannel, finalPath, archiveSize);
        data.setOriginalCommand(command);
    }

    private void clearUserDirectory(Path userFolder) throws IOException {
        if (Files.exists(userFolder)) {
            try (var stream = Files.walk(userFolder)) {
                stream.sorted(java.util.Comparator.reverseOrder())
                        .forEach(p -> {
                            try {
                                Files.delete(p);
                            } catch (IOException e) {
                                // ignored
                            }
                        });
            }
        }
    }

    private static final int CONNECTION_CLOSED_CODE = -1;
    public boolean processIncrementalUpload(SocketChannel channel, ConnectionState data) throws IOException {
        ByteBuffer buffer = data.fileBuffer();
        int bytesRead = channel.read(buffer);
        if (bytesRead == CONNECTION_CLOSED_CODE) {
            throw new ConnectionClosedDuringUpload("Connection was closed by the client");
        }

        buffer.flip();
        int toWrite = (int) Math.min(buffer.remaining(), data.getRemainingBytes());
        buffer.limit(buffer.position() + toWrite);
        data.getCurrentFileChannel().write(buffer);
        data.addBytesReceived(toWrite);
        buffer.compact();
        return data.isUploadComplete();
    }

    private static final String ACCEPTABLE_FILE_EXTENSION = ".java";
    public String buildFinalResult(ConnectionState data) {
        Path archivePath = data.getCurrentFilePath();
        String resultSummary;

        resultSummary = extractJavaFiles(archivePath);
        deleteFileOrDirectory(archivePath);

        return resultSummary;
    }

    private String extractJavaFiles(Path archivePath) {
        StringBuilder result = new StringBuilder();
        try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(archivePath))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                String name = entry.getName();

                if (!name.endsWith(ACCEPTABLE_FILE_EXTENSION)) {
                    result.append("skipped (not .java): ").append(name);
                    zip.closeEntry();
                    continue;
                }

                Path target = archivePath.getParent().resolve(name);
                Files.createDirectories(target.getParent());
                Files.copy(zip, target, StandardCopyOption.REPLACE_EXISTING);

                zip.closeEntry();
            }
        } catch (IOException e) {
            return "Error extracting archive: " + e.getMessage();
        }
        result.append("Finished file uploading");
        return result.toString();
    }

    private void deleteFileOrDirectory(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            // ignored
        }
    }

    private int readInt(SocketChannel channel) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(INT_BYTES);
        readFully(channel, buf);
        buf.flip();
        return buf.getInt();
    }

    private void readFully(SocketChannel channel, ByteBuffer buffer) throws IOException {
        while (buffer.hasRemaining()) {
            int read = channel.read(buffer);
            if (read == CONNECTION_CLOSED_CODE) {
                throw new ConnectionClosedDuringUpload("Client closed connection");
            }
        }
    }

    /**
     * The BiConsumer<T, U> is a built-in functional interface that represents
     * an operation that accepts two input arguments and returns no result.
     * The consume method doesn't need to know how to write to a socket/process command
     * it just knows that when it’s done, it should trigger the behavior defined in the callback
     * Once the result string is ready, the consume method "calls back" to the server logic
     * */
    private static final String POST_PREFIX = "post-";
    public void consume(UploadTask task, BiConsumer<SocketChannel, String> callback) {
        String result = POST_PREFIX + task.command() + " " + buildFinalResult(task.data());
        callback.accept(task.channel(), result);
    }
}
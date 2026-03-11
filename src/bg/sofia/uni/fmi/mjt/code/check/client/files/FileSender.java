package bg.sofia.uni.fmi.mjt.code.check.client.files;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class FileSender {
    public static final String SUBMIT_COMMAND = "submit-assignment";
    public static final int BUFFER_SIZE = 8192;
    public static final int INT_BYTES = Integer.BYTES;
    public static final int FILE_PATH_INDEX = 3;

    private static final String UPLOAD_TEMP_FILE_PREFIX = "upload_";
    private static final String ZIP_EXTENSION = ".zip";

    /** Send a directory or file as a single ZIP archive
     * For directories, use parent as base to preserve directory name in zip
     * For single files, use parent as base so file goes to zip root
     */
    public static void sendPath(SocketChannel channel, Path root) throws IOException {
        Path tempZip = Files.createTempFile(UPLOAD_TEMP_FILE_PREFIX, ZIP_EXTENSION);
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(tempZip))) {
            if (Files.isDirectory(root)) {
                zipRecursive(root, root, zos);
            } else {
                Path base = root.getParent() != null ? root.getParent() : root.toAbsolutePath().getParent();
                if (base == null) {
                    base = root.getFileSystem().getPath("").toAbsolutePath();
                }
                zipRecursive(root, base, zos);
            }
        }
        byte[] nameBytes = tempZip.getFileName().toString().getBytes();
        sendInt(channel, nameBytes.length);
        channel.write(ByteBuffer.wrap(nameBytes));
        long fileSize = Files.size(tempZip);
        sendInt(channel, (int) fileSize);
        try (FileChannel fileChannel = FileChannel.open(tempZip, StandardOpenOption.READ)) {
            ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
            while (fileChannel.read(buffer) > 0) {
                buffer.flip();
                while (buffer.hasRemaining()) {
                    channel.write(buffer);
                }
                buffer.clear();
            }
        }
        Files.deleteIfExists(tempZip);
    }

    /** relativize() computes the relative path from base to source
     * e.g., base=/home/user, source=/home/user/hw1/Main.java → hw1/Main.java
     * This gives us the entry name for the zip file without the full absolute path
     * tried to do it with string manipulation but had some bugs
     */
    private static final String SLASH = "/";
    private static void zipRecursive(Path source, Path base, ZipOutputStream zos) throws IOException {
        if (Files.isDirectory(source)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(source)) {
                for (Path entry : stream) {
                    zipRecursive(entry, base, zos);
                }
            }
        } else {
            Path relativePath = base.relativize(source);
            String entryName = relativePath.toString().replace(File.separator, SLASH);

            ZipEntry entry = new ZipEntry(entryName);
            zos.putNextEntry(entry);
            Files.copy(source, zos);
            zos.closeEntry();
        }
    }

    private static void sendInt(SocketChannel channel, int value) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(INT_BYTES);
        buffer.putInt(value);
        buffer.flip();
        channel.write(buffer);
    }

    public static boolean isSubmitCommand(String command) {
        return command != null && command.startsWith(SUBMIT_COMMAND);
    }

    public static Path extractPath(String command) {
        List<String> parts = splitCommand(command);

        if (parts.size() <= FILE_PATH_INDEX) {
            throw new IllegalArgumentException("Missing file path in command");
        }

        return Path.of(parts.get(FILE_PATH_INDEX));
    }


    private static final Character SEPARATOR = ' ';
    private static final Character QUOTE = '"';
    private static List<String> splitCommand(String input) {
        List<String> tokens = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean insideQuote = false;

        for (char c : input.toCharArray()) {
            if (c == QUOTE) {
                insideQuote = !insideQuote;
                continue;
            }
            if (c == SEPARATOR && !insideQuote) {
                if (!sb.isEmpty()) {
                    tokens.add(sb.toString());
                    sb.setLength(0);
                }
            } else {
                sb.append(c);
            }
        }
        if (!sb.isEmpty()) {
            tokens.add(sb.toString());
        }
        return tokens;
    }
}
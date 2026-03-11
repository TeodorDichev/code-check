package bg.sofia.uni.fmi.mjt.code.check.server;

import bg.sofia.uni.fmi.mjt.code.check.contract.PlainTextResponse;
import bg.sofia.uni.fmi.mjt.code.check.server.commands.CommandExecutor;
import bg.sofia.uni.fmi.mjt.code.check.server.commands.CommandType;
import bg.sofia.uni.fmi.mjt.code.check.server.entities.User;
import bg.sofia.uni.fmi.mjt.code.check.server.exceptions.ConnectionClosedDuringUpload;
import bg.sofia.uni.fmi.mjt.code.check.server.exceptions.UnauthorizedException;
import bg.sofia.uni.fmi.mjt.code.check.contract.ServerResponse;
import bg.sofia.uni.fmi.mjt.code.check.contract.Status;
import bg.sofia.uni.fmi.mjt.code.check.server.services.UploadsService;
import bg.sofia.uni.fmi.mjt.code.check.server.services.logging.Logger;
import bg.sofia.uni.fmi.mjt.code.check.server.session.Session;
import bg.sofia.uni.fmi.mjt.code.check.server.session.SessionStore;
import bg.sofia.uni.fmi.mjt.code.check.server.session.ConnectionState;
import bg.sofia.uni.fmi.mjt.code.check.server.session.UploadTask;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;

public class CodeCheckServer implements Runnable {
    private final ServerOptions options;
    private final ExecutorService executor;
    private final Logger logger;
    private final SessionStore sessionStore;
    private final CommandExecutor commandExecutor;
    private final UploadsService uploadsService;

    /**
     * The blocking queue implements a producer-consumer pattern for file uploads.
     *
     * <p>The Selector thread cannot be blocked; saving a large file directly would cause it to hang.
     * Worker threads act as consumers while the Selector thread acts as the producer.</p>
     *
     * <p>To test concurrency: One client sends a 100MB file while another logs in.
     * The login command should finish before the upload, even if sent later.</p>
     *
     * <p>Test file generation (Windows): {@code fsutil file createnew name.java 100000000}</p>
     *
     * <p>Note: This code evolved through numerous iterations. Check git history for details.
     * AI was used for proof-of-concept validation and formating this comment.</p>
     */
    private final BlockingQueue<UploadTask> uploadQueue = new LinkedBlockingQueue<>();

    private Selector selector;
    private boolean isRunning;

    public CodeCheckServer(ServerOptions options) {
        this.options = options;
        this.logger = options.logger();
        this.executor = options.executor();
        this.sessionStore = options.sessionStore();
        this.uploadsService = options.context().uploadsService();
        this.commandExecutor = CommandExecutor.configure(sessionStore, options.context(), logger);

    }

    @Override
    public void run() {
        startUploadWorkers();
        try (ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
             Selector selector = Selector.open()) {
            configureServerSocket(serverSocketChannel, selector);
            logger.logInfo("Server started on " + InetAddress.getLocalHost().getHostAddress() + ":" + options.port());
            while (isRunning) {
                if (selector.select() == 0) {
                    continue;
                }
                Set<SelectionKey> keys = selector.selectedKeys();
                Iterator<SelectionKey> it = keys.iterator();
                while (it.hasNext()) {
                    SelectionKey key = it.next();
                    it.remove();
                    if (!key.isValid()) {
                        continue;
                    } else if (key.isAcceptable()) {
                        accept(key);
                    } else if (key.isReadable()) {
                        handleRead(key);
                    }
                }
            }
        } catch (IOException e) {
            logger.logError("Server failed", e);
        } finally {
            executor.shutdown();
        }
    }

    public void stop() {
        isRunning = false;
        if (selector != null && selector.isOpen()) {
            selector.wakeup();
        }
        logger.logInfo("Server stopped");
    }

    private void configureServerSocket(ServerSocketChannel channel, Selector selector) throws IOException {
        this.selector = selector;
        channel.bind(new InetSocketAddress(options.host(), options.port()));
        channel.configureBlocking(false);
        channel.register(selector, SelectionKey.OP_ACCEPT);
        isRunning = true;
    }

    private void accept(SelectionKey key) throws IOException {
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        SocketChannel clientChannel = serverChannel.accept();
        if (clientChannel == null) {
            return;
        }

        clientChannel.configureBlocking(false);
        clientChannel.register(selector, SelectionKey.OP_READ, new ConnectionState(options.bufferSize()));
        logger.logInfo("Accepted connection from " + clientChannel.getRemoteAddress());
    }

    private void handleRead(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        ConnectionState state = (ConnectionState) key.attachment();
        try {
            if (state.isReceivingFile()) {
                handleReceivingFile(channel, state);
                return;
            } else if (state.isAwaitingFileUpload()) {
                handleStartingFileUpload(channel, state, state.getPendingCommand());
                return;
            }
            String command = readCommand(channel, state);
            if (uploadsService.isFileUploadCommand(command)) {
                executor.submit(() -> {
                    ServerResponse validationResponse = processCommand(channel, command);
                    if (validationResponse.status() == Status.OK) {
                        try {
                            state.setPendingUpload(command);
                        } catch (Exception e) {
                            logger.logError("Failed to prepare upload", e);
                        }
                    }
                });
            } else {
                executor.submit(() -> processCommand(channel, command));
            }
        } catch (IOException ex) {
            closeChannel(channel, state);
        }
    }

    /**
     * sets the needed flags so when in uploading mode we will go into session.isReceivingFile()
     * next time we select this channel with the selector we will go into the first if
     * occasionally I forgot a regular symbols in the set with forbidden symbols and caught the error here
     * it broke the clients console (only the one send this command). The problem is if the pre-submit command
     * has returned OK then the bytes are sent... This is reading the byter handler. We need to disassociate from
     * this client because most of their messages now are with redundant file info. I wanted to drain the rest of the
     * scent bytes, but what if it was an attack, and it's a petabyte of data.
     * */
    private void handleStartingFileUpload(SocketChannel channel, ConnectionState state, String command) {
        try {
            User user = sessionStore.getUser(channel);
            if (user == null) {
                throw new UnauthorizedException("User in when starting upload was null");
            }
            uploadsService.startUpload(channel, state, user, command);
        } catch (Exception ex) {
            logger.logError("Upload initialization failed. Closing connection to prevent further errors.", ex);
            sendPlainTextResponse(channel, "Upload failed: " + ex.getMessage() + ". Connection closed.");
            closeChannel(channel, state);
        }
        state.clearPendingUpload();
    }

    private void handleReceivingFile(SocketChannel channel, ConnectionState state) throws IOException {
        boolean done;
        try {
            done = uploadsService.processIncrementalUpload(channel, state);
        } catch (ConnectionClosedDuringUpload ce) {
            closeChannel(channel, state);
            return;
        }

        if (done) {
            UploadTask task = new UploadTask(channel, state, state.getOriginalCommand());
            state.setReceivingFile(false);
            state.setAwaitingFileUpload(false);

            if (!uploadQueue.offer(task)) {
                // Only if the queue is full, we clear everything
                state.clearUpload();
                sendResponse(channel, new ServerResponse(
                        CommandType.SUBMIT_ASSIGNMENT,
                        Status.ERROR,
                        "Server busy. Try again later.",
                        sessionStore.getUser(channel),
                        null
                ));
            }
        }
    }

    /**
     * I use buffer.compact() here to preserve any remaining unread
     * bytes in the buffer while preparing it for the next read
     * <a href="https://www.geeksforgeeks.org/java/bytebuffer-compact-method-in-java-with-examples/">link</a>
     * */
    private static final int CONNECTION_CLOSED_CODE = -1;
    private static final int NO_MORE_INPUT_CODE = 0;
    private String readCommand(SocketChannel channel, ConnectionState session) throws IOException {
        ByteBuffer buffer = session.commandBuffer();
        int bytesRead = channel.read(buffer);
        if (bytesRead == CONNECTION_CLOSED_CODE) {
            closeChannel(channel, session);
            return null;
        }

        if (bytesRead == NO_MORE_INPUT_CODE) {
            return null;
        }

        buffer.flip();
        String command = StandardCharsets.UTF_8.decode(buffer).toString().trim();
        buffer.compact();
        return command;
    }

    private void closeChannel(SocketChannel channel, ConnectionState state) {
        try {
            if (channel == null) {
                logger.logInfo("Channel is null and already closed");
                return;
            }

            logger.logInfo("Closing connection for " + channel.getRemoteAddress());

            if (state.isReceivingFile()) {
                state.clearUpload();
            }

            sessionStore.remove(channel);
            if (channel.isOpen()) {
                channel.close();
            }
        } catch (IOException e) {
            logger.logError("Error closing channel", e);
        }
    }

    private ServerResponse processCommand(SocketChannel channel, String command) {
        ServerResponse response;
        try {
            response = commandExecutor.execute(command, new Session(channel, null));
        } catch (Exception e) {
            response = new ServerResponse(CommandType.UNKNOWN, Status.ERROR,
                    "Command processing failed: " + e.getMessage(),
                    sessionStore.getUser(channel), null);
            logger.logError("Processing command failed", e);
        }

        return sendResponse(channel, response);
    }

    private ServerResponse sendResponse(SocketChannel channel, ServerResponse response) {
        try {
            if (response == null) {
                response = new ServerResponse(CommandType.UNKNOWN, Status.ERROR,
                        "Server response was null", null, null);
            }
            String json = options.gson().toJson(response) + System.lineSeparator();
            writeClientOutput(channel, json);
            return response;
        } catch (IOException e) {
            logger.logError("Sending response failed", e);
        }

        return new ServerResponse(CommandType.UNKNOWN, Status.ERROR,
                "Server response was null", null, null);
    }

    private void sendPlainTextResponse(SocketChannel channel, String message) {
        try {
            String json = options.gson().toJson(new PlainTextResponse(message)) + System.lineSeparator();
            writeClientOutput(channel, json);
        } catch (IOException e) {
            logger.logError("Failed to send plain text response", e);
        }
    }

    /**
     * I use ByteBuffer.wrap() here to create a ByteBuffer backed by the existing byte array
     * Creates a ByteBuffer that wraps the array without copying it.
     * The buffer’s position = 0 and limit = array.length automatically.
     * This means it’s ready to be written to the channel immediately.
     * <a href="https://www.geeksforgeeks.org/java/bytebuffer-wrap-method-in-java-with-examples/">link</a>
     * */
    private void writeClientOutput(SocketChannel channel, String output) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(output.getBytes(StandardCharsets.UTF_8));
        while (buffer.hasRemaining()) {
            channel.write(buffer);
        }
    }

    /**
     * Limiting the concurrent uploads with a fixed number. Can be increased.
     * The idea is the same as the lab when we implemented the ArrayBlockingQueue by ourselves
     * It is used only when saving the files to the disk, regular commands do not go through it
     * */
    private static final int UPLOAD_WORKERS = 4;
    private void startUploadWorkers() {
        for (int i = 0; i < UPLOAD_WORKERS; i++) {
            executor.submit(this::uploadWorkerLoop);
        }
    }

    /**
     * Warning due to finishing the loop only with an exception. Okay to ignore.
     * .take() blocks the thread if the queue is empty
     * The thread waits until a new UploadTask is available.
     * This is why selector thread is not blocked
     * This loop should never end that's why we can ignore the warning
     * */
    @SuppressWarnings("InfiniteLoopStatement")
    private void uploadWorkerLoop() {
        while (true) {
            try {
                UploadTask task = uploadQueue.take();
                uploadsService.consume(task, this::processCommand);
            } catch (Exception e) {
                logger.logError("Upload worker failed", e);
            }
        }
    }
}
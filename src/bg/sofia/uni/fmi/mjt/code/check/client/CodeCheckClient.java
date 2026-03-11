package bg.sofia.uni.fmi.mjt.code.check.client;

import bg.sofia.uni.fmi.mjt.code.check.contract.ServerResponse;
import bg.sofia.uni.fmi.mjt.code.check.contract.Status;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Scanner;

import static bg.sofia.uni.fmi.mjt.code.check.client.files.FileSender.extractPath;
import static bg.sofia.uni.fmi.mjt.code.check.client.files.FileSender.isSubmitCommand;
import static bg.sofia.uni.fmi.mjt.code.check.client.files.FileSender.sendPath;

public class CodeCheckClient {

    private final int serverPort;
    private static final String SERVER_HOST = "localhost";
    private static final String EXIT_COMMAND = "exit";
    private static final String NO_RESPONSE_MSG = "The server didn't respond";
    private final Gson gson = new GsonBuilder()
            .disableHtmlEscaping() // escapes symbols like \u0003 which as far as I know are generate by the parsing
            .setPrettyPrinting()
            .create();

    // private static final ResponseFormatter formatter = new ResponseFormatter();

    public CodeCheckClient(int serverPort) {
        this.serverPort = serverPort;
    }

    public void start() {
        try (SocketChannel socketChannel = SocketChannel.open();
             BufferedReader reader = new BufferedReader(Channels.newReader(socketChannel, StandardCharsets.UTF_8));
             PrintWriter writer = new PrintWriter(Channels.newWriter(socketChannel, StandardCharsets.UTF_8), true);
             Scanner scanner = new Scanner(System.in)) {

            socketChannel.connect(new InetSocketAddress(SERVER_HOST, serverPort));
            System.out.println("Welcome to CodeCheck. By T.A.D.");

            handleUserInputLoop(socketChannel, reader, writer, scanner);

        } catch (IOException e) {
            throw new RuntimeException("There is a problem with the network communication", e);
        }
    }

    private void handleUserInputLoop(SocketChannel channel,
                                     BufferedReader reader,
                                     PrintWriter writer,
                                     Scanner scanner) throws IOException {

        while (true) {
            String message = scanner.nextLine().trim();

            if (message.isBlank()) {
                continue;
            }
            if (EXIT_COMMAND.equalsIgnoreCase(message)) {
                break;
            }

            writer.println(message);

            if (isSubmitCommand(message)) {
                handleSubmitAssignment(channel, reader, message);
            } else {
                handleServerResponse(reader);
            }
        }
    }

    private void handleSubmitAssignment(SocketChannel channel, BufferedReader reader, String command)
            throws IOException {
        String rawJsonResponse = reader.readLine();

        if (rawJsonResponse == null) {
            System.out.println(NO_RESPONSE_MSG);
            return;
        }

        ServerResponse response = gson.fromJson(rawJsonResponse, ServerResponse.class);
        System.out.println(gson.toJson(response));

        if (response.status() == Status.OK) {
            System.out.println("Uploading file...");
            Path path = extractPath(command);
            sendPath(channel, path);
            System.out.println("Upload complete. Waiting for processing...");
            handleServerResponse(reader);
        }
    }

    private static final String JSON_NEW_LINE_ESCAPE = "\\n";
    private static final String JSON_CARRIAGE_RETURN_ESCAPE = "\\r";
    private static final String ACTUAL_NEW_LINE = System.lineSeparator();

    private void handleServerResponse(BufferedReader reader) throws IOException {
        String rawJsonResponse = reader.readLine();

        if (rawJsonResponse == null) {
            System.out.println(NO_RESPONSE_MSG);
            return;
        }

        ServerResponse response = gson.fromJson(rawJsonResponse, ServerResponse.class);
        String prettyJson = gson.toJson(response);

        // used mainly in displaying the text from the view file command not on 1 line
        String formattedOutput = prettyJson
                .replace(JSON_CARRIAGE_RETURN_ESCAPE + JSON_NEW_LINE_ESCAPE, ACTUAL_NEW_LINE)
                .replace(JSON_NEW_LINE_ESCAPE, ACTUAL_NEW_LINE);

        System.out.println(formattedOutput);
    }
}

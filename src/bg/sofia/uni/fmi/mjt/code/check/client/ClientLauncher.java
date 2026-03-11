package bg.sofia.uni.fmi.mjt.code.check.client;

import bg.sofia.uni.fmi.mjt.code.check.Config;

public class ClientLauncher {
    public static void main(String[] args) {

        CodeCheckClient client = new CodeCheckClient(Config.port());
        client.start();
    }
}

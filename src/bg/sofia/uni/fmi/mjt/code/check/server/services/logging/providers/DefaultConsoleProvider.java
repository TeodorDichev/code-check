package bg.sofia.uni.fmi.mjt.code.check.server.services.logging.providers;

public class DefaultConsoleProvider implements ConsoleProvider {
    @Override
    public void write(String str) {
        System.out.print(str);
    }
}

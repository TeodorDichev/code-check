package bg.sofia.uni.fmi.mjt.code.check.server.services;

import java.util.UUID;

// https://medium.com/@ganeshbn/how-to-generate-a-unique-string-in-java-without-third-party-libraries-a0b9691b2b93
public class UniqueStringGenerator {
    public static String generate() {
        return UUID.randomUUID().toString();
    }
}

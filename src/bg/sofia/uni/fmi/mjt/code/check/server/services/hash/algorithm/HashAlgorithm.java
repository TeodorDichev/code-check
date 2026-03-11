package bg.sofia.uni.fmi.mjt.code.check.server.services.hash.algorithm;

public interface HashAlgorithm {
    String hash(String plainText);

    boolean verify(String plainText, String hashedText);
}

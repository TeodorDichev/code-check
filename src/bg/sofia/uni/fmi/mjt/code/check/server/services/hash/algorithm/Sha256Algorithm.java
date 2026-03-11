package bg.sofia.uni.fmi.mjt.code.check.server.services.hash.algorithm;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public class Sha256Algorithm implements HashAlgorithm {
    @Override
    public String hash(String plainText) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(plainText.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(encodedHash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Hash algorithm not found", e);
        }
    }

    @Override
    public boolean verify(String plainText, String hashedText) {
        return hash(plainText).equals(hashedText);
    }
}

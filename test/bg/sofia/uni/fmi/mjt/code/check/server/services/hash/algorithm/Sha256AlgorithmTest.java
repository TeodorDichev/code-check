package bg.sofia.uni.fmi.mjt.code.check.server.services.hash.algorithm;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class Sha256AlgorithmTest {

    private Sha256Algorithm algorithm;

    @BeforeEach
    void setUp() {
        algorithm = new Sha256Algorithm();
    }

    @Test
    void testHashProducesCorrectSha256Value() {
        // Standard SHA-256 hash for "password"
        String input = "password";
        String expected = "5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8";

        String actual = algorithm.hash(input);

        assertEquals(expected, actual, "The hash of 'password' should match the standard SHA-256 hex string");
    }

    @Test
    void testHashIsDeterministic() {
        String input = "mjt-2026";

        assertEquals(algorithm.hash(input), algorithm.hash(input),
                "Hashing the same input twice should produce identical results");
    }

    @Test
    void testHashDifferentInputsProduceDifferentResults() {
        assertNotEquals(algorithm.hash("user1"), algorithm.hash("user2"),
                "Different inputs should produce different hash values");
    }

    @Test
    void testVerifyReturnsTrueForCorrectMatch() {
        String input = "secure_pass";
        String hash = algorithm.hash(input);

        assertTrue(algorithm.verify(input, hash),
                "Verify should return true when the plain text matches the hash");
    }

    @Test
    void testVerifyReturnsFalseForIncorrectMatch() {
        String input = "secure_pass";
        String wrongInput = "wrong_pass";
        String hash = algorithm.hash(input);

        assertFalse(algorithm.verify(wrongInput, hash),
                "Verify should return false when the plain text does not match the hash");
    }
}
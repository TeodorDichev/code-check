package bg.sofia.uni.fmi.mjt.code.check.server.utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class NullableTest {

    @Test
    void testOrDefaultReturnsValueWhenObjectIsNotNull() {
        String original = "exists";
        String defaultValue = "fallback";

        String result = Nullable.orDefault(original, defaultValue);

        assertEquals(original, result, "Should return the original object if it's not null");
    }

    @Test
    void testOrDefaultReturnsDefaultWhenObjectIsNull() {
        String defaultValue = "fallback";

        String result = Nullable.orDefault(null, defaultValue);

        assertEquals(defaultValue, result, "Should return the default value when the original object is null");
    }
}
package bg.sofia.uni.fmi.mjt.code.check.server.session;

import bg.sofia.uni.fmi.mjt.code.check.server.entities.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.nio.channels.SocketChannel;

@ExtendWith(MockitoExtension.class)
class DefaultSessionStoreTest {

    private DefaultSessionStore sessionStore;
    private SocketChannel mockChannel1;
    private SocketChannel mockChannel2;

    @BeforeEach
    void setUp() {
        sessionStore = new DefaultSessionStore();
        mockChannel1 = mock(SocketChannel.class);
        mockChannel2 = mock(SocketChannel.class);
    }

    @Test
    void testUpdateUserReplacesObjectForMatchingUsername() {
        // Arrange
        String username = "stoyo";
        String dummyHash = "123";
        String course = "mjt";

        User oldUser = new User(username, dummyHash);
        oldUser.enrollInCourse(course);

        User newUser = new User(username, dummyHash);
        newUser.administerCourse(course); // Now an admin

        sessionStore.register(new Session(mockChannel1, oldUser));
        sessionStore.updateUser(username, newUser);

        User cachedUser = sessionStore.getUser(mockChannel1);

        assertNotNull(cachedUser, "User should still exist in session");
        assertSame(newUser, cachedUser, "The session store must hold the NEW object reference");
        assertTrue(cachedUser.enrolledCourseIds().isEmpty(), "Should no longer be enrolled");
        assertTrue(cachedUser.administeredCoursesIds().contains("mjt"), "Should now be administering");
    }

    @Test
    void testUpdateUserDoesNotAffectOtherUsers() {
        User userA = new User("azis", "motel");
        User userB = new User("fiki", "ne znam");
        User updatedUserA = new User("azis", "should update other but lazy");

        sessionStore.register(new Session(mockChannel1, userA));
        sessionStore.register(new Session(mockChannel2, userB));
        sessionStore.updateUser("azis", updatedUserA);

        assertSame(updatedUserA, sessionStore.getUser(mockChannel1), "azis should be updated");
        assertSame(userB, sessionStore.getUser(mockChannel2), "fiki's session should remain completely untouched");
    }

    @Test
    void testUpdateUserWithNullParameters() {
        User user = new User("papi hans", "keks");
        sessionStore.register(new Session(mockChannel1, user));

        assertDoesNotThrow(() -> sessionStore.updateUser(null, user),
                "Should handle null username gracefully");
        assertDoesNotThrow(() -> sessionStore.updateUser("papi hans", null),
                "Should handle null user object gracefully");

        assertSame(user, sessionStore.getUser(mockChannel1), "User should remain unchanged after null update attempt");
    }
}
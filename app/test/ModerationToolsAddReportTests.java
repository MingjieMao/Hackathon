import dao.PostDAO;
import dao.UserDAO;
import dao.model.Message;
import dao.model.Post;
import dao.model.User;
import moderation.ModerationTools;
import org.junit.Before;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.*;

public class ModerationToolsAddReportTests {

    private UUID userId;
    private UUID msgId;
    private UUID unknownId;

    @Before
    public void setUp() {
        UserDAO.getInstance().clear();
        PostDAO.getInstance().clear();
        ModerationTools.clearAll();

        userId = UUID.randomUUID();
        msgId = UUID.randomUUID();
        unknownId = UUID.randomUUID();

        UserDAO.getInstance().add(new User(userId, User.Role.Member, "testuser", "pass1234"));

        Post post = new Post(UUID.randomUUID(), userId, "Test Post");
        post.messages.insert(new Message(msgId, userId, post.id, 1000L, "hello"));
        PostDAO.getInstance().add(post);
    }

    // message UUID does not exist → false
    @Test
    public void testMessageNotFound() {
        assertFalse(ModerationTools.addReport(unknownId, userId, 1000L));
    }

    // user UUID does not exist → false
    @Test
    public void testUserNotFound() {
        assertFalse(ModerationTools.addReport(msgId, unknownId, 1000L));
    }

    // both UUIDs do not exist → false (message check fires first)
    @Test
    public void testBothNotFound() {
        assertFalse(ModerationTools.addReport(unknownId, unknownId, 1000L));
    }

    // user already reported this message → false
    @Test
    public void testAlreadyReported() {
        assertTrue(ModerationTools.addReport(msgId, userId, 1000L));
        assertFalse(ModerationTools.addReport(msgId, userId, 2000L));
    }

    // valid inputs, first report → true and hasReported reflects it
    @Test
    public void testSuccessAndHasReported() {
        assertFalse(ModerationTools.hasReported(msgId, userId));
        assertTrue(ModerationTools.addReport(msgId, userId, 1000L));
        assertTrue(ModerationTools.hasReported(msgId, userId));
    }

    // different users can each report the same message
    @Test
    public void testMultipleUsersReportSameMessage() {
        UUID userId2 = UUID.randomUUID();
        UserDAO.getInstance().add(new User(userId2, User.Role.Member, "other111", "pass1234"));

        assertTrue(ModerationTools.addReport(msgId, userId, 1000L));
        assertTrue(ModerationTools.addReport(msgId, userId2, 2000L));
    }

    // message exists in a second post (findMessage traverses multiple posts)
    @Test
    public void testMessageInSecondPost() {
        UUID msgId2 = UUID.randomUUID();
        Post post2 = new Post(UUID.randomUUID(), userId, "Second Post");
        post2.messages.insert(new Message(msgId2, userId, post2.id, 2000L, "world"));
        PostDAO.getInstance().add(post2);

        assertTrue(ModerationTools.addReport(msgId2, userId, 1000L));
    }
}

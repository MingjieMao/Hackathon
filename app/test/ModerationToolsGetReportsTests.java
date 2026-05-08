import dao.PostDAO;
import dao.UserDAO;
import dao.model.Message;
import dao.model.Post;
import dao.model.User;
import moderation.ModerationTools;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;

public class ModerationToolsGetReportsTests {

    private UUID userId1, userId2, userId3;
    private UUID msgId1, msgId2, msgId3;

    @Before
    public void setUp() {
        UserDAO.getInstance().clear();
        PostDAO.getInstance().clear();
        ModerationTools.clearAll();

        userId1 = UUID.randomUUID();
        userId2 = UUID.randomUUID();
        userId3 = UUID.randomUUID();

        UserDAO.getInstance().add(new User(userId1, User.Role.Member, "user1111", "pass1234"));
        UserDAO.getInstance().add(new User(userId2, User.Role.Member, "user2222", "pass1234"));
        UserDAO.getInstance().add(new User(userId3, User.Role.Member, "user3333", "pass1234"));

        msgId1 = UUID.randomUUID();
        msgId2 = UUID.randomUUID();
        msgId3 = UUID.randomUUID();

        Post post = new Post(UUID.randomUUID(), userId1, "Test Post");
        post.messages.insert(new Message(msgId1, userId1, post.id, 100L, "msg1"));
        post.messages.insert(new Message(msgId2, userId1, post.id, 200L, "msg2"));
        post.messages.insert(new Message(msgId3, userId1, post.id, 300L, "msg3"));
        PostDAO.getInstance().add(post);
    }

    // ── invalid inputs ───────────────────────────────────────────────────────

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidStrategy() {
        ModerationTools.getReportedMessages("NEWEST", 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyStrategy() {
        ModerationTools.getReportedMessages("", 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testZeroAmount() {
        ModerationTools.getReportedMessages("OLDEST", 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNegativeAmount() {
        ModerationTools.getReportedMessages("MOST", -1);
    }

    // ── no active reports → empty result ─────────────────────────────────────

    @Test
    public void testNoReports_emptyIterator() {
        assertFalse(ModerationTools.getReportedMessages("OLDEST", 10).hasNext());
        assertFalse(ModerationTools.getReportedMessages("MOST", 10).hasNext());
    }

    @Test
    public void testAllReportsRemoved_emptyIterator() {
        ModerationTools.addReport(msgId1, userId1, 10L);
        ModerationTools.removeReport(msgId1, userId1, 0L);
        assertFalse(ModerationTools.getReportedMessages("OLDEST", 10).hasNext());
    }

    // ── OLDEST: order by oldest non-removed report timestamp ascending ────────

    @Test
    public void testOldest_twoMessages() {
        ModerationTools.addReport(msgId1, userId1, 10L);
        ModerationTools.addReport(msgId2, userId1, 20L);

        List<UUID> result = collectIds(ModerationTools.getReportedMessages("OLDEST", 10));
        assertEquals(List.of(msgId1, msgId2), result);
    }

    @Test
    public void testOldest_multipleReportsPerMessage_usesOldestTimestamp() {
        // msg2: reports at t=5 (user2) and t=1 (user1) → oldest is t=1
        // msg1: report at t=3
        // expected order: msg2 (oldest=1), msg1 (oldest=3)
        ModerationTools.addReport(msgId2, userId2, 5L);
        ModerationTools.addReport(msgId2, userId1, 1L);
        ModerationTools.addReport(msgId1, userId1, 3L);

        List<UUID> result = collectIds(ModerationTools.getReportedMessages("OLDEST", 10));
        assertEquals(List.of(msgId2, msgId1), result);
    }

    @Test
    public void testOldest_removedReportNotConsideredOldest() {
        // msg1: report at t=1 then removed, report at t=50 remains → oldest active is t=50
        // msg2: report at t=10
        // expected: msg2 (oldest=10), msg1 (oldest=50)
        ModerationTools.addReport(msgId1, userId1, 1L);
        ModerationTools.removeReport(msgId1, userId1, 0L);
        ModerationTools.addReport(msgId1, userId2, 50L);
        ModerationTools.addReport(msgId2, userId1, 10L);

        List<UUID> result = collectIds(ModerationTools.getReportedMessages("OLDEST", 10));
        assertEquals(List.of(msgId2, msgId1), result);
    }

    // ── MOST: order by active report count descending ────────────────────────

    @Test
    public void testMost_twoMessages() {
        // msg1: 2 reports, msg2: 1 report → msg1 first
        ModerationTools.addReport(msgId1, userId1, 100L);
        ModerationTools.addReport(msgId1, userId2, 200L);
        ModerationTools.addReport(msgId2, userId1, 100L);

        List<UUID> result = collectIds(ModerationTools.getReportedMessages("MOST", 10));
        assertEquals(List.of(msgId1, msgId2), result);
    }

    @Test
    public void testMost_removedReportReducesCount() {
        // msg1: 2 reports, msg2: 2 reports with one removed → msg2 has 1 active
        ModerationTools.addReport(msgId1, userId1, 100L);
        ModerationTools.addReport(msgId1, userId2, 200L);
        ModerationTools.addReport(msgId2, userId1, 100L);
        ModerationTools.addReport(msgId2, userId2, 200L);
        ModerationTools.removeReport(msgId2, userId2, 0L);

        List<UUID> result = collectIds(ModerationTools.getReportedMessages("MOST", 10));
        assertEquals(List.of(msgId1, msgId2), result);
    }

    // ── amount limiting ──────────────────────────────────────────────────────

    @Test
    public void testAmountLimitsResults() {
        ModerationTools.addReport(msgId1, userId1, 10L);
        ModerationTools.addReport(msgId2, userId1, 20L);
        ModerationTools.addReport(msgId3, userId1, 30L);

        List<UUID> result = collectIds(ModerationTools.getReportedMessages("OLDEST", 2));
        assertEquals(2, result.size());
        assertEquals(msgId1, result.get(0));
        assertEquals(msgId2, result.get(1));
    }

    @Test
    public void testAmountLargerThanAvailable() {
        ModerationTools.addReport(msgId1, userId1, 10L);

        List<UUID> result = collectIds(ModerationTools.getReportedMessages("OLDEST", 100));
        assertEquals(1, result.size());
    }

    // ── each message appears at most once ────────────────────────────────────

    @Test
    public void testNoDuplicates_multipleReportsPerMessage() {
        ModerationTools.addReport(msgId1, userId1, 10L);
        ModerationTools.addReport(msgId1, userId2, 20L);
        ModerationTools.addReport(msgId1, userId3, 30L);

        List<UUID> result = collectIds(ModerationTools.getReportedMessages("MOST", 10));
        assertEquals(1, result.size());
        assertEquals(msgId1, result.get(0));
    }

    // ── helper ───────────────────────────────────────────────────────────────

    private List<UUID> collectIds(Iterator<Message> it) {
        List<UUID> ids = new ArrayList<>();
        while (it.hasNext()) ids.add(it.next().id());
        return ids;
    }
}

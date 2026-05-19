package com.example.myapplication;

import android.content.Context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.UUID;

import dao.PostDAO;
import dao.UserDAO;
import dao.model.Message;
import dao.model.Post;
import dao.model.Report;
import dao.model.TimestampFormatter;
import dao.model.TimestampFormatterTimeSinceEnglish;
import dao.model.User;
import moderation.ModerationTools;

public final class AppData {
    public static final String STRATEGY_OLDEST = "OLDEST";
    public static final String STRATEGY_MOST = "MOST";

    private static final TimestampFormatter TIMESTAMP_FORMATTER = new TimestampFormatterTimeSinceEnglish();

    private static boolean populated;
    private static boolean adminMode;

    private static User memberViewer;
    private static User adminViewer;
    private static User studyBuddy;
    private static User lateCoder;
    private static User uxPilot;
    private static User treeSage;

    private static Message hiddenExampleMessage;
    private static Message queueExampleMessage;

    private AppData() {
    }

    public static void ensurePopulated() {
        if (populated) {
            return;
        }

        UserDAO.getInstance().clear();
        PostDAO.getInstance().clear();
        ModerationTools.clearAll();

        seedUsers();
        seedForumThreads();
        seedModerationState();

        adminMode = false;
        populated = true;
    }

    public static boolean isAdminMode() {
        return adminMode;
    }

    public static void toggleViewerMode() {
        ensurePopulated();
        adminMode = !adminMode;
    }

    public static String getCurrentModeLabel(Context context) {
        return context.getString(adminMode ? R.string.mode_admin : R.string.mode_member);
    }

    public static String getCurrentUserLabel(Context context) {
        User user = getCurrentUser();
        if (user == null) {
            return context.getString(R.string.viewer_unknown);
        }
        return context.getString(
                adminMode ? R.string.viewer_user_admin : R.string.viewer_user_member,
                user.username()
        );
    }

    public static String getMainSubtitle(Context context) {
        if (adminMode) {
            return context.getString(R.string.main_subtitle_admin);
        }
        return context.getString(R.string.main_subtitle_member);
    }

    public static ArrayList<Post> getPosts() {
        ensurePopulated();

        ArrayList<Post> posts = new ArrayList<>();
        Iterator<Post> iterator = PostDAO.getInstance().getAll();
        while (iterator.hasNext()) {
            posts.add(iterator.next());
        }

        posts.sort(Comparator.comparingLong(AppData::getLatestActivityTimestamp).reversed());
        return posts;
    }

    public static Post getPostById(String postId) {
        ensurePopulated();
        if (postId == null) {
            return null;
        }

        return PostDAO.getInstance().get(new Post(UUID.fromString(postId)));
    }

    public static Post getPostForMessage(Message message) {
        if (message == null) {
            return null;
        }
        return PostDAO.getInstance().get(new Post(message.thread()));
    }

    public static ArrayList<Message> getMessages(Post post) {
        ensurePopulated();
        ArrayList<Message> messages = new ArrayList<>();
        if (post == null) {
            return messages;
        }

        Iterator<Message> iterator = adminMode ? post.messages.getAll() : post.getVisibleMessages(false).getAll();
        while (iterator.hasNext()) {
            messages.add(iterator.next());
        }
        return messages;
    }

    public static ArrayList<Message> getReportedMessages(String strategy) {
        ensurePopulated();
        ArrayList<Message> messages = new ArrayList<>();
        int totalMessages = Math.max(1, getAllMessages().size());
        Iterator<Message> iterator = ModerationTools.getReportedMessages(strategy, totalMessages);
        while (iterator.hasNext()) {
            messages.add(iterator.next());
        }
        return messages;
    }

    public static String getPostSummary(Context context, Post post) {
        int visibleReplies = getVisibleMessageCount(post);
        int totalReplies = getTotalMessageCount(post);
        int hiddenReplies = totalReplies - visibleReplies;

        if (adminMode) {
            if (hiddenReplies > 0) {
                return context.getString(R.string.post_summary_admin_hidden, totalReplies, hiddenReplies);
            }
            return context.getString(R.string.post_summary_admin_visible, totalReplies);
        }

        if (hiddenReplies > 0) {
            return context.getString(R.string.post_summary_member_hidden, visibleReplies, hiddenReplies);
        }
        return context.getString(R.string.post_summary_member_visible, visibleReplies);
    }

    public static String getPostMeta(Context context, Post post) {
        return context.getString(R.string.post_meta_started_by, getUsername(post.poster));
    }

    public static int getTotalMessageCount(Post post) {
        return collectMessages(post.messages.getAll()).size();
    }

    public static int getVisibleMessageCount(Post post) {
        return collectMessages(post.getVisibleMessages(false).getAll()).size();
    }

    public static int getReportCount(Message message) {
        int count = 0;
        Iterator<Report> iterator = ModerationTools.getAllReports();
        while (iterator.hasNext()) {
            Report report = iterator.next();
            if (report.message.equals(message.id())) {
                count++;
            }
        }
        return count;
    }

    public static boolean isHidden(Message message) {
        return message != null && ModerationTools.isHidden(message.id());
    }

    public static boolean hasCurrentUserReported(Message message) {
        User currentUser = getCurrentUser();
        return currentUser != null && message != null && ModerationTools.hasReported(message.id(), currentUser.id());
    }

    public static boolean toggleReport(Message message) {
        ensurePopulated();
        if (adminMode || message == null || memberViewer == null) {
            return false;
        }

        long now = System.currentTimeMillis();
        if (ModerationTools.hasReported(message.id(), memberViewer.id())) {
            return ModerationTools.removeReport(message.id(), memberViewer.id(), now);
        }
        return ModerationTools.addReport(message.id(), memberViewer.id(), now);
    }

    public static boolean toggleHidden(Message message) {
        ensurePopulated();
        if (!adminMode || message == null || adminViewer == null) {
            return false;
        }

        return ModerationTools.setHidden(message.id(), adminViewer.id(), !ModerationTools.isHidden(message.id()));
    }

    public static String getMessageStatus(Context context, Message message) {
        ArrayList<String> parts = new ArrayList<>();
        int reportCount = getReportCount(message);

        if (isHidden(message)) {
            parts.add(context.getString(adminMode ? R.string.message_hidden_admin : R.string.message_hidden_member));
        }
        if (reportCount > 0) {
            parts.add(reportCount == 1
                    ? context.getString(R.string.message_report_single)
                    : context.getString(R.string.message_report_plural, reportCount));
        }
        if (!adminMode && hasCurrentUserReported(message)) {
            parts.add(context.getString(R.string.message_reported_by_you));
        }
        if (parts.isEmpty()) {
            return context.getString(adminMode ? R.string.message_visible_admin : R.string.message_visible_member);
        }
        return joinWithBullets(parts);
    }

    public static String getMessageActionLabel(Context context, Message message) {
        if (adminMode) {
            return context.getString(isHidden(message) ? R.string.action_restore : R.string.action_hide);
        }
        return context.getString(hasCurrentUserReported(message) ? R.string.action_undo_report : R.string.action_report);
    }

    public static String getQueueSubtitle(Context context, String strategy, int size) {
        String prefix = context.getString(
                STRATEGY_MOST.equals(strategy) ? R.string.queue_sort_most : R.string.queue_sort_oldest
        );
        if (size == 0) {
            return context.getString(R.string.queue_subtitle_empty, prefix);
        }
        return context.getString(R.string.queue_subtitle_count, prefix, size);
    }

    public static String getReportedCardMeta(Context context, Message message) {
        Post post = getPostForMessage(message);
        String topic = post == null ? context.getString(R.string.thread_unknown) : post.topic;
        return context.getString(R.string.reported_meta_format, topic, getMessageStatus(context, message));
    }

    public static String getUsername(UUID userId) {
        ensurePopulated();
        User user = UserDAO.getInstance().getByUUID(userId);
        if (user == null || user.username() == null) {
            return "Unknown user";
        }
        return user.username();
    }

    public static String formatTimestamp(long timestamp) {
        return TIMESTAMP_FORMATTER.format(timestamp);
    }

    public static String getThreadUnknownLabel(Context context) {
        return context.getString(R.string.thread_unknown);
    }

    public static String formatMessageAuthorLine(Context context, Message message) {
        return context.getString(
                R.string.message_author_line,
                getUsername(message.poster()),
                formatTimestamp(message.timestamp())
        );
    }

    private static User getCurrentUser() {
        return adminMode ? adminViewer : memberViewer;
    }

    private static void seedUsers() {
        memberViewer = addUser("campusviewer", User.Role.Member);
        adminViewer = addUser("modmentor", User.Role.Admin);
        studyBuddy = addUser("studybuddy", User.Role.Member);
        lateCoder = addUser("latecoder", User.Role.Member);
        uxPilot = addUser("uxpilot", User.Role.Member);
        treeSage = addUser("treesage", User.Role.Member);
    }

    private static void seedForumThreads() {
        long now = System.currentTimeMillis();

        Post teamworkPost = createPost(memberViewer, "How should we split Hackathon 2 work fairly?");
        addMessage(teamworkPost, memberViewer, now - minutes(90), "We only have a short window. How would you divide UI, backend wiring, and demo prep?");
        addMessage(teamworkPost, studyBuddy, now - minutes(82), "I would keep one person on Android layouts, one person on adapters and activities, and one person checking the backend integration.");
        hiddenExampleMessage = addMessage(teamworkPost, lateCoder, now - minutes(71), "This question is basic. Read the brief before posting.");
        addMessage(teamworkPost, adminViewer, now - minutes(65), "Let's keep replies constructive. A clean plan beats rushing at the last minute.");

        Post featurePost = createPost(uxPilot, "Showcase ideas for the moderation dashboard");
        addMessage(featurePost, uxPilot, now - minutes(54), "A lightweight admin queue could show reported replies, counts, and a quick hide action.");
        queueExampleMessage = addMessage(featurePost, studyBuddy, now - minutes(49), "We could sort the queue by oldest report or by the number of reports to match the backend strategies.");
        addMessage(featurePost, treeSage, now - minutes(44), "If we surface hidden status directly in the thread, the demo becomes much easier to understand.");

        Post reusePost = createPost(treeSage, "Can we reuse the week 7 Android prototype?");
        addMessage(reusePost, treeSage, now - minutes(36), "The week 7 project already has a post list and thread screen, so reusing it should save a lot of time.");
        addMessage(reusePost, memberViewer, now - minutes(31), "That sounds good, especially if we swap in the Hackathon 1 backend packages.");
        addMessage(reusePost, adminViewer, now - minutes(28), "Exactly. Then we only need to add reporting, hiding, and a moderation queue.");

        Post explanationPost = createPost(studyBuddy, "Best way to explain AVL trees in 30 seconds");
        addMessage(explanationPost, studyBuddy, now - minutes(24), "I want a version that makes sense to markers who only have a few seconds.");
        addMessage(explanationPost, treeSage, now - minutes(20), "Describe them as self-balancing search trees that keep lookups fast by preventing tall, lopsided branches.");
        addMessage(explanationPost, lateCoder, now - minutes(16), "If you show one before-and-after rotation, people usually understand it quickly.");

        Post presentationPost = createPost(adminViewer, "What should the demo video focus on?");
        addMessage(presentationPost, adminViewer, now - minutes(12), "I think we should show member reporting, then switch to admin mode and review the queue.");
        addMessage(presentationPost, uxPilot, now - minutes(8), "That gives a clear cause-and-effect story and shows the extension is fully integrated.");
        addMessage(presentationPost, memberViewer, now - minutes(5), "Agreed. It also proves hidden replies disappear for members but remain visible to admins.");
    }

    private static void seedModerationState() {
        long now = System.currentTimeMillis();

        ModerationTools.addReport(hiddenExampleMessage.id(), memberViewer.id(), now - minutes(64));
        ModerationTools.addReport(hiddenExampleMessage.id(), studyBuddy.id(), now - minutes(63));
        ModerationTools.setHidden(hiddenExampleMessage.id(), adminViewer.id(), true);

        ModerationTools.addReport(queueExampleMessage.id(), uxPilot.id(), now - minutes(41));
        ModerationTools.addReport(queueExampleMessage.id(), treeSage.id(), now - minutes(39));
    }

    private static User addUser(String username, User.Role role) {
        User user = new User(UUID.randomUUID(), role, username, "demo1234");
        UserDAO.getInstance().add(user);
        return user;
    }

    private static Post createPost(User poster, String topic) {
        Post post = new Post(UUID.randomUUID(), poster.id(), topic);
        PostDAO.getInstance().add(post);
        return post;
    }

    private static Message addMessage(Post post, User poster, long timestamp, String content) {
        Message message = new Message(UUID.randomUUID(), poster.id(), post.id, timestamp, content);
        post.messages.insert(message);
        return message;
    }

    private static ArrayList<Message> getAllMessages() {
        ArrayList<Message> messages = new ArrayList<>();
        Iterator<Message> iterator = PostDAO.getInstance().getAllMessages();
        while (iterator.hasNext()) {
            messages.add(iterator.next());
        }
        return messages;
    }

    private static ArrayList<Message> collectMessages(Iterator<Message> iterator) {
        ArrayList<Message> messages = new ArrayList<>();
        while (iterator.hasNext()) {
            messages.add(iterator.next());
        }
        return messages;
    }

    private static long getLatestActivityTimestamp(Post post) {
        ArrayList<Message> messages = collectMessages(post.messages.getAll());
        if (messages.isEmpty()) {
            return Long.MIN_VALUE;
        }
        return Collections.max(messages, Comparator.comparingLong(Message::timestamp)).timestamp();
    }

    private static long minutes(long amount) {
        return amount * 60_000L;
    }

    private static String joinWithBullets(ArrayList<String> parts) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) {
                builder.append(" • ");
            }
            builder.append(parts.get(i));
        }
        return builder.toString();
    }
}

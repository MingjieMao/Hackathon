package com.example.myapplication;

import android.content.Context;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    public static final String FORUM_ANU = "anu";
    public static final String FORUM_UNSW = "unsw";
    public static final String FORUM_USYD = "usyd";
    public static final String FORUM_UM = "um";

    private static final List<String> FORUM_ORDER = Arrays.asList(
            FORUM_ANU,
            FORUM_UNSW,
            FORUM_USYD,
            FORUM_UM
    );

    private static final TimestampFormatter TIMESTAMP_FORMATTER = new TimestampFormatterTimeSinceEnglish();

    private static final Map<UUID, PostMeta> POST_META = new HashMap<>();
    private static final Map<UUID, String> POST_IMAGE_URIS = new HashMap<>();
    private static final Map<UUID, MessageMeta> MESSAGE_META = new HashMap<>();
    private static final Map<UUID, String> MESSAGE_IMAGE_URIS = new HashMap<>();
    private static final Map<VoteTarget, Integer> BASE_VOTES = new HashMap<>();
    private static final Map<UserVoteKey, Integer> USER_VOTES = new HashMap<>();

    private static boolean populated;
    private static boolean adminMode;
    private static String populatedLanguage;
    private static String selectedForumKey = FORUM_ANU;

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
        String currentLanguage = currentLanguageTag();
        if (populated && currentLanguage.equals(populatedLanguage)) {
            return;
        }

        UserDAO.getInstance().clear();
        PostDAO.getInstance().clear();
        ModerationTools.clearAll();
        POST_META.clear();
        POST_IMAGE_URIS.clear();
        MESSAGE_META.clear();
        MESSAGE_IMAGE_URIS.clear();
        BASE_VOTES.clear();
        USER_VOTES.clear();

        seedUsers();
        seedForumThreads();
        seedModerationState();

        adminMode = false;
        selectedForumKey = FORUM_ANU;
        populated = true;
        populatedLanguage = currentLanguage;
    }

    public static boolean isAdminMode() {
        return adminMode;
    }

    public static void toggleViewerMode() {
        ensurePopulated();
        adminMode = !adminMode;
    }

    public static List<String> getForumKeys() {
        return FORUM_ORDER;
    }

    public static void setSelectedForum(String forumKey) {
        ensurePopulated();
        if (FORUM_ORDER.contains(forumKey)) {
            selectedForumKey = forumKey;
        }
    }

    public static boolean isSelectedForum(String forumKey) {
        ensurePopulated();
        return selectedForumKey.equals(forumKey);
    }

    public static String getSelectedForumLabel(Context context) {
        return getForumLabel(context, selectedForumKey);
    }

    public static int getSelectedForumAvatarResId() {
        return getForumAvatarResId(selectedForumKey);
    }

    public static String getForumLabel(Context context, String forumKey) {
        if (FORUM_UNSW.equals(forumKey)) {
            return context.getString(R.string.forum_unsw_label);
        }
        if (FORUM_USYD.equals(forumKey)) {
            return context.getString(R.string.forum_usyd_label);
        }
        if (FORUM_UM.equals(forumKey)) {
            return context.getString(R.string.forum_um_label);
        }
        return context.getString(R.string.forum_anu_label);
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
        ensurePopulated();
        if (!FORUM_ANU.equals(selectedForumKey)) {
            return context.getString(R.string.forum_empty_subtitle, getSelectedForumLabel(context));
        }
        return context.getString(adminMode
                ? R.string.forum_anu_subtitle_admin
                : R.string.forum_anu_subtitle_member);
    }

    public static String getFeedEmptyTitle(Context context) {
        if (FORUM_ANU.equals(selectedForumKey)) {
            return context.getString(R.string.feed_empty_title_generic);
        }
        return context.getString(R.string.feed_empty_title_forum, getSelectedForumLabel(context));
    }

    public static String getFeedEmptyBody(Context context) {
        if (FORUM_ANU.equals(selectedForumKey)) {
            return context.getString(R.string.feed_empty_body_generic);
        }
        return context.getString(R.string.feed_empty_body_forum, getSelectedForumLabel(context));
    }

    public static ArrayList<Post> getPosts() {
        ensurePopulated();

        ArrayList<Post> posts = new ArrayList<>();
        Iterator<Post> iterator = PostDAO.getInstance().getAll();
        while (iterator.hasNext()) {
            Post post = iterator.next();
            if (!selectedForumKey.equals(getForumKey(post))) {
                continue;
            }
            if (!adminMode && isRootHidden(post)) {
                continue;
            }
            posts.add(post);
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

    public static Message getRootMessage(Post post) {
        if (post == null) {
            return null;
        }
        UUID rootId = getRootMessageId(post);
        if (rootId == null) {
            return null;
        }
        return findMessage(post, rootId);
    }

    public static ArrayList<Message> getMessages(Post post) {
        ensurePopulated();
        ArrayList<Message> messages = new ArrayList<>();
        if (post == null) {
            return messages;
        }

        ArrayList<Message> visibleMessages = collectMessages(adminMode
                ? post.messages.getAll()
                : post.getVisibleMessages(false).getAll());
        Set<UUID> visibleIds = new HashSet<>();
        for (Message message : visibleMessages) {
            visibleIds.add(message.id());
        }

        ArrayList<Message> topLevel = new ArrayList<>();
        Map<UUID, ArrayList<Message>> groupedReplies = new HashMap<>();
        UUID rootId = getRootMessageId(post);

        for (Message message : visibleMessages) {
            if (isRootMessage(message)) {
                continue;
            }

            UUID parentId = getParentId(message);
            if (parentId == null || parentId.equals(rootId) || !visibleIds.contains(parentId)) {
                topLevel.add(message);
                continue;
            }
            groupedReplies.computeIfAbsent(parentId, unused -> new ArrayList<>()).add(message);
        }

        sortMessagesByTimestamp(topLevel);
        for (ArrayList<Message> children : groupedReplies.values()) {
            sortMessagesByTimestamp(children);
        }

        for (Message message : topLevel) {
            appendThread(message, groupedReplies, messages);
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

        if (adminMode && hiddenReplies > 0) {
            return context.getString(R.string.post_summary_admin_hidden, totalReplies, hiddenReplies);
        }
        if (adminMode) {
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

    public static String getPostFeedMeta(Context context, Post post) {
        return context.getString(
                R.string.post_feed_meta_format,
                getPostCommunityLabel(context, post),
                getUsername(post.poster),
                getPostTimestampLabel(post)
        );
    }

    public static String getPostFeedByline(Context context, Post post) {
        return context.getString(
                R.string.post_feed_meta_byline_format,
                getUsername(post.poster),
                getPostTimestampLabel(post)
        );
    }

    public static String getPostCommunityLabel(Context context, Post post) {
        return getForumLabel(context, getForumKey(post));
    }

    public static int getPostCommunityAvatarResId(Post post) {
        return getForumAvatarResId(getForumKey(post));
    }

    public static int getForumAvatarResId(String forumKey) {
        if (FORUM_UNSW.equals(forumKey)) {
            return R.drawable.avatar_unsw;
        }
        if (FORUM_USYD.equals(forumKey)) {
            return R.drawable.avatar_usyd;
        }
        if (FORUM_UM.equals(forumKey)) {
            return R.drawable.avatar_um;
        }
        return R.drawable.avatar_anu;
    }

    public static String getPostBody(Post post) {
        Message rootMessage = getRootMessage(post);
        return rootMessage == null ? "" : rootMessage.message();
    }

    public static String getPostBodyPreview(Post post) {
        return ellipsize(getPostBody(post), 220);
    }

    public static String getPostTimestampLabel(Post post) {
        Message rootMessage = getRootMessage(post);
        return rootMessage == null ? "" : formatTimestamp(rootMessage.timestamp());
    }

    public static int getTotalMessageCount(Post post) {
        if (post == null) {
            return 0;
        }
        int count = 0;
        Iterator<Message> iterator = post.messages.getAll();
        while (iterator.hasNext()) {
            Message message = iterator.next();
            if (!isRootMessage(message)) {
                count++;
            }
        }
        return count;
    }

    public static int getVisibleMessageCount(Post post) {
        return getMessages(post).size();
    }

    public static int getCommentDepth(Message message) {
        if (message == null || isRootMessage(message)) {
            return 0;
        }
        int depth = 0;
        UUID rootId = getRootMessageId(getPostForMessage(message));
        UUID parentId = getParentId(message);
        while (parentId != null && !parentId.equals(rootId)) {
            depth++;
            MessageMeta meta = MESSAGE_META.get(parentId);
            parentId = meta == null ? null : meta.parentId;
        }
        return depth;
    }

    public static int getMessageReplyCount(Message parent) {
        ensurePopulated();
        if (parent == null) {
            return 0;
        }

        Post post = getPostForMessage(parent);
        if (post == null) {
            return 0;
        }

        int count = 0;
        ArrayList<Message> visibleMessages = collectMessages(adminMode
                ? post.messages.getAll()
                : post.getVisibleMessages(false).getAll());
        for (Message message : visibleMessages) {
            UUID parentId = getParentId(message);
            if (parent.id().equals(parentId)) {
                count++;
            }
        }
        return count;
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
        return currentUser != null
                && message != null
                && ModerationTools.hasReported(message.id(), currentUser.id());
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

    public static Post createPost(String topic, String body) {
        return createPost(topic, body, null);
    }

    public static Post createPost(String topic, String body, String imageUri) {
        ensurePopulated();
        User poster = getCurrentUser();
        if (poster == null || topic == null || topic.trim().isEmpty()) {
            return null;
        }
        long now = System.currentTimeMillis();
        Post post = createForumPost(
                selectedForumKey,
                poster,
                topic.trim(),
                body == null ? "" : body.trim(),
                now,
                1
        );
        String trimmedImageUri = imageUri == null ? "" : imageUri.trim();
        if (!trimmedImageUri.isEmpty()) {
            POST_IMAGE_URIS.put(post.id, trimmedImageUri);
        }
        return post;
    }

    public static Message createReply(Message parent, String content) {
        return createReply(parent, content, null);
    }

    public static Message createReply(Message parent, String content, String imageUri) {
        ensurePopulated();
        User poster = getCurrentUser();
        Post post = getPostForMessage(parent);
        String trimmedContent = content == null ? "" : content.trim();
        String trimmedImageUri = imageUri == null ? "" : imageUri.trim();
        if (poster == null || post == null || parent == null || (trimmedContent.isEmpty() && trimmedImageUri.isEmpty())) {
            return null;
        }

        Message reply = addComment(post, poster, parent, System.currentTimeMillis(), trimmedContent, 0);
        if (!trimmedImageUri.isEmpty()) {
            MESSAGE_IMAGE_URIS.put(reply.id(), trimmedImageUri);
        }
        return reply;
    }

    public static String getMessageImageUri(Message message) {
        if (message == null) {
            return null;
        }
        return MESSAGE_IMAGE_URIS.get(message.id());
    }

    public static String getPostImageUri(Post post) {
        if (post == null) {
            return null;
        }
        return POST_IMAGE_URIS.get(post.id);
    }

    public static boolean togglePostVote(Post post, int direction) {
        return toggleVote(TargetType.POST, post == null ? null : post.id, direction);
    }

    public static boolean toggleMessageVote(Message message, int direction) {
        return toggleVote(TargetType.MESSAGE, message == null ? null : message.id(), direction);
    }

    public static boolean togglePostLike(Post post) {
        return togglePostVote(post, 1);
    }

    public static boolean toggleMessageLike(Message message) {
        return toggleMessageVote(message, 1);
    }

    public static boolean toggleHidden(Message message) {
        ensurePopulated();
        if (!adminMode || message == null || adminViewer == null) {
            return false;
        }

        return ModerationTools.setHidden(message.id(), adminViewer.id(), !ModerationTools.isHidden(message.id()));
    }

    public static int getPostVoteScore(Post post) {
        return getVoteScore(TargetType.POST, post == null ? null : post.id);
    }

    public static int getMessageVoteScore(Message message) {
        return getVoteScore(TargetType.MESSAGE, message == null ? null : message.id());
    }

    public static String getPostLikeCountLabel(Context context, Post post) {
        return String.valueOf(getPostVoteScore(post));
    }

    public static String getPostReplyCountLabel(Context context, Post post) {
        return String.valueOf(getVisibleMessageCount(post));
    }

    public static String getPostCommentChipLabel(Context context, Post post) {
        return context.getString(R.string.post_comments_chip, getVisibleMessageCount(post));
    }

    public static String getMessageLikeCountLabel(Context context, Message message) {
        return String.valueOf(getMessageVoteScore(message));
    }

    public static boolean hasCurrentUserLikedPost(Post post) {
        return getCurrentUserVote(TargetType.POST, post == null ? null : post.id) > 0;
    }

    public static boolean hasCurrentUserLikedMessage(Message message) {
        return getCurrentUserVote(TargetType.MESSAGE, message == null ? null : message.id()) > 0;
    }

    public static int getCurrentUserPostVote(Post post) {
        return getCurrentUserVote(TargetType.POST, post == null ? null : post.id);
    }

    public static int getCurrentUserMessageVote(Message message) {
        return getCurrentUserVote(TargetType.MESSAGE, message == null ? null : message.id());
    }

    public static String getMessageStatus(Context context, Message message) {
        if (message == null) {
            return "";
        }
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
            return "";
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
        String forum = post == null ? context.getString(R.string.thread_unknown) : getPostCommunityLabel(context, post);
        return context.getString(R.string.reported_meta_format, forum, getMessageStatus(context, message));
    }

    public static String getUsername(UUID userId) {
        ensurePopulated();
        User user = UserDAO.getInstance().getByUUID(userId);
        if (user == null || user.username() == null) {
            return contextLanguageIsChinese() ? "unknown" : "unknown";
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

    public static String getAvatarLetter(UUID userId) {
        String username = getUsername(userId);
        if (username.isEmpty()) {
            return "?";
        }
        return username.substring(0, 1).toUpperCase();
    }

    public static int getAvatarColor(UUID userId) {
        String username = getUsername(userId);
        int index = Math.abs(username.hashCode()) % UiPreferences.getGoogleColorCount();
        return UiPreferences.getGoogleColor(index);
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
        boolean zh = contextLanguageIsChinese();

        Post layoutPost = createForumPost(
                FORUM_ANU,
                memberViewer,
                zh ? "我们的 ANU 主页要不要直接做成 Reddit 风格？" : "Should the ANU home feed lean fully into a Reddit-style layout?",
                zh
                        ? "我更想把主页做成信息流，减少大块卡片，把帖子重点放在论坛名、标题、正文和评论入口上。这样讲 demo 的时候会更像真实社区产品。"
                        : "I want the home feed to feel more like a discussion stream: less chunky cards, more focus on the community name, title, body, and comment entry. It should read like a real social product during the demo.",
                now - minutes(96),
                42
        );
        POST_IMAGE_URIS.put(layoutPost.id, drawableImageUri(R.drawable.avatar_anu));
        Message layoutTopReply = addComment(
                layoutPost,
                studyBuddy,
                null,
                now - minutes(88),
                zh
                        ? "赞成，尤其是如果我们把侧边栏改成只负责切论坛，首页就会清爽很多。"
                        : "I agree, especially if the drawer only handles community switching. That would make the home screen much cleaner.",
                14
        );
        MESSAGE_IMAGE_URIS.put(layoutTopReply.id(), drawableImageUri(R.drawable.avatar_unsw));
        addComment(
                layoutPost,
                uxPilot,
                layoutTopReply,
                now - minutes(82),
                zh
                        ? "帖子之间只用分隔线会更像网页 Reddit，视觉节奏也会更快。"
                        : "Using separators instead of cards would feel much closer to Reddit and speed up the visual rhythm.",
                9
        );
        Message layoutThirdLevel = addComment(
                layoutPost,
                treeSage,
                layoutTopReply,
                now - minutes(81),
                zh
                        ? "如果要演示楼中楼，我建议这里再接一个二楼回复，让缩进一眼就能看出来。"
                        : "If we want to show nesting clearly, we should add a second-floor reply here so the indentation reads immediately.",
                6
        );
        addComment(
                layoutPost,
                memberViewer,
                layoutThirdLevel,
                now - minutes(80),
                zh
                        ? "三楼也可以保留得很简洁，只要头像、昵称和操作按钮还在就够了。"
                        : "A third-floor reply can stay very simple as long as the avatar, username, and actions still hold together.",
                4
        );
        hiddenExampleMessage = addComment(
                layoutPost,
                lateCoder,
                layoutTopReply,
                now - minutes(79),
                zh
                        ? "别想太多，直接照着抄就好了。"
                        : "Stop overthinking it and just copy the layout.",
                -3
        );
        addComment(
                layoutPost,
                adminViewer,
                null,
                now - minutes(74),
                zh
                        ? "参考可以，但语气也要保持建设性。我们重点展示的是产品表达和审核能力。"
                        : "Borrow the structure if it helps, but keep the tone constructive. The real story is product clarity plus moderation.",
                18
        );

        Post forumPost = createForumPost(
                FORUM_ANU,
                uxPilot,
                zh ? "先只填充 ANU，另外三个论坛留空可以吗？" : "Can we fully seed ANU first and leave the other three forums empty?",
                zh
                        ? "我打算先把 ANU 论坛的帖子和评论做完整，UNSW、USYD、UM 先显示为空状态，这样论坛切换的逻辑也能先验收。"
                        : "My plan is to fully populate the ANU forum first, while UNSW, USYD, and UM show an empty state for now. That still lets us prove the forum switching flow.",
                now - minutes(67),
                28
        );
        queueExampleMessage = addComment(
                forumPost,
                studyBuddy,
                null,
                now - minutes(60),
                zh
                        ? "可以。老师能一眼看出我们已经有多论坛结构，只是暂时没往另外三个论坛灌内容。"
                        : "Yes. Markers can immediately see the multi-forum structure even if only one forum is populated right now.",
                11
        );
        Message forumSecondFloor = addComment(
                forumPost,
                treeSage,
                queueExampleMessage,
                now - minutes(56),
                zh
                        ? "而且这样更方便测试空页面和创建新帖之后的更新。"
                        : "It also makes empty-state handling and new-post refreshes much easier to test.",
                7
        );
        addComment(
                forumPost,
                adminViewer,
                forumSecondFloor,
                now - minutes(54),
                zh
                        ? "再往下接一个三楼，就能同时展示论坛切换和多层评论。"
                        : "One more third-floor reply here lets us show forum switching and deeper threading in the same post.",
                5
        );

        Post nestedPost = createForumPost(
                FORUM_ANU,
                treeSage,
                zh ? "楼中楼缩进要做到多深才够演示？" : "How deep should comment nesting go for the demo?",
                zh
                        ? "我觉得至少要有两层回复。只要楼层缩进清楚、头像和昵称层级明显，老师就能看出来评论树已经建好了。"
                        : "I think we need at least two visible reply levels. As long as the indentation, avatars, and bold usernames read clearly, the thread structure will be obvious in the demo.",
                now - minutes(49),
                19
        );
        Message nestedRootReply = addComment(
                nestedPost,
                memberViewer,
                null,
                now - minutes(42),
                zh
                        ? "两层够了，再深会挤压手机宽度。"
                        : "Two levels should be enough. Going deeper will start to squeeze the mobile layout.",
                6
        );
        Message nestedSecondLevel = addComment(
                nestedPost,
                uxPilot,
                nestedRootReply,
                now - minutes(38),
                zh
                        ? "那就让第二层仍然保留投票、reply 和举报入口，证明交互不会因为缩进消失。"
                        : "Then the second level should still keep vote, reply, and report entry points so the interaction does not disappear with indentation.",
                8
        );
        MESSAGE_IMAGE_URIS.put(nestedSecondLevel.id(), drawableImageUri(R.drawable.avatar_usyd));
        addComment(
                nestedPost,
                adminViewer,
                nestedSecondLevel,
                now - minutes(33),
                zh
                        ? "对，重点是结构清楚，不一定要把 reply 真做完。"
                        : "Exactly. The important part is that the structure is clear; the reply composer does not need to be fully wired yet.",
                5
        );
        addComment(
                nestedPost,
                studyBuddy,
                nestedRootReply,
                now - minutes(31),
                zh
                        ? "我再补一个二楼分支，这样就不会看起来像只有单条链路。"
                        : "I will add another second-floor branch so the thread does not look like only a single chain.",
                3
        );

        Post unswPost = createForumPost(
                FORUM_UNSW,
                studyBuddy,
                zh ? "UNSW 论坛先放什么内容比较自然？" : "What should the UNSW forum open with?",
                zh
                        ? "如果只是为了演示多论坛切换，我觉得可以先放一些和课程协作、社团活动相关的轻量帖子，不需要每个论坛都做得一样满。"
                        : "If the goal is to prove multi-forum switching, I think a few lightweight posts about coursework and student clubs are enough. Every forum does not need the same content density.",
                now - minutes(58),
                17
        );
        POST_IMAGE_URIS.put(unswPost.id, drawableImageUri(R.drawable.avatar_unsw));
        addComment(
                unswPost,
                uxPilot,
                null,
                now - minutes(51),
                zh
                        ? "对，论坛氛围稍微区分开一点，切换时会更像真实产品。"
                        : "Agreed. Giving each forum a slightly different tone makes the switch feel more like a real product.",
                6
        );
        Message unswNested = addComment(
                unswPost,
                memberViewer,
                null,
                now - minutes(49),
                zh
                        ? "那我先占一楼，顺便拿这个帖子做楼中楼样例。"
                        : "I will take the first-floor slot here and use this thread as another nesting example.",
                4
        );
        MESSAGE_IMAGE_URIS.put(unswNested.id(), drawableImageUri(R.drawable.avatar_anu));
        Message unswSecondFloor = addComment(
                unswPost,
                adminViewer,
                unswNested,
                now - minutes(47),
                zh
                        ? "二楼回复放在这里，老师切到 UNSW 也能看到缩进。"
                        : "The second-floor reply can go here so the indentation is visible even after switching to UNSW.",
                3
        );
        addComment(
                unswPost,
                treeSage,
                unswSecondFloor,
                now - minutes(46),
                zh
                        ? "三楼再跟一句，演示时就不会只剩 ANU 有深层评论。"
                        : "And a third-floor reply keeps ANU from being the only forum with deeper comments.",
                2
        );

        createForumPost(
                FORUM_UNSW,
                lateCoder,
                zh ? "下周展示前要不要先统一界面术语？" : "Should we unify the UI wording before next week's demo?",
                zh
                        ? "现在不同页面里有“帖子 / 评论 / 回复”几种叫法，最好在正式演示前统一一下，不然视觉上已经成熟了，文案却会显得散。"
                        : "We currently mix labels like post, comment, and reply across screens. It would be good to unify them before the live demo so the wording feels as polished as the UI.",
                now - minutes(45),
                9
        );

        Post usydPost = createForumPost(
                FORUM_USYD,
                treeSage,
                zh ? "USYD 论坛适合拿来放更学术一点的话题" : "The USYD forum could lean a bit more academic",
                zh
                        ? "比如数据结构讲解、课程项目复盘、或者如何把 UML 图讲得更清楚。这样不同论坛会有一点自己的气质。"
                        : "We could use it for topics like data-structure explanations, project retrospectives, or clearer UML storytelling. That gives each forum a distinct personality.",
                now - minutes(53),
                13
        );
        POST_IMAGE_URIS.put(usydPost.id, drawableImageUri(R.drawable.avatar_usyd));
        addComment(
                usydPost,
                memberViewer,
                null,
                now - minutes(47),
                zh
                        ? "这样挺好，切论坛时用户能马上感受到内容方向不同。"
                        : "That works well. Users can immediately feel the difference in content direction when they switch forums.",
                4
        );
        Message usydNested = addComment(
                usydPost,
                uxPilot,
                null,
                now - minutes(45),
                zh
                        ? "我也想在这个论坛放一个结构更清楚的评论树。"
                        : "I also want one cleaner nested thread in this forum.",
                3
        );
        MESSAGE_IMAGE_URIS.put(usydNested.id(), drawableImageUri(R.drawable.avatar_um));
        Message usydSecondFloor = addComment(
                usydPost,
                studyBuddy,
                usydNested,
                now - minutes(43),
                zh
                        ? "那这条就当二楼，顺便验证学术向帖子也能复用同一套布局。"
                        : "Then this can be the second floor, which also proves the same layout works for more academic threads.",
                2
        );
        addComment(
                usydPost,
                adminViewer,
                usydSecondFloor,
                now - minutes(42),
                zh
                        ? "三楼继续跟进一下，这样 USYD 也有完整的楼层示例。"
                        : "A third-floor follow-up here gives USYD a full nesting example too.",
                2
        );

        createForumPost(
                FORUM_USYD,
                adminViewer,
                zh ? "管理员视角下最重要的是操作路径短" : "Short moderator flows matter more than flashy UI",
                zh
                        ? "我更在意的是，从看到被举报评论，到进入帖子，再到执行隐藏动作，整个路径是否足够短。样式可以继续打磨，但流程要先顺。"
                        : "I care most about how quickly a moderator can move from seeing a reported comment to opening the thread and taking action. The visuals can keep improving, but the flow has to stay tight.",
                now - minutes(39),
                21
        );

        Post umPost = createForumPost(
                FORUM_UM,
                uxPilot,
                zh ? "UM 论坛可以更偏生活化一点" : "The UM forum can feel a bit more casual",
                zh
                        ? "比如校园生活、咖啡店推荐、社团活动分享，这样四个论坛切换时不会全是同一种项目讨论。"
                        : "It could cover things like campus life, cafe recommendations, and student activity chatter so the four forums do not all feel like clones of the same project board.",
                now - minutes(50),
                15
        );
        Message umReply = addComment(
                umPost,
                studyBuddy,
                null,
                now - minutes(44),
                zh
                        ? "这个思路不错，能让界面看起来更像真正长期在用的社区。"
                        : "I like that direction. It makes the app feel more like a community people would actually keep using.",
                5
        );
        MESSAGE_IMAGE_URIS.put(umReply.id(), drawableImageUri(R.drawable.avatar_um));
        Message umSecondFloor = addComment(
                umPost,
                memberViewer,
                umReply,
                now - minutes(41),
                zh
                        ? "而且这样也适合展示楼中楼，不一定都要围绕审核功能说。"
                        : "It also gives us another good place to show nested replies without making every thread explicitly about moderation.",
                3
        );
        addComment(
                umPost,
                adminViewer,
                umSecondFloor,
                now - minutes(39),
                zh
                        ? "再接一个三楼，生活类论坛也能自然出现深层讨论。"
                        : "One more third-floor reply makes deep discussion feel natural even in the more casual forum.",
                2
        );

        createForumPost(
                FORUM_UM,
                lateCoder,
                zh ? "如果之后真做后端扩展，论坛字段最好进模型" : "If we extend the backend later, forum ownership should move into the model",
                zh
                        ? "现在演示层里已经能切论坛了，但如果后面要持久化或者做真实多论坛查询，帖子所属论坛最好正式进 Post 数据结构。"
                        : "The demo layer already supports forum switching, but if we later add persistence or real multi-forum queries, the forum field should live directly in the Post model.",
                now - minutes(34),
                11
        );
    }

    private static String drawableImageUri(int drawableResId) {
        return "android.resource://com.example.myapplication/" + drawableResId;
    }

    private static void seedModerationState() {
        long now = System.currentTimeMillis();

        ModerationTools.addReport(hiddenExampleMessage.id(), memberViewer.id(), now - minutes(72));
        ModerationTools.addReport(hiddenExampleMessage.id(), studyBuddy.id(), now - minutes(71));
        ModerationTools.setHidden(hiddenExampleMessage.id(), adminViewer.id(), true);

        ModerationTools.addReport(queueExampleMessage.id(), uxPilot.id(), now - minutes(55));
        ModerationTools.addReport(queueExampleMessage.id(), treeSage.id(), now - minutes(53));
    }

    private static User addUser(String username, User.Role role) {
        User user = new User(UUID.randomUUID(), role, username, "demo1234");
        UserDAO.getInstance().add(user);
        return user;
    }

    private static Post createForumPost(
            String forumKey,
            User poster,
            String topic,
            String body,
            long timestamp,
            int voteScore
    ) {
        Post post = new Post(UUID.randomUUID(), poster.id(), topic);
        Message rootMessage = new Message(UUID.randomUUID(), poster.id(), post.id, timestamp, body);
        post.messages.insert(rootMessage);
        PostDAO.getInstance().add(post);

        POST_META.put(post.id, new PostMeta(forumKey, rootMessage.id()));
        MESSAGE_META.put(rootMessage.id(), new MessageMeta(null, true));
        seedVote(TargetType.POST, post.id, voteScore);
        return post;
    }

    private static Message addComment(
            Post post,
            User poster,
            Message parent,
            long timestamp,
            String content,
            int voteScore
    ) {
        Message message = new Message(UUID.randomUUID(), poster.id(), post.id, timestamp, content);
        post.messages.insert(message);
        UUID rootId = getRootMessageId(post);
        UUID parentId = parent == null ? rootId : parent.id();
        MESSAGE_META.put(message.id(), new MessageMeta(parentId, false));
        seedVote(TargetType.MESSAGE, message.id(), voteScore);
        return message;
    }

    private static void seedVote(TargetType targetType, UUID targetId, int score) {
        if (targetId != null) {
            BASE_VOTES.put(new VoteTarget(targetType, targetId), score);
        }
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

    private static void appendThread(
            Message message,
            Map<UUID, ArrayList<Message>> groupedReplies,
            ArrayList<Message> orderedMessages
    ) {
        orderedMessages.add(message);
        ArrayList<Message> children = groupedReplies.get(message.id());
        if (children == null) {
            return;
        }
        for (Message child : children) {
            appendThread(child, groupedReplies, orderedMessages);
        }
    }

    private static void sortMessagesByTimestamp(ArrayList<Message> messages) {
        messages.sort(Comparator.comparingLong(Message::timestamp));
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

    private static boolean toggleVote(TargetType targetType, UUID targetId, int direction) {
        ensurePopulated();
        User user = getCurrentUser();
        if (user == null || targetId == null || (direction != 1 && direction != -1)) {
            return false;
        }

        UserVoteKey key = new UserVoteKey(targetType, targetId, user.id());
        int currentDirection = USER_VOTES.getOrDefault(key, 0);
        if (currentDirection == direction) {
            USER_VOTES.remove(key);
            return true;
        }
        USER_VOTES.put(key, direction);
        return true;
    }

    private static int getVoteScore(TargetType targetType, UUID targetId) {
        if (targetId == null) {
            return 0;
        }
        int total = BASE_VOTES.getOrDefault(new VoteTarget(targetType, targetId), 0);
        for (Map.Entry<UserVoteKey, Integer> entry : USER_VOTES.entrySet()) {
            if (entry.getKey().targetType == targetType && targetId.equals(entry.getKey().targetId)) {
                total += entry.getValue();
            }
        }
        return total;
    }

    private static int getCurrentUserVote(TargetType targetType, UUID targetId) {
        User user = getCurrentUser();
        if (user == null || targetId == null) {
            return 0;
        }
        return USER_VOTES.getOrDefault(new UserVoteKey(targetType, targetId, user.id()), 0);
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

    private static String getForumKey(Post post) {
        PostMeta meta = post == null ? null : POST_META.get(post.id);
        return meta == null ? FORUM_ANU : meta.forumKey;
    }

    private static UUID getRootMessageId(Post post) {
        PostMeta meta = post == null ? null : POST_META.get(post.id);
        return meta == null ? null : meta.rootMessageId;
    }

    private static UUID getParentId(Message message) {
        MessageMeta meta = message == null ? null : MESSAGE_META.get(message.id());
        return meta == null ? null : meta.parentId;
    }

    private static boolean isRootMessage(Message message) {
        MessageMeta meta = message == null ? null : MESSAGE_META.get(message.id());
        return meta != null && meta.rootBody;
    }

    private static boolean isRootHidden(Post post) {
        Message rootMessage = getRootMessage(post);
        return rootMessage != null && ModerationTools.isHidden(rootMessage.id());
    }

    private static Message findMessage(Post post, UUID messageId) {
        if (post == null || messageId == null) {
            return null;
        }
        Iterator<Message> iterator = post.messages.getAll();
        while (iterator.hasNext()) {
            Message message = iterator.next();
            if (message.id().equals(messageId)) {
                return message;
            }
        }
        return null;
    }

    private static String ellipsize(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text == null ? "" : text;
        }
        return text.substring(0, maxLength - 3).trim() + "...";
    }

    private static boolean contextLanguageIsChinese() {
        return currentLanguageTag().startsWith("zh");
    }

    private static String currentLanguageTag() {
        return java.util.Locale.getDefault().toLanguageTag();
    }

    private enum TargetType {
        POST,
        MESSAGE
    }

    private record PostMeta(String forumKey, UUID rootMessageId) {
    }

    private record MessageMeta(UUID parentId, boolean rootBody) {
    }

    private record VoteTarget(TargetType targetType, UUID targetId) {
    }

    private record UserVoteKey(TargetType targetType, UUID targetId, UUID userId) {
    }
}

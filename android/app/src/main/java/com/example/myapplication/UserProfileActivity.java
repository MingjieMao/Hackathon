package com.example.myapplication;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.UUID;

import dao.model.Message;
import dao.model.Post;
import dao.model.User;

public class UserProfileActivity extends AppCompatActivity {
    public static final String EXTRA_USER_ID = "user_id";

    private enum Section {
        POSTS,
        COMMENTS,
        SAVED,
        LIKED
    }

    private UUID userId;
    private TextView textProfileAvatar;
    private TextView textProfileName;
    private TextView textProfileMeta;
    private TextView textProfileFollowingCount;
    private TextView textProfileFollowersCount;
    private TextView textProfileEngagementCount;
    private TextView textProfileContentEmpty;
    private RecyclerView recyclerProfilePosts;
    private LinearLayout layoutProfileComments;
    private Button buttonFollow;
    private Button buttonProfileTabPosts;
    private Button buttonProfileTabComments;
    private Button buttonProfileTabSaved;
    private Button buttonProfileTabLiked;
    private Section selectedSection = Section.POSTS;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        UiPreferences.applyAppearance(this);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_user_profile);
        View root = findViewById(R.id.userProfileRoot);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(
                    view.getPaddingLeft(),
                    view.getPaddingTop() + systemBars.top,
                    view.getPaddingRight(),
                    view.getPaddingBottom() + systemBars.bottom
            );
            return insets;
        });

        String id = getIntent().getStringExtra(EXTRA_USER_ID);
        userId = id == null ? null : UUID.fromString(id);
        textProfileAvatar = findViewById(R.id.textProfileAvatar);
        textProfileName = findViewById(R.id.textProfileName);
        textProfileMeta = findViewById(R.id.textProfileMeta);
        textProfileContentEmpty = findViewById(R.id.textProfileContentEmpty);
        recyclerProfilePosts = findViewById(R.id.recyclerProfilePosts);
        recyclerProfilePosts.setLayoutManager(new LinearLayoutManager(this));
        recyclerProfilePosts.setNestedScrollingEnabled(false);
        layoutProfileComments = findViewById(R.id.layoutProfileComments);
        buttonFollow = findViewById(R.id.buttonProfileFollow);
        textProfileFollowingCount = findViewById(R.id.textProfileFollowingCount);
        textProfileFollowersCount = findViewById(R.id.textProfileFollowersCount);
        textProfileEngagementCount = findViewById(R.id.textProfileEngagementCount);
        buttonProfileTabPosts = findViewById(R.id.buttonProfileTabPosts);
        buttonProfileTabComments = findViewById(R.id.buttonProfileTabComments);
        buttonProfileTabSaved = findViewById(R.id.buttonProfileTabSaved);
        buttonProfileTabLiked = findViewById(R.id.buttonProfileTabLiked);

        findViewById(R.id.buttonProfileBack).setOnClickListener(v -> finish());
        findViewById(R.id.layoutProfileFollowing).setOnClickListener(v ->
                showUserList(R.string.profile_following_label, AppData.getFollowingPeople(userId)));
        findViewById(R.id.layoutProfileFollowers).setOnClickListener(v ->
                showUserList(R.string.you_followers, AppData.getFollowerPeople(userId)));
        findViewById(R.id.layoutProfileEngagement).setOnClickListener(v ->
                showEngagementStatsDialog(userId));
        buttonProfileTabPosts.setOnClickListener(v -> selectSection(Section.POSTS));
        buttonProfileTabComments.setOnClickListener(v -> selectSection(Section.COMMENTS));
        buttonProfileTabSaved.setOnClickListener(v -> selectSection(Section.SAVED));
        buttonProfileTabLiked.setOnClickListener(v -> selectSection(Section.LIKED));
        buttonFollow.setOnClickListener(v -> {
            AppData.toggleFollow(userId);
            refresh();
        });
        refresh();
    }

    private void refresh() {
        User user = AppData.getUser(userId);
        if (user == null) {
            finish();
            return;
        }
        textProfileName.setText(AppData.getDisplayName(this, userId));
        textProfileAvatar.setText(AppData.getAvatarLetter(this, userId));
        GradientDrawable avatar = (GradientDrawable) ContextCompat.getDrawable(this, R.drawable.bg_avatar_circle).mutate();
        avatar.setColor(AppData.getAvatarColor(this, userId));
        textProfileAvatar.setBackground(avatar);
        ArrayList<Post> posts = AppData.getPostsByUser(userId);
        textProfileMeta.setText(getString(R.string.profile_meta_format, user.username(), posts.size()));
        boolean following = AppData.isFollowing(userId);
        boolean self = userId.equals(AppData.getCurrentUserId());
        textProfileFollowingCount.setText(String.valueOf(AppData.getFollowingCount(userId)));
        textProfileFollowersCount.setText(String.valueOf(AppData.getFollowerCount(userId)));
        textProfileEngagementCount.setText(String.valueOf(AppData.getReceivedEngagementCount(userId)));
        buttonFollow.setVisibility(self ? View.GONE : View.VISIBLE);
        buttonFollow.setText(following ? R.string.action_following : R.string.action_follow);
        buttonFollow.setAlpha(following ? 0.55f : 1.0f);
        refreshSelectedSection();
    }

    private void selectSection(Section section) {
        selectedSection = section;
        refreshSelectedSection();
    }

    private void refreshSelectedSection() {
        styleTabs();
        if (selectedSection == Section.COMMENTS) {
            refreshComments();
            return;
        }

        layoutProfileComments.setVisibility(View.GONE);
        ArrayList<Post> posts;
        int emptyText;
        if (selectedSection == Section.SAVED) {
            posts = getSavedProfilePosts();
            emptyText = R.string.you_saved_empty;
        } else if (selectedSection == Section.LIKED) {
            posts = getLikedProfilePosts();
            emptyText = R.string.you_liked_empty;
        } else {
            posts = AppData.getPostsByUser(userId);
            emptyText = R.string.you_posts_empty;
        }
        textProfileContentEmpty.setText(emptyText);
        textProfileContentEmpty.setVisibility(posts.isEmpty() ? View.VISIBLE : View.GONE);
        recyclerProfilePosts.setVisibility(posts.isEmpty() ? View.GONE : View.VISIBLE);
        recyclerProfilePosts.setAdapter(buildPostAdapter(posts));
    }

    private PostAdapter buildPostAdapter(ArrayList<Post> posts) {
        PostAdapter adapter = new PostAdapter(posts);
        adapter.setOnClickListener(this::openPost);
        adapter.setOnVoteClickListener((post, direction) -> {
            AppData.togglePostVote(post, direction);
            refreshSelectedSection();
        });
        adapter.setOnBookmarkClickListener(post -> {
            AppData.togglePostBookmark(post);
            refreshSelectedSection();
        });
        adapter.setOnUserClickListener(clickedUserId -> {
            if (!userId.equals(clickedUserId)) {
                Intent intent = new Intent(this, UserProfileActivity.class);
                intent.putExtra(EXTRA_USER_ID, clickedUserId.toString());
                startActivity(intent);
            }
        });
        return adapter;
    }

    private void refreshComments() {
        ArrayList<Message> comments = AppData.getMessagesByUser(userId);
        layoutProfileComments.removeAllViews();
        recyclerProfilePosts.setVisibility(View.GONE);
        layoutProfileComments.setVisibility(comments.isEmpty() ? View.GONE : View.VISIBLE);
        textProfileContentEmpty.setText(R.string.you_comments_empty);
        textProfileContentEmpty.setVisibility(comments.isEmpty() ? View.VISIBLE : View.GONE);
        for (Message message : comments) {
            layoutProfileComments.addView(makeCommentRow(message));
        }
    }

    private View makeCommentRow(Message message) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setBackgroundResource(R.drawable.bg_card);
        row.setPadding(dp(16), dp(14), dp(16), dp(14));
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        rowParams.setMargins(0, 0, 0, dp(10));
        row.setLayoutParams(rowParams);

        Post post = AppData.getPostForMessage(message);
        TextView meta = new TextView(this);
        meta.setText(post == null
                ? AppData.formatTimestamp(message.timestamp())
                : AppData.getPostCommunityLabel(this, post) + " · "
                + AppData.getPostTitle(post) + " · " + AppData.formatTimestamp(message.timestamp()));
        meta.setTextSize(13);
        meta.setTextColor(ContextCompat.getColor(this, R.color.ink_secondary));
        meta.setTypeface(Typeface.DEFAULT_BOLD);
        row.addView(meta);

        TextView body = new TextView(this);
        body.setText(message.message());
        body.setTextSize(16);
        body.setTextColor(ContextCompat.getColor(this, R.color.ink_primary));
        body.setLineSpacing(dp(3), 1.0f);
        LinearLayout.LayoutParams bodyParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        bodyParams.setMargins(0, dp(8), 0, 0);
        row.addView(body, bodyParams);

        row.setOnClickListener(v -> {
            if (post != null) {
                openPost(post);
            }
        });
        return row;
    }

    private ArrayList<Post> getSavedProfilePosts() {
        ArrayList<Post> posts = new ArrayList<>();
        for (Post post : AppData.getPostsByUser(userId)) {
            if (AppData.getPostBookmarkCount(post) > 0) {
                posts.add(post);
            }
        }
        return posts;
    }

    private ArrayList<Post> getLikedProfilePosts() {
        ArrayList<Post> posts = new ArrayList<>();
        for (Post post : AppData.getPostsByUser(userId)) {
            if (AppData.getPostVoteScore(post) > 0) {
                posts.add(post);
            }
        }
        return posts;
    }

    private ArrayList<Post> getEngagedPosts() {
        ArrayList<Post> posts = AppData.getPostsByUser(userId);
        posts.removeIf(post -> AppData.getPostVoteScore(post) + AppData.getPostBookmarkCount(post) <= 0);
        return posts;
    }

    private void styleTabs() {
        styleTab(buttonProfileTabPosts, selectedSection == Section.POSTS);
        styleTab(buttonProfileTabComments, selectedSection == Section.COMMENTS);
        styleTab(buttonProfileTabSaved, selectedSection == Section.SAVED);
        styleTab(buttonProfileTabLiked, selectedSection == Section.LIKED);
    }

    private void styleTab(Button button, boolean selected) {
        button.setTypeface(selected ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        button.setTextColor(ContextCompat.getColor(this,
                selected ? R.color.ink_primary : R.color.ink_secondary));
        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.RECTANGLE);
        background.setCornerRadius(dp(999));
        background.setColor(ContextCompat.getColor(this,
                selected ? R.color.surface : R.color.tab_bar_fill));
        background.setStroke(dp(1), ContextCompat.getColor(this, R.color.surface_border));
        button.setBackground(background);
    }

    private void showUserList(int titleResId, ArrayList<User> users) {
        LinearLayout content = buildDialogContent(titleResId);
        androidx.appcompat.app.AlertDialog[] dialogHolder = new androidx.appcompat.app.AlertDialog[1];
        if (users.isEmpty()) {
            TextView empty = makeDialogItem(getString(R.string.profile_list_empty));
            content.addView(empty);
        }
        for (User user : users) {
            TextView item = makeDialogItem(AppData.getDisplayName(this, user.id()));
            item.setOnClickListener(v -> {
                if (dialogHolder[0] != null) dialogHolder[0].dismiss();
                Intent intent = new Intent(this, UserProfileActivity.class);
                intent.putExtra(EXTRA_USER_ID, user.id().toString());
                startActivity(intent);
            });
            content.addView(item);
        }
        showListDialog(content, dialogHolder);
    }

    private void showPostList(int titleResId, ArrayList<Post> posts) {
        LinearLayout content = buildDialogContent(titleResId);
        androidx.appcompat.app.AlertDialog[] dialogHolder = new androidx.appcompat.app.AlertDialog[1];
        if (posts.isEmpty()) {
            content.addView(makeDialogItem(getString(R.string.profile_list_empty)));
        }
        for (Post post : posts) {
            TextView item = makeDialogItem(AppData.getPostTitle(post));
            item.setOnClickListener(v -> {
                if (dialogHolder[0] != null) dialogHolder[0].dismiss();
                openPost(post);
            });
            content.addView(item);
        }
        showListDialog(content, dialogHolder);
    }

    private void showEngagementStatsDialog(UUID targetUserId) {
        LinearLayout content = buildDialogContent(R.string.you_engagement);
        androidx.appcompat.app.AlertDialog[] dialogHolder = new androidx.appcompat.app.AlertDialog[1];
        content.addView(makeEngagementStatRow(R.drawable.ic_channel_24,
                R.string.engagement_posts_label, AppData.getPostsByUser(targetUserId).size()));
        content.addView(makeEngagementStatRow(R.drawable.ic_vote_up_filled_24,
                R.string.engagement_likes_label, AppData.getReceivedLikeCount(targetUserId)));
        content.addView(makeEngagementStatRow(R.drawable.ic_bookmark_filled_24,
                R.string.engagement_saves_label, AppData.getReceivedBookmarkCount(targetUserId)));
        showListDialog(content, dialogHolder);
    }

    private LinearLayout buildDialogContent(int titleResId) {
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setBackgroundResource(R.drawable.bg_avatar_picker_dialog);
        content.setPadding(dp(20), dp(18), dp(20), dp(14));

        TextView title = new TextView(this);
        title.setText(titleResId);
        title.setTextColor(ContextCompat.getColor(this, R.color.ink_primary));
        title.setTextSize(20);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        titleParams.setMargins(0, 0, 0, dp(10));
        content.addView(title, titleParams);
        return content;
    }

    private TextView makeDialogItem(String label) {
        TextView item = new TextView(this);
        item.setText(label);
        item.setTextColor(ContextCompat.getColor(this, R.color.ink_primary));
        item.setTextSize(16);
        item.setPadding(0, dp(12), 0, dp(12));
        return item;
    }

    private View makeEngagementStatRow(int iconResId, int labelResId, int count) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(12), 0, dp(12));

        ImageView icon = new ImageView(this);
        icon.setImageResource(iconResId);
        icon.setColorFilter(ContextCompat.getColor(this, R.color.ink_secondary));
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(22), dp(22));
        iconParams.setMargins(0, 0, dp(12), 0);
        row.addView(icon, iconParams);

        TextView label = new TextView(this);
        label.setText(labelResId);
        label.setTextColor(ContextCompat.getColor(this, R.color.ink_primary));
        label.setTextSize(16);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        row.addView(label, labelParams);

        TextView value = new TextView(this);
        value.setText(String.valueOf(count));
        value.setTextColor(ContextCompat.getColor(this, R.color.ink_primary));
        value.setTextSize(18);
        value.setTypeface(Typeface.DEFAULT_BOLD);
        row.addView(value);
        return row;
    }

    private void showListDialog(LinearLayout content, androidx.appcompat.app.AlertDialog[] dialogHolder) {
        TextView cancelBtn = new TextView(this);
        cancelBtn.setText(R.string.action_cancel);
        cancelBtn.setTextColor(ContextCompat.getColor(this, R.color.ink_primary));
        cancelBtn.setTextSize(14);
        cancelBtn.setTypeface(Typeface.DEFAULT_BOLD);
        cancelBtn.setPadding(dp(12), dp(10), dp(4), dp(8));
        LinearLayout.LayoutParams cancelParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        cancelParams.gravity = Gravity.END;
        cancelParams.setMargins(0, dp(8), 0, 0);

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(
                this, R.style.ThemeOverlay_App_MaterialAlertDialog)
                .setView(content)
                .create();
        dialogHolder[0] = dialog;
        cancelBtn.setOnClickListener(v -> dialog.dismiss());
        content.addView(cancelBtn, cancelParams);

        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void openPost(Post post) {
        Intent intent = new Intent(this, PostViewerActivity.class);
        intent.putExtra(PostViewerActivity.EXTRA_POST_ID, post.id.toString());
        startActivity(intent);
    }
}

package com.example.myapplication;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.Locale;
import java.util.UUID;

import dao.model.Message;
import dao.model.Post;
import dao.model.User;

public class YouFragment extends Fragment implements RefreshablePage {
    private enum Section {
        POSTS,
        COMMENTS,
        SAVED,
        LIKED
    }

    private TextView textYouAvatar;
    private TextView textYouNickname;
    private TextView textYouUid;
    private TextView textYouPostsEmpty;
    private RecyclerView recyclerYouPosts;
    private LinearLayout layoutYouComments;
    private Button buttonYouTabPosts;
    private Button buttonYouTabComments;
    private Button buttonYouTabSaved;
    private Button buttonYouTabLiked;
    private TextView textYouFollowingCount;
    private TextView textYouFollowersCount;
    private TextView textYouEngagementCount;
    private Section selectedSection = Section.POSTS;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_you, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        textYouAvatar = view.findViewById(R.id.textYouAvatar);
        textYouNickname = view.findViewById(R.id.textYouNickname);
        textYouUid = view.findViewById(R.id.textYouUid);
        textYouPostsEmpty = view.findViewById(R.id.textYouPostsEmpty);
        recyclerYouPosts = view.findViewById(R.id.recyclerYouPosts);
        layoutYouComments = view.findViewById(R.id.layoutYouComments);
        recyclerYouPosts.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerYouPosts.setNestedScrollingEnabled(false);

        ImageButton buttonYouSettings = view.findViewById(R.id.buttonYouSettings);
        LinearLayout layoutYouFollowing = view.findViewById(R.id.layoutYouFollowing);
        LinearLayout layoutYouFollowers = view.findViewById(R.id.layoutYouFollowers);
        LinearLayout layoutYouEngagement = view.findViewById(R.id.layoutYouEngagement);
        textYouFollowingCount = view.findViewById(R.id.textYouFollowingCount);
        textYouFollowersCount = view.findViewById(R.id.textYouFollowersCount);
        textYouEngagementCount = view.findViewById(R.id.textYouEngagementCount);
        buttonYouTabPosts = view.findViewById(R.id.buttonYouTabPosts);
        buttonYouTabComments = view.findViewById(R.id.buttonYouTabComments);
        buttonYouTabSaved = view.findViewById(R.id.buttonYouTabSaved);
        buttonYouTabLiked = view.findViewById(R.id.buttonYouTabLiked);

        textYouAvatar.setOnClickListener(v -> host().showAvatarPicker());
        textYouNickname.setOnClickListener(v -> host().showNicknameDialog());
        buttonYouSettings.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), SettingsActivity.class)));
        layoutYouFollowing.setOnClickListener(v ->
                showUserList(R.string.you_following,
                        AppData.getFollowingPeople(AppData.getCurrentUserId())));
        layoutYouFollowers.setOnClickListener(v ->
                showUserList(R.string.you_followers,
                        AppData.getFollowerPeople(AppData.getCurrentUserId())));
        layoutYouEngagement.setOnClickListener(v ->
                showEngagementStatsDialog(AppData.getCurrentUserId()));
        buttonYouTabPosts.setOnClickListener(v -> selectSection(Section.POSTS));
        buttonYouTabComments.setOnClickListener(v -> selectSection(Section.COMMENTS));
        buttonYouTabSaved.setOnClickListener(v -> selectSection(Section.SAVED));
        buttonYouTabLiked.setOnClickListener(v -> selectSection(Section.LIKED));

        refreshContent();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshContent();
    }

    @Override
    public void refreshContent() {
        if (!isAdded() || getView() == null || textYouAvatar == null) {
            return;
        }

        String nickname = UiPreferences.getProfileNickname(requireContext());
        String uid = UiPreferences.getProfileUid(requireContext());

        textYouNickname.setText(nickname);
        textYouUid.setText(getString(R.string.you_uid_format, uid));
        refreshProfileStats();
        String avatarImageUri = UiPreferences.getAvatarImageUri(requireContext());
        if (avatarImageUri != null) {
            try {
                Bitmap bmp = BitmapFactory.decodeFile(Uri.parse(avatarImageUri).getPath());
                if (bmp != null) {
                    RoundedBitmapDrawable d = RoundedBitmapDrawableFactory.create(getResources(), bmp);
                    d.setCircular(true);
                    textYouAvatar.setBackground(d);
                    textYouAvatar.setText("");
                } else {
                    showDefaultAvatar(nickname);
                }
            } catch (Exception ignored) {
                showDefaultAvatar(nickname);
            }
        } else {
            showDefaultAvatar(nickname);
        }
        refreshSelectedSection();
    }

    private void refreshProfileStats() {
        UUID userId = AppData.getCurrentUserId();
        textYouFollowingCount.setText(String.valueOf(AppData.getFollowingCount(userId)));
        textYouFollowersCount.setText(String.valueOf(AppData.getFollowerCount(userId)));
        textYouEngagementCount.setText(String.valueOf(AppData.getReceivedEngagementCount(userId)));
    }

    private void selectSection(Section section) {
        selectedSection = section;
        refreshSelectedSection();
    }

    private void refreshSelectedSection() {
        styleTabs();
        if (selectedSection == Section.COMMENTS) {
            refreshMyComments();
            return;
        }

        layoutYouComments.setVisibility(View.GONE);
        recyclerYouPosts.setVisibility(View.VISIBLE);
        ArrayList<Post> posts;
        int emptyText;
        if (selectedSection == Section.SAVED) {
            posts = AppData.getBookmarkedPosts();
            emptyText = R.string.you_saved_empty;
        } else if (selectedSection == Section.LIKED) {
            posts = AppData.getLikedPosts();
            emptyText = R.string.you_liked_empty;
        } else {
            posts = AppData.getPostsByUser(AppData.getCurrentUserId());
            emptyText = R.string.you_posts_empty;
        }
        textYouPostsEmpty.setText(emptyText);
        textYouPostsEmpty.setVisibility(posts.isEmpty() ? View.VISIBLE : View.GONE);
        recyclerYouPosts.setVisibility(posts.isEmpty() ? View.GONE : View.VISIBLE);
        recyclerYouPosts.setAdapter(buildPostAdapter(posts));
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
        adapter.setOnUserClickListener(this::openUserProfile);
        return adapter;
    }

    private void refreshMyComments() {
        ArrayList<Message> comments = AppData.getMessagesByUser(AppData.getCurrentUserId());
        layoutYouComments.removeAllViews();
        recyclerYouPosts.setVisibility(View.GONE);
        layoutYouComments.setVisibility(comments.isEmpty() ? View.GONE : View.VISIBLE);
        textYouPostsEmpty.setText(R.string.you_comments_empty);
        textYouPostsEmpty.setVisibility(comments.isEmpty() ? View.VISIBLE : View.GONE);
        for (Message message : comments) {
            layoutYouComments.addView(makeCommentRow(message));
        }
    }

    private View makeCommentRow(Message message) {
        LinearLayout row = new LinearLayout(requireContext());
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
        TextView meta = new TextView(requireContext());
        meta.setText(post == null
                ? AppData.formatTimestamp(message.timestamp())
                : AppData.getPostCommunityLabel(requireContext(), post) + " · "
                + AppData.getPostTitle(post) + " · " + AppData.formatTimestamp(message.timestamp()));
        meta.setTextSize(13);
        meta.setTextColor(ContextCompat.getColor(requireContext(), R.color.ink_secondary));
        meta.setTypeface(Typeface.DEFAULT_BOLD);
        row.addView(meta);

        TextView body = new TextView(requireContext());
        body.setText(message.message());
        body.setTextSize(16);
        body.setTextColor(ContextCompat.getColor(requireContext(), R.color.ink_primary));
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

    private void styleTabs() {
        styleTab(buttonYouTabPosts, selectedSection == Section.POSTS);
        styleTab(buttonYouTabComments, selectedSection == Section.COMMENTS);
        styleTab(buttonYouTabSaved, selectedSection == Section.SAVED);
        styleTab(buttonYouTabLiked, selectedSection == Section.LIKED);
    }

    private void styleTab(Button button, boolean selected) {
        if (button == null) {
            return;
        }
        button.setTypeface(selected ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        button.setTextColor(ContextCompat.getColor(requireContext(),
                selected ? R.color.ink_primary : R.color.ink_secondary));
        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.RECTANGLE);
        background.setCornerRadius(dp(999));
        background.setColor(ContextCompat.getColor(requireContext(),
                selected ? R.color.surface : R.color.tab_bar_fill));
        background.setStroke(dp(1), ContextCompat.getColor(requireContext(), R.color.surface_border));
        button.setBackground(background);
    }

    private void openPost(Post post) {
        Intent intent = new Intent(requireContext(), PostViewerActivity.class);
        intent.putExtra(PostViewerActivity.EXTRA_POST_ID, post.id.toString());
        startActivity(intent);
    }

    private void openUserProfile(UUID userId) {
        if (userId == null) {
            return;
        }
        Intent intent = new Intent(requireContext(), UserProfileActivity.class);
        intent.putExtra(UserProfileActivity.EXTRA_USER_ID, userId.toString());
        startActivity(intent);
    }

    private ArrayList<Post> getEngagedPosts() {
        ArrayList<Post> posts = AppData.getPostsByUser(AppData.getCurrentUserId());
        posts.removeIf(post -> AppData.getPostVoteScore(post) + AppData.getPostBookmarkCount(post) <= 0);
        return posts;
    }

    private void showUserList(int titleResId, ArrayList<User> users) {
        LinearLayout content = buildDialogContent(titleResId);
        androidx.appcompat.app.AlertDialog[] dialogHolder = new androidx.appcompat.app.AlertDialog[1];
        if (users.isEmpty()) {
            content.addView(makeDialogItem(getString(R.string.profile_list_empty)));
        }
        for (User user : users) {
            TextView item = makeDialogItem(AppData.getDisplayName(requireContext(), user.id()));
            item.setOnClickListener(v -> {
                if (dialogHolder[0] != null) dialogHolder[0].dismiss();
                openUserProfile(user.id());
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

    private void showEngagementStatsDialog(UUID userId) {
        LinearLayout content = buildDialogContent(R.string.you_engagement);
        androidx.appcompat.app.AlertDialog[] dialogHolder = new androidx.appcompat.app.AlertDialog[1];
        content.addView(makeEngagementStatRow(R.drawable.ic_channel_24,
                R.string.engagement_posts_label, AppData.getPostsByUser(userId).size()));
        content.addView(makeEngagementStatRow(R.drawable.ic_vote_up_filled_24,
                R.string.engagement_likes_label, AppData.getReceivedLikeCount(userId)));
        content.addView(makeEngagementStatRow(R.drawable.ic_bookmark_filled_24,
                R.string.engagement_saves_label, AppData.getReceivedBookmarkCount(userId)));
        showListDialog(content, dialogHolder);
    }

    private void showEditProfileMenu() {
        LinearLayout content = buildDialogContent(R.string.action_edit_profile);
        androidx.appcompat.app.AlertDialog[] dialogHolder = new androidx.appcompat.app.AlertDialog[1];

        TextView avatar = makeDialogItem(getString(R.string.drawer_change_avatar));
        avatar.setOnClickListener(v -> {
            if (dialogHolder[0] != null) dialogHolder[0].dismiss();
            host().showAvatarPicker();
        });
        content.addView(avatar);

        TextView nickname = makeDialogItem(getString(R.string.action_edit_nickname));
        nickname.setOnClickListener(v -> {
            if (dialogHolder[0] != null) dialogHolder[0].dismiss();
            host().showNicknameDialog();
        });
        content.addView(nickname);

        showListDialog(content, dialogHolder);
    }

    private LinearLayout buildDialogContent(int titleResId) {
        LinearLayout content = new LinearLayout(requireContext());
        content.setOrientation(LinearLayout.VERTICAL);
        content.setBackgroundResource(R.drawable.bg_avatar_picker_dialog);
        content.setPadding(dp(20), dp(18), dp(20), dp(14));

        TextView title = new TextView(requireContext());
        title.setText(titleResId);
        title.setTextColor(ContextCompat.getColor(requireContext(), R.color.ink_primary));
        title.setTextSize(20);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        titleParams.setMargins(0, 0, 0, dp(10));
        content.addView(title, titleParams);
        return content;
    }

    private TextView makeDialogItem(String label) {
        TextView item = new TextView(requireContext());
        item.setText(label);
        item.setTextColor(ContextCompat.getColor(requireContext(), R.color.ink_primary));
        item.setTextSize(16);
        item.setPadding(0, dp(12), 0, dp(12));
        return item;
    }

    private View makeEngagementStatRow(int iconResId, int labelResId, int count) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(12), 0, dp(12));

        ImageView icon = new ImageView(requireContext());
        icon.setImageResource(iconResId);
        icon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.ink_secondary));
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(22), dp(22));
        iconParams.setMargins(0, 0, dp(12), 0);
        row.addView(icon, iconParams);

        TextView label = new TextView(requireContext());
        label.setText(labelResId);
        label.setTextColor(ContextCompat.getColor(requireContext(), R.color.ink_primary));
        label.setTextSize(16);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        row.addView(label, labelParams);

        TextView value = new TextView(requireContext());
        value.setText(String.valueOf(count));
        value.setTextColor(ContextCompat.getColor(requireContext(), R.color.ink_primary));
        value.setTextSize(18);
        value.setTypeface(Typeface.DEFAULT_BOLD);
        row.addView(value);
        return row;
    }

    private void showListDialog(LinearLayout content, androidx.appcompat.app.AlertDialog[] dialogHolder) {
        TextView cancelBtn = new TextView(requireContext());
        cancelBtn.setText(R.string.action_cancel);
        cancelBtn.setTextColor(ContextCompat.getColor(requireContext(), R.color.ink_primary));
        cancelBtn.setTextSize(14);
        cancelBtn.setTypeface(Typeface.DEFAULT_BOLD);
        cancelBtn.setPadding(dp(12), dp(10), dp(4), dp(8));
        LinearLayout.LayoutParams cancelParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cancelParams.gravity = Gravity.END;
        cancelParams.setMargins(0, dp(8), 0, 0);

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(
                requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog)
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

    private MainActivity host() {
        return (MainActivity) requireActivity();
    }

    private void showDefaultAvatar(String nickname) {
        textYouAvatar.setText(getAvatarLetter(nickname));
        textYouAvatar.setBackground(makeAvatarBackground(UiPreferences.getAvatarIndex(requireContext())));
    }

    private GradientDrawable makeAvatarBackground(int avatarIndex) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(UiPreferences.getGoogleColor(avatarIndex));
        return drawable;
    }

    private String getAvatarLetter(String nickname) {
        String trimmed = nickname == null ? "" : nickname.trim();
        if (trimmed.isEmpty()) {
            return "?";
        }
        return trimmed.substring(0, 1).toUpperCase(Locale.getDefault());
    }
}

package com.example.myapplication;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import dao.model.Message;
import dao.model.Post;

public class PostViewerActivity extends AppCompatActivity {
    public static final String EXTRA_POST_ID = "post_id";

    private TextView textPostViewerMode;
    private TextView textPostViewerForum;
    private TextView textPostViewerMeta;
    private TextView textPostViewerTitle;
    private TextView textPostViewerBody;
    private TextView textPostViewerState;
    private TextView textPostViewerScore;
    private TextView textPostViewerComments;
    private TextView textCommentsEmpty;
    private TextView buttonPostUpvote;
    private TextView buttonPostDownvote;
    private Button buttonBack;
    private ImageButton buttonPostMenu;
    private RecyclerView recyclerMessages;
    private Post post;
    private Message rootMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        UiPreferences.applyAppearance(this);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_post_viewer);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.postViewerRoot), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(
                    v.getPaddingLeft(),
                    systemBars.top + v.getPaddingTop(),
                    v.getPaddingRight(),
                    systemBars.bottom + v.getPaddingBottom()
            );
            return insets;
        });

        textPostViewerMode = findViewById(R.id.textPostViewerMode);
        textPostViewerForum = findViewById(R.id.textPostViewerForum);
        textPostViewerMeta = findViewById(R.id.textPostViewerMeta);
        textPostViewerTitle = findViewById(R.id.textPostViewerTitle);
        textPostViewerBody = findViewById(R.id.textPostViewerBody);
        textPostViewerState = findViewById(R.id.textPostViewerState);
        textPostViewerScore = findViewById(R.id.textPostViewerScore);
        textPostViewerComments = findViewById(R.id.textPostViewerComments);
        textCommentsEmpty = findViewById(R.id.textCommentsEmpty);
        buttonPostUpvote = findViewById(R.id.buttonPostUpvote);
        buttonPostDownvote = findViewById(R.id.buttonPostDownvote);
        buttonBack = findViewById(R.id.buttonBack);
        buttonPostMenu = findViewById(R.id.buttonPostMenu);
        recyclerMessages = findViewById(R.id.recyclerMessages);

        recyclerMessages.setLayoutManager(new LinearLayoutManager(this));
        post = AppData.getPostById(getIntent().getStringExtra(EXTRA_POST_ID));

        buttonBack.setOnClickListener(v -> finish());
        buttonPostUpvote.setOnClickListener(v -> {
            if (AppData.togglePostVote(post, 1)) {
                refreshUi();
            }
        });
        buttonPostDownvote.setOnClickListener(v -> {
            if (AppData.togglePostVote(post, -1)) {
                refreshUi();
            }
        });
        buttonPostMenu.setOnClickListener(v -> {
            if (rootMessage != null) {
                showPostMenu(v, rootMessage);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshUi();
    }

    private void refreshUi() {
        if (post == null) {
            textPostViewerMode.setText(R.string.thread_unavailable);
            textPostViewerForum.setText(R.string.post_not_found);
            textPostViewerMeta.setText(R.string.post_not_found_body);
            textPostViewerTitle.setText("");
            textPostViewerBody.setText(R.string.post_not_found_summary);
            textPostViewerState.setVisibility(View.GONE);
            textCommentsEmpty.setVisibility(View.VISIBLE);
            recyclerMessages.setAdapter(new MessageAdapter(new ArrayList<>()));
            return;
        }

        rootMessage = AppData.getRootMessage(post);
        textPostViewerMode.setText(AppData.getCurrentModeLabel(this));
        textPostViewerForum.setText(AppData.getPostCommunityLabel(this, post));
        textPostViewerMeta.setText(getString(
                R.string.message_author_line,
                AppData.getUsername(post.poster),
                AppData.getPostTimestampLabel(post)
        ));
        textPostViewerTitle.setText(post.topic);
        textPostViewerBody.setText(AppData.getPostBody(post));
        textPostViewerComments.setText(AppData.getPostCommentChipLabel(this, post));
        textPostViewerScore.setText(String.valueOf(AppData.getPostVoteScore(post)));
        updatePostVoteColors();

        String rootState = rootMessage == null ? "" : AppData.getMessageStatus(this, rootMessage);
        textPostViewerState.setText(rootState);
        textPostViewerState.setVisibility(rootState.isEmpty() ? View.GONE : View.VISIBLE);

        ArrayList<Message> messages = AppData.getMessages(post);
        textCommentsEmpty.setVisibility(messages.isEmpty() ? View.VISIBLE : View.GONE);

        MessageAdapter adapter = new MessageAdapter(messages);
        adapter.setOnMessageActionListener(this::handleMessageAction);
        adapter.setOnMessageVoteListener((message, direction) -> {
            AppData.toggleMessageVote(message, direction);
            refreshUi();
        });
        adapter.setOnMessageReplyListener(message ->
                Toast.makeText(this, getString(R.string.toast_reply_coming_soon), Toast.LENGTH_SHORT).show());
        recyclerMessages.setAdapter(adapter);
    }

    private void updatePostVoteColors() {
        int upvoteColor = ContextCompat.getColor(this, R.color.accent_strong);
        int neutralColor = ContextCompat.getColor(this, R.color.ink_secondary);
        int downvoteColor = ContextCompat.getColor(this, R.color.danger_ink);
        int primaryColor = ContextCompat.getColor(this, R.color.ink_primary);

        int voteDirection = AppData.getCurrentUserPostVote(post);
        buttonPostUpvote.setTextColor(voteDirection > 0 ? upvoteColor : neutralColor);
        buttonPostDownvote.setTextColor(voteDirection < 0 ? downvoteColor : neutralColor);
        textPostViewerScore.setTextColor(voteDirection > 0
                ? upvoteColor
                : voteDirection < 0 ? downvoteColor : primaryColor);
    }

    private void showPostMenu(View anchor, Message message) {
        PopupMenu menu = new PopupMenu(anchor.getContext(), anchor);
        menu.getMenu().add(AppData.getMessageActionLabel(anchor.getContext(), message));
        menu.setOnMenuItemClickListener(item -> {
            handleMessageAction(message);
            return true;
        });
        menu.show();
    }

    private void handleMessageAction(Message message) {
        boolean success = AppData.isAdminMode() ? AppData.toggleHidden(message) : AppData.toggleReport(message);
        if (!success) {
            Toast.makeText(this, getString(R.string.toast_action_failed), Toast.LENGTH_SHORT).show();
            return;
        }

        String feedback = AppData.isAdminMode()
                ? (AppData.isHidden(message)
                ? getString(R.string.toast_reply_hidden)
                : getString(R.string.toast_reply_restored))
                : (AppData.hasCurrentUserReported(message)
                ? getString(R.string.toast_reply_reported)
                : getString(R.string.toast_report_removed));
        Toast.makeText(this, feedback, Toast.LENGTH_SHORT).show();
        refreshUi();
    }
}

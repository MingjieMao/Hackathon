package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
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
    private TextView textPostViewerTitle;
    private TextView textPostViewerAuthor;
    private TextView textPostViewerSummary;
    private Button buttonBack;
    private Button buttonQueue;
    private RecyclerView recyclerMessages;
    private Post post;

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
        textPostViewerTitle = findViewById(R.id.textPostViewerTitle);
        textPostViewerAuthor = findViewById(R.id.textPostViewerAuthor);
        textPostViewerSummary = findViewById(R.id.textPostViewerSummary);
        buttonBack = findViewById(R.id.buttonBack);
        buttonQueue = findViewById(R.id.buttonQueue);
        recyclerMessages = findViewById(R.id.recyclerMessages);

        recyclerMessages.setLayoutManager(new LinearLayoutManager(this));
        post = AppData.getPostById(getIntent().getStringExtra(EXTRA_POST_ID));

        buttonBack.setOnClickListener(v -> finish());
        buttonQueue.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), ModerationQueueActivity.class);
            startActivity(intent);
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
            textPostViewerTitle.setText(R.string.post_not_found);
            textPostViewerAuthor.setText(R.string.post_not_found_body);
            textPostViewerSummary.setText(R.string.post_not_found_summary);
            buttonQueue.setVisibility(View.GONE);
            recyclerMessages.setAdapter(new MessageAdapter(new ArrayList<>()));
            return;
        }

        textPostViewerMode.setText(AppData.getCurrentModeLabel(this));
        textPostViewerTitle.setText(post.topic);
        textPostViewerAuthor.setText(AppData.getPostMeta(this, post));
        textPostViewerSummary.setText(AppData.getPostSummary(this, post));

        buttonQueue.setVisibility(AppData.isAdminMode() ? View.VISIBLE : View.GONE);

        ArrayList<Message> messages = AppData.getMessages(post);
        MessageAdapter adapter = new MessageAdapter(messages);
        adapter.setOnMessageActionListener(this::handleMessageAction);
        recyclerMessages.setAdapter(adapter);
    }

    private void handleMessageAction(Message message) {
        boolean success = AppData.isAdminMode() ? AppData.toggleHidden(message) : AppData.toggleReport(message);
        if (!success) {
            Toast.makeText(this, getString(R.string.toast_action_failed), Toast.LENGTH_SHORT).show();
            return;
        }

        String feedback = AppData.isAdminMode()
                ? (AppData.isHidden(message) ? getString(R.string.toast_reply_hidden) : getString(R.string.toast_reply_restored))
                : (AppData.hasCurrentUserReported(message) ? getString(R.string.toast_reply_reported) : getString(R.string.toast_report_removed));
        Toast.makeText(this, feedback, Toast.LENGTH_SHORT).show();
        refreshUi();
    }
}

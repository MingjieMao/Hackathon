package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

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

public class ModerationQueueActivity extends AppCompatActivity {
    private String strategy = AppData.STRATEGY_OLDEST;

    private TextView textQueueSubtitle;
    private TextView textQueueEmpty;
    private Button buttonQueueBack;
    private Button buttonOldest;
    private Button buttonMost;
    private RecyclerView recyclerReportedMessages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        UiPreferences.applyAppearance(this);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_moderation_queue);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.queueRoot), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(
                    v.getPaddingLeft(),
                    systemBars.top + v.getPaddingTop(),
                    v.getPaddingRight(),
                    systemBars.bottom + v.getPaddingBottom()
            );
            return insets;
        });

        textQueueSubtitle = findViewById(R.id.textQueueSubtitle);
        textQueueEmpty = findViewById(R.id.textQueueEmpty);
        buttonQueueBack = findViewById(R.id.buttonQueueBack);
        buttonOldest = findViewById(R.id.buttonOldest);
        buttonMost = findViewById(R.id.buttonMost);
        recyclerReportedMessages = findViewById(R.id.recyclerReportedMessages);

        recyclerReportedMessages.setLayoutManager(new LinearLayoutManager(this));

        buttonQueueBack.setOnClickListener(v -> finish());
        buttonOldest.setOnClickListener(v -> {
            strategy = AppData.STRATEGY_OLDEST;
            refreshUi();
        });
        buttonMost.setOnClickListener(v -> {
            strategy = AppData.STRATEGY_MOST;
            refreshUi();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshUi();
    }

    private void refreshUi() {
        if (!AppData.isAdminMode()) {
            finish();
            return;
        }

        ArrayList<Message> messages = AppData.getReportedMessages(strategy);
        textQueueSubtitle.setText(AppData.getQueueSubtitle(this, strategy, messages.size()));
        textQueueEmpty.setVisibility(messages.isEmpty() ? View.VISIBLE : View.GONE);

        buttonOldest.setEnabled(!AppData.STRATEGY_OLDEST.equals(strategy));
        buttonOldest.setAlpha(AppData.STRATEGY_OLDEST.equals(strategy) ? 1.0f : 0.7f);
        buttonMost.setEnabled(!AppData.STRATEGY_MOST.equals(strategy));
        buttonMost.setAlpha(AppData.STRATEGY_MOST.equals(strategy) ? 1.0f : 0.7f);

        ReportedMessageAdapter adapter = new ReportedMessageAdapter(messages);
        adapter.setOnOpenThreadListener(this::openThread);
        adapter.setOnToggleHiddenListener(message -> {
            AppData.toggleHidden(message);
            refreshUi();
        });
        recyclerReportedMessages.setAdapter(adapter);
    }

    private void openThread(Message message) {
        Post post = AppData.getPostForMessage(message);
        if (post == null) {
            return;
        }

        Intent intent = new Intent(getApplicationContext(), PostViewerActivity.class);
        intent.putExtra(PostViewerActivity.EXTRA_POST_ID, post.id.toString());
        startActivity(intent);
    }
}

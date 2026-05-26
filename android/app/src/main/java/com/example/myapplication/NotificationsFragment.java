package com.example.myapplication;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class NotificationsFragment extends Fragment implements RefreshablePage {
    private RecyclerView recyclerNotifications;
    private RadioGroup radioNotificationFilters;
    private AppData.NotificationType currentFilter = AppData.NotificationType.ALL;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notifications, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recyclerNotifications = view.findViewById(R.id.recyclerNotifications);
        radioNotificationFilters = view.findViewById(R.id.radioNotificationFilters);
        recyclerNotifications.setLayoutManager(new LinearLayoutManager(requireContext()));
        prepareFilterButtons();
        radioNotificationFilters.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.filterNotificationsLikes) {
                currentFilter = AppData.NotificationType.LIKE;
            } else if (checkedId == R.id.filterNotificationsBookmarks) {
                currentFilter = AppData.NotificationType.BOOKMARK;
            } else if (checkedId == R.id.filterNotificationsComments) {
                currentFilter = AppData.NotificationType.COMMENT;
            } else if (checkedId == R.id.filterNotificationsMentions) {
                currentFilter = AppData.NotificationType.MENTION;
            } else {
                currentFilter = AppData.NotificationType.ALL;
            }
            styleFilterButtons();
            refreshContent();
        });
        styleFilterButtons();
        refreshContent();
    }

    private void prepareFilterButtons() {
        for (int i = 0; i < radioNotificationFilters.getChildCount(); i++) {
            View child = radioNotificationFilters.getChildAt(i);
            if (!(child instanceof RadioButton button)) {
                continue;
            }
            button.setButtonDrawable(null);
            button.setGravity(Gravity.CENTER);
            button.setMinWidth(dp(76));
            button.setPadding(dp(16), 0, dp(16), 0);
            button.setTextSize(15);
            button.setTypeface(Typeface.DEFAULT_BOLD);
        }
    }

    private void styleFilterButtons() {
        for (int i = 0; i < radioNotificationFilters.getChildCount(); i++) {
            View child = radioNotificationFilters.getChildAt(i);
            if (!(child instanceof RadioButton button)) {
                continue;
            }
            boolean selected = button.isChecked();
            GradientDrawable background = new GradientDrawable();
            background.setShape(GradientDrawable.RECTANGLE);
            background.setCornerRadius(dp(999));
            background.setColor(selected
                    ? Color.argb(170, 232, 236, 243)
                    : Color.argb(74, 255, 255, 255));
            background.setStroke(dp(1), selected
                    ? ContextCompat.getColor(requireContext(), R.color.tab_bar_stroke)
                    : Color.argb(128, 226, 229, 234));
            button.setBackground(background);
            button.setTextColor(ContextCompat.getColor(
                    requireContext(),
                    selected ? R.color.ink_primary : R.color.ink_secondary
            ));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshContent();
    }

    @Override
    public void refreshContent() {
        if (!isAdded() || recyclerNotifications == null) {
            return;
        }

        ArrayList<AppData.AppNotification> notifications = AppData.getNotifications(requireContext(), currentFilter);
        NotificationAdapter adapter = new NotificationAdapter(notifications);
        adapter.setOnClickListener(this::openNotification);
        recyclerNotifications.setAdapter(adapter);
    }

    private void openNotification(AppData.AppNotification notification) {
        if (!isAdded() || notification == null) {
            return;
        }
        Intent intent = new Intent(requireContext(), PostViewerActivity.class);
        intent.putExtra(PostViewerActivity.EXTRA_POST_ID, notification.postId().toString());
        startActivity(intent);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}

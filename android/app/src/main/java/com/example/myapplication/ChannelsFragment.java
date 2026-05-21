package com.example.myapplication;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import dao.model.Post;

public class ChannelsFragment extends Fragment implements RefreshablePage {
    private LinearLayout layoutChannelsHeader;
    private TextView textChannelsMode;
    private TextView textChannelsTitle;
    private TextView textChannelsSubtitle;
    private TextView textFeedEmptyTitle;
    private TextView textFeedEmptyBody;
    private ImageView imageChannelsForumAvatar;
    private ImageView imageChannelSearchIcon;
    private LinearLayout layoutChannelSearch;
    private LinearLayout layoutSearchAssistant;
    private EditText inputChannelSearch;
    private ImageButton buttonClearSearch;
    private ImageButton buttonChannelsDrawer;
    private RecyclerView recyclerPosts;
    private String currentForumKey;
    private String searchQuery = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_channels, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        layoutChannelsHeader = view.findViewById(R.id.layoutChannelsHeader);
        textChannelsMode = view.findViewById(R.id.textChannelsMode);
        textChannelsTitle = view.findViewById(R.id.textChannelsTitle);
        textChannelsSubtitle = view.findViewById(R.id.textChannelsSubtitle);
        textFeedEmptyTitle = view.findViewById(R.id.textFeedEmptyTitle);
        textFeedEmptyBody = view.findViewById(R.id.textFeedEmptyBody);
        imageChannelsForumAvatar = view.findViewById(R.id.imageChannelsForumAvatar);
        imageChannelSearchIcon = view.findViewById(R.id.imageChannelSearchIcon);
        layoutChannelSearch = view.findViewById(R.id.layoutChannelSearch);
        layoutSearchAssistant = view.findViewById(R.id.layoutSearchAssistant);
        layoutSearchAssistant.setVisibility(View.GONE);
        inputChannelSearch = view.findViewById(R.id.inputChannelSearch);
        buttonClearSearch = view.findViewById(R.id.buttonClearSearch);
        recyclerPosts = view.findViewById(R.id.recyclerPosts);
        buttonChannelsDrawer = view.findViewById(R.id.buttonChannelsDrawer);
        ImageButton buttonCreatePost = view.findViewById(R.id.buttonCreatePost);

        recyclerPosts.setLayoutManager(new LinearLayoutManager(requireContext()));
        layoutChannelSearch.setOnClickListener(v -> showKeyboard());
        inputChannelSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                searchQuery = inputChannelSearch.getText().toString();
                refreshContent();
                inputChannelSearch.clearFocus();
                hideKeyboard();
                return true;
            }
            return false;
        });
        inputChannelSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s == null ? "" : s.toString();
                refreshContent();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        buttonChannelsDrawer.setFocusable(false);
        buttonChannelsDrawer.setFocusableInTouchMode(false);
        buttonChannelsDrawer.setOnClickListener(v -> host().openDrawer());
        buttonCreatePost.setOnClickListener(v -> startActivity(new Intent(requireContext(), CreatePostActivity.class)));
        buttonClearSearch.setOnClickListener(v -> inputChannelSearch.setText(""));
        refreshContent();
    }

    private void showKeyboard() {
        inputChannelSearch.requestFocus();
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(inputChannelSearch, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(inputChannelSearch.getWindowToken(), 0);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshContent();
    }

    @Override
    public void refreshContent() {
        if (!isAdded() || getView() == null || textChannelsMode == null || recyclerPosts == null) {
            return;
        }

        layoutSearchAssistant.setVisibility(View.GONE);
        textChannelsMode.setText(AppData.getCurrentModeLabel(requireContext()));
        String selectedForumKey = AppData.getSelectedForumKey();
        if (currentForumKey == null || !currentForumKey.equals(selectedForumKey)) {
            currentForumKey = selectedForumKey;
            if (!searchQuery.isEmpty()) {
                inputChannelSearch.setText("");
                return;
            }
        }
        textChannelsTitle.setText(AppData.getSelectedForumLabel(requireContext()));
        imageChannelsForumAvatar.setImageResource(AppData.getSelectedForumAvatarResId());
        textChannelsSubtitle.setVisibility(View.GONE);
        applyHeaderTheme(selectedForumKey);
        inputChannelSearch.setHint(getString(R.string.search_channel_hint, AppData.getSelectedForumLabel(requireContext())));
        buttonClearSearch.setVisibility(searchQuery.trim().isEmpty() ? View.GONE : View.VISIBLE);

        ArrayList<Post> posts = AppData.searchPosts(requireContext(), searchQuery);
        PostAdapter adapter = new PostAdapter(posts);
        adapter.setOnClickListener(this::openPost);
        adapter.setOnVoteClickListener((post, direction) -> AppData.togglePostVote(post, direction));
        recyclerPosts.setAdapter(adapter);

        boolean empty = posts.isEmpty();
        recyclerPosts.setVisibility(empty ? View.GONE : View.VISIBLE);
        textFeedEmptyTitle.setVisibility(empty ? View.VISIBLE : View.GONE);
        textFeedEmptyBody.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (empty && !searchQuery.trim().isEmpty()) {
            textFeedEmptyTitle.setText(R.string.search_empty_title);
            textFeedEmptyBody.setText(R.string.search_empty_body);
        } else {
            textFeedEmptyTitle.setText(AppData.getFeedEmptyTitle(requireContext()));
            textFeedEmptyBody.setText(AppData.getFeedEmptyBody(requireContext()));
        }
    }

    private void openPost(Post post) {
        if (!isAdded()) {
            return;
        }

        Intent intent = new Intent(requireContext(), PostViewerActivity.class);
        intent.putExtra(PostViewerActivity.EXTRA_POST_ID, post.id.toString());
        startActivity(intent);
    }

    private MainActivity host() {
        return (MainActivity) requireActivity();
    }

    private void applyHeaderTheme(String forumKey) {
        if (!isAdded() || layoutChannelsHeader == null) {
            return;
        }

        int headerColor = ContextCompat.getColor(requireContext(), AppData.getForumHeaderColorResId(forumKey));
        int onColor = ContextCompat.getColor(requireContext(), R.color.forum_header_on);
        int onSecondary = ContextCompat.getColor(requireContext(), R.color.forum_header_on_secondary);

        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.RECTANGLE);
        background.setCornerRadius(dp(28));
        background.setColor(headerColor);
        layoutChannelsHeader.setBackground(background);

        textChannelsTitle.setTextColor(onColor);
        textChannelsSubtitle.setTextColor(onSecondary);
        buttonChannelsDrawer.setColorFilter(onColor);
        textChannelsMode.setTextColor(onColor);

        GradientDrawable modePill = new GradientDrawable();
        modePill.setShape(GradientDrawable.RECTANGLE);
        modePill.setCornerRadius(dp(999));
        modePill.setColor(Color.argb(38, 255, 255, 255));
        textChannelsMode.setBackground(modePill);

        GradientDrawable searchBackground = new GradientDrawable();
        searchBackground.setShape(GradientDrawable.RECTANGLE);
        searchBackground.setCornerRadius(dp(999));
        searchBackground.setColor(Color.argb(42, 255, 255, 255));
        layoutChannelSearch.setBackground(searchBackground);
        inputChannelSearch.setTextColor(onColor);
        inputChannelSearch.setHintTextColor(onSecondary);
        imageChannelSearchIcon.setColorFilter(onColor);
        buttonClearSearch.setColorFilter(onSecondary);

        imageChannelsForumAvatar.setImageTintList(null);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}

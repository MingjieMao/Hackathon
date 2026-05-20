package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import dao.model.Post;

public class ChannelsFragment extends Fragment implements RefreshablePage {
    private TextView textChannelsMode;
    private TextView textChannelsTitle;
    private TextView textChannelsSubtitle;
    private TextView textFeedEmptyTitle;
    private TextView textFeedEmptyBody;
    private RecyclerView recyclerPosts;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_channels, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        textChannelsMode = view.findViewById(R.id.textChannelsMode);
        textChannelsTitle = view.findViewById(R.id.textChannelsTitle);
        textChannelsSubtitle = view.findViewById(R.id.textChannelsSubtitle);
        textFeedEmptyTitle = view.findViewById(R.id.textFeedEmptyTitle);
        textFeedEmptyBody = view.findViewById(R.id.textFeedEmptyBody);
        recyclerPosts = view.findViewById(R.id.recyclerPosts);
        ImageButton buttonChannelsDrawer = view.findViewById(R.id.buttonChannelsDrawer);
        ImageButton buttonCreatePost = view.findViewById(R.id.buttonCreatePost);

        recyclerPosts.setLayoutManager(new LinearLayoutManager(requireContext()));
        buttonChannelsDrawer.setOnClickListener(v -> host().openDrawer());
        buttonCreatePost.setOnClickListener(v -> startActivity(new Intent(requireContext(), CreatePostActivity.class)));
        refreshContent();
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

        textChannelsMode.setText(AppData.getCurrentModeLabel(requireContext()));
        textChannelsTitle.setText(AppData.getSelectedForumLabel(requireContext()));
        textChannelsSubtitle.setText(AppData.getMainSubtitle(requireContext()));

        ArrayList<Post> posts = AppData.getPosts();
        PostAdapter adapter = new PostAdapter(posts);
        adapter.setOnClickListener(this::openPost);
        adapter.setOnVoteClickListener((post, direction) -> AppData.togglePostVote(post, direction));
        recyclerPosts.setAdapter(adapter);

        boolean empty = posts.isEmpty();
        recyclerPosts.setVisibility(empty ? View.GONE : View.VISIBLE);
        textFeedEmptyTitle.setVisibility(empty ? View.VISIBLE : View.GONE);
        textFeedEmptyBody.setVisibility(empty ? View.VISIBLE : View.GONE);
        textFeedEmptyTitle.setText(AppData.getFeedEmptyTitle(requireContext()));
        textFeedEmptyBody.setText(AppData.getFeedEmptyBody(requireContext()));
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
}

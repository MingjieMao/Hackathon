package com.example.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import dao.model.Post;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.ViewHolder> {
    private final List<Post> posts;
    private OnClickListener onClickListener;
    private OnVoteClickListener onVoteClickListener;

    public PostAdapter(List<Post> posts) {
        this.posts = posts;
    }

    public void setOnClickListener(OnClickListener onClickListener) {
        this.onClickListener = onClickListener;
    }

    public void setOnVoteClickListener(OnVoteClickListener onVoteClickListener) {
        this.onVoteClickListener = onVoteClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_post, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Post post = posts.get(position);
        holder.display(post);
        holder.itemView.setOnClickListener(v -> openPost(post));
        holder.buttonPostComments.setOnClickListener(v -> openPost(post));
        holder.buttonPostUpvote.setOnClickListener(v -> vote(holder, post, 1));
        holder.buttonPostDownvote.setOnClickListener(v -> vote(holder, post, -1));
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }

    public interface OnClickListener {
        void onClick(Post post);
    }

    public interface OnVoteClickListener {
        void onVote(Post post, int direction);
    }

    private void openPost(Post post) {
        if (onClickListener != null) {
            onClickListener.onClick(post);
        }
    }

    private void vote(ViewHolder holder, Post post, int direction) {
        if (onVoteClickListener != null) {
            onVoteClickListener.onVote(post, direction);
            holder.display(post);
            animateVote(direction > 0 ? holder.buttonPostUpvote : holder.buttonPostDownvote);
        }
    }

    private void animateVote(View view) {
        view.animate().cancel();
        view.setScaleX(1.0f);
        view.setScaleY(1.0f);
        view.animate()
                .scaleX(1.16f)
                .scaleY(1.16f)
                .setDuration(110L)
                .withEndAction(() -> view.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(160L)
                        .start())
                .start();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView textPostMeta;
        private final TextView textPostTitle;
        private final TextView textPostBody;
        private final TextView textPostScore;
        private final TextView buttonPostUpvote;
        private final TextView buttonPostDownvote;
        private final TextView buttonPostComments;

        ViewHolder(View view) {
            super(view);
            textPostMeta = view.findViewById(R.id.textPostMeta);
            textPostTitle = view.findViewById(R.id.textPostTitle);
            textPostBody = view.findViewById(R.id.textPostBody);
            textPostScore = view.findViewById(R.id.textPostScore);
            buttonPostUpvote = view.findViewById(R.id.buttonPostUpvote);
            buttonPostDownvote = view.findViewById(R.id.buttonPostDownvote);
            buttonPostComments = view.findViewById(R.id.buttonPostComments);
        }

        void display(Post post) {
            int upvoteColor = ContextCompat.getColor(itemView.getContext(), R.color.accent_strong);
            int neutralColor = ContextCompat.getColor(itemView.getContext(), R.color.ink_secondary);
            int downvoteColor = ContextCompat.getColor(itemView.getContext(), R.color.danger_ink);

            textPostMeta.setText(AppData.getPostFeedMeta(itemView.getContext(), post));
            textPostTitle.setText(post.topic);
            textPostBody.setText(AppData.getPostBodyPreview(post));
            textPostScore.setText(String.valueOf(AppData.getPostVoteScore(post)));
            buttonPostComments.setText(AppData.getPostCommentChipLabel(itemView.getContext(), post));

            int voteDirection = AppData.getCurrentUserPostVote(post);
            buttonPostUpvote.setTextColor(voteDirection > 0 ? upvoteColor : neutralColor);
            buttonPostDownvote.setTextColor(voteDirection < 0 ? downvoteColor : neutralColor);
            textPostScore.setTextColor(voteDirection > 0
                    ? upvoteColor
                    : voteDirection < 0 ? downvoteColor : ContextCompat.getColor(itemView.getContext(), R.color.ink_primary));
        }
    }
}

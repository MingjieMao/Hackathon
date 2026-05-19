package com.example.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import dao.model.Post;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.ViewHolder> {
    private final List<Post> posts;
    private OnClickListener onClickListener;

    public PostAdapter(List<Post> posts) {
        this.posts = posts;
    }

    public void setOnClickListener(OnClickListener onClickListener) {
        this.onClickListener = onClickListener;
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
        holder.itemView.setOnClickListener(v -> {
            if (onClickListener != null) {
                onClickListener.onClick(post);
            }
        });
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }

    public interface OnClickListener {
        void onClick(Post post);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView textPostTitle;
        private final TextView textPostAuthor;
        private final TextView textPostCount;

        ViewHolder(View view) {
            super(view);
            textPostTitle = view.findViewById(R.id.textPostTitle);
            textPostAuthor = view.findViewById(R.id.textPostAuthor);
            textPostCount = view.findViewById(R.id.textPostCount);
        }

        void display(Post post) {
            textPostTitle.setText(post.topic);
            textPostAuthor.setText(AppData.getPostMeta(itemView.getContext(), post));
            textPostCount.setText(AppData.getPostSummary(itemView.getContext(), post));
        }
    }
}

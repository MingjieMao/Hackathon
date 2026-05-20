package com.example.myapplication;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import dao.model.Message;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.ViewHolder> {
    private final List<Message> messages;
    private OnMessageActionListener onMessageActionListener;
    private OnMessageVoteListener onMessageVoteListener;
    private OnMessageReplyListener onMessageReplyListener;

    public MessageAdapter(List<Message> messages) {
        this.messages = messages;
    }

    public void setOnMessageActionListener(OnMessageActionListener onMessageActionListener) {
        this.onMessageActionListener = onMessageActionListener;
    }

    public void setOnMessageVoteListener(OnMessageVoteListener onMessageVoteListener) {
        this.onMessageVoteListener = onMessageVoteListener;
    }

    public void setOnMessageReplyListener(OnMessageReplyListener onMessageReplyListener) {
        this.onMessageReplyListener = onMessageReplyListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_message, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Message message = messages.get(position);
        holder.display(message);
        holder.buttonMessageUpvote.setOnClickListener(v -> vote(holder, message, 1));
        holder.buttonMessageDownvote.setOnClickListener(v -> vote(holder, message, -1));
        holder.buttonMessageReply.setOnClickListener(v -> {
            if (onMessageReplyListener != null) {
                onMessageReplyListener.onReply(message);
            }
        });
        holder.buttonMessageMenu.setOnClickListener(v -> showMessageMenu(holder.buttonMessageMenu, message));
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public interface OnMessageActionListener {
        void onPrimaryAction(Message message);
    }

    public interface OnMessageVoteListener {
        void onVote(Message message, int direction);
    }

    public interface OnMessageReplyListener {
        void onReply(Message message);
    }

    private void vote(ViewHolder holder, Message message, int direction) {
        if (onMessageVoteListener != null) {
            onMessageVoteListener.onVote(message, direction);
            holder.display(message);
            animateVote(direction > 0 ? holder.buttonMessageUpvote : holder.buttonMessageDownvote);
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

    private void showMessageMenu(View anchor, Message message) {
        PopupMenu menu = new PopupMenu(anchor.getContext(), anchor);
        menu.getMenu().add(AppData.getMessageActionLabel(anchor.getContext(), message));
        menu.setOnMenuItemClickListener(item -> {
            if (onMessageActionListener != null) {
                onMessageActionListener.onPrimaryAction(message);
            }
            return true;
        });
        menu.show();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final View messageRoot;
        private final TextView textMessageAvatar;
        private final View viewMessageThreadLine;
        private final TextView textMessageAuthor;
        private final TextView textMessageTimestamp;
        private final TextView textMessageState;
        private final TextView textMessageContent;
        private final TextView textMessageScore;
        private final TextView buttonMessageUpvote;
        private final TextView buttonMessageDownvote;
        private final TextView buttonMessageReply;
        private final ImageButton buttonMessageMenu;

        ViewHolder(View view) {
            super(view);
            messageRoot = view.findViewById(R.id.messageRoot);
            textMessageAvatar = view.findViewById(R.id.textMessageAvatar);
            viewMessageThreadLine = view.findViewById(R.id.viewMessageThreadLine);
            textMessageAuthor = view.findViewById(R.id.textMessageAuthor);
            textMessageTimestamp = view.findViewById(R.id.textMessageTimestamp);
            textMessageState = view.findViewById(R.id.textMessageState);
            textMessageContent = view.findViewById(R.id.textMessageContent);
            textMessageScore = view.findViewById(R.id.textMessageScore);
            buttonMessageUpvote = view.findViewById(R.id.buttonMessageUpvote);
            buttonMessageDownvote = view.findViewById(R.id.buttonMessageDownvote);
            buttonMessageReply = view.findViewById(R.id.buttonMessageReply);
            buttonMessageMenu = view.findViewById(R.id.buttonMessageMenu);
        }

        void display(Message message) {
            int upvoteColor = ContextCompat.getColor(itemView.getContext(), R.color.accent_strong);
            int neutralColor = ContextCompat.getColor(itemView.getContext(), R.color.ink_secondary);
            int downvoteColor = ContextCompat.getColor(itemView.getContext(), R.color.danger_ink);
            int primaryColor = ContextCompat.getColor(itemView.getContext(), R.color.ink_primary);

            int depth = AppData.getCommentDepth(message);
            int extraStart = Math.round(depth * 18 * itemView.getResources().getDisplayMetrics().density);
            messageRoot.setPaddingRelative(extraStart, messageRoot.getPaddingTop(), messageRoot.getPaddingEnd(), messageRoot.getPaddingBottom());

            textMessageAvatar.setText(AppData.getAvatarLetter(message.poster()));
            GradientDrawable avatarBackground = (GradientDrawable) ContextCompat.getDrawable(itemView.getContext(), R.drawable.bg_avatar_circle).mutate();
            avatarBackground.setColor(AppData.getAvatarColor(message.poster()));
            textMessageAvatar.setBackground(avatarBackground);

            textMessageAuthor.setText(AppData.getUsername(message.poster()));
            textMessageTimestamp.setText(AppData.formatTimestamp(message.timestamp()));
            String status = AppData.getMessageStatus(itemView.getContext(), message);
            textMessageState.setText(status);
            textMessageState.setVisibility(status.isEmpty() ? View.GONE : View.VISIBLE);
            textMessageContent.setText(message.message());
            textMessageScore.setText(String.valueOf(AppData.getMessageVoteScore(message)));
            viewMessageThreadLine.setVisibility(depth > 0 ? View.VISIBLE : View.GONE);

            int voteDirection = AppData.getCurrentUserMessageVote(message);
            buttonMessageUpvote.setTextColor(voteDirection > 0 ? upvoteColor : neutralColor);
            buttonMessageDownvote.setTextColor(voteDirection < 0 ? downvoteColor : neutralColor);
            textMessageScore.setTextColor(voteDirection > 0
                    ? upvoteColor
                    : voteDirection < 0 ? downvoteColor : primaryColor);
        }
    }
}

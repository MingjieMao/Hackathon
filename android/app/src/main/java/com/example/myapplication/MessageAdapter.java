package com.example.myapplication;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
        holder.buttonMessageMenu.setOnClickListener(v -> {
            if (onMessageActionListener != null) {
                onMessageActionListener.onPrimaryAction(message);
            }
        });
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

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final View messageRoot;
        private final LinearLayout layoutMessageDepthRails;
        private final TextView textMessageAvatar;
        private final View viewMessageThreadLine;
        private final TextView textMessageAuthor;
        private final TextView textMessageTimestamp;
        private final TextView textMessageState;
        private final TextView textMessageContent;
        private final TextView textMessageScore;
        private final TextView textMessageReplyCount;
        private final ImageView imageMessageReplyIcon;
        private final ImageButton buttonMessageUpvote;
        private final ImageButton buttonMessageDownvote;
        private final View buttonMessageReply;
        private final ImageButton buttonMessageMenu;

        ViewHolder(View view) {
            super(view);
            messageRoot = view.findViewById(R.id.messageRoot);
            layoutMessageDepthRails = view.findViewById(R.id.layoutMessageDepthRails);
            textMessageAvatar = view.findViewById(R.id.textMessageAvatar);
            viewMessageThreadLine = view.findViewById(R.id.viewMessageThreadLine);
            textMessageAuthor = view.findViewById(R.id.textMessageAuthor);
            textMessageTimestamp = view.findViewById(R.id.textMessageTimestamp);
            textMessageState = view.findViewById(R.id.textMessageState);
            textMessageContent = view.findViewById(R.id.textMessageContent);
            textMessageScore = view.findViewById(R.id.textMessageScore);
            textMessageReplyCount = view.findViewById(R.id.textMessageReplyCount);
            imageMessageReplyIcon = view.findViewById(R.id.imageMessageReplyIcon);
            buttonMessageUpvote = view.findViewById(R.id.buttonMessageUpvote);
            buttonMessageDownvote = view.findViewById(R.id.buttonMessageDownvote);
            buttonMessageReply = view.findViewById(R.id.buttonMessageReply);
            buttonMessageMenu = view.findViewById(R.id.buttonMessageMenu);
        }

        void display(Message message) {
            int upvoteColor = ContextCompat.getColor(itemView.getContext(), R.color.vote_up);
            int neutralColor = ContextCompat.getColor(itemView.getContext(), R.color.ink_secondary);
            int downvoteColor = ContextCompat.getColor(itemView.getContext(), R.color.vote_down);
            int primaryColor = ContextCompat.getColor(itemView.getContext(), R.color.ink_primary);
            int reportColor = ContextCompat.getColor(itemView.getContext(), R.color.danger_ink);

            int depth = AppData.getCommentDepth(message);
            messageRoot.setPaddingRelative(0, messageRoot.getPaddingTop(), messageRoot.getPaddingEnd(), messageRoot.getPaddingBottom());
            bindDepthRails(depth);

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
            textMessageReplyCount.setText(String.valueOf(AppData.getMessageReplyCount(message)));
            viewMessageThreadLine.setVisibility(depth > 0 ? View.VISIBLE : View.GONE);

            int voteDirection = AppData.getCurrentUserMessageVote(message);
            buttonMessageUpvote.setImageResource(voteDirection > 0
                    ? R.drawable.ic_vote_up_filled_24
                    : R.drawable.ic_vote_up_outline_24);
            buttonMessageDownvote.setImageResource(voteDirection < 0
                    ? R.drawable.ic_vote_down_filled_24
                    : R.drawable.ic_vote_down_outline_24);
            boolean reported = AppData.hasCurrentUserReported(message);
            buttonMessageMenu.setImageResource(reported ? R.drawable.ic_flag_24 : R.drawable.ic_flag_outline_24);
            buttonMessageMenu.setColorFilter(reported ? reportColor : neutralColor);
            imageMessageReplyIcon.setColorFilter(primaryColor);
            textMessageReplyCount.setTextColor(primaryColor);
            textMessageScore.setTextColor(voteDirection > 0
                    ? upvoteColor
                    : voteDirection < 0 ? downvoteColor : primaryColor);
        }

        private void bindDepthRails(int depth) {
            layoutMessageDepthRails.removeAllViews();
            layoutMessageDepthRails.setVisibility(depth > 0 ? View.VISIBLE : View.GONE);
            if (depth <= 0) {
                return;
            }

            float density = itemView.getResources().getDisplayMetrics().density;
            int railWidth = Math.round(18 * density);
            int lineWidth = Math.max(1, Math.round(2 * density));
            int lineColor = ContextCompat.getColor(itemView.getContext(), R.color.surface_border);
            for (int i = 0; i < depth; i++) {
                LinearLayout rail = new LinearLayout(itemView.getContext());
                rail.setGravity(android.view.Gravity.CENTER);
                layoutMessageDepthRails.addView(rail, new LinearLayout.LayoutParams(railWidth, ViewGroup.LayoutParams.MATCH_PARENT));

                View line = new View(itemView.getContext());
                line.setBackgroundColor(lineColor);
                rail.addView(line, new LinearLayout.LayoutParams(lineWidth, ViewGroup.LayoutParams.MATCH_PARENT));
            }
        }
    }
}

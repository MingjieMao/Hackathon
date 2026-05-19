package com.example.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import dao.model.Message;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.ViewHolder> {
    private final List<Message> messages;
    private OnMessageActionListener onMessageActionListener;

    public MessageAdapter(List<Message> messages) {
        this.messages = messages;
    }

    public void setOnMessageActionListener(OnMessageActionListener onMessageActionListener) {
        this.onMessageActionListener = onMessageActionListener;
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
        holder.buttonMessageMenu.setOnClickListener(v -> showMessageMenu(holder.buttonMessageMenu, message));
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public interface OnMessageActionListener {
        void onPrimaryAction(Message message);
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
        private final TextView textMessageAuthor;
        private final TextView textMessageTimestamp;
        private final TextView textMessageState;
        private final TextView textMessageContent;
        private final ImageButton buttonMessageMenu;

        ViewHolder(View view) {
            super(view);
            textMessageAuthor = view.findViewById(R.id.textMessageAuthor);
            textMessageTimestamp = view.findViewById(R.id.textMessageTimestamp);
            textMessageState = view.findViewById(R.id.textMessageState);
            textMessageContent = view.findViewById(R.id.textMessageContent);
            buttonMessageMenu = view.findViewById(R.id.buttonMessageMenu);
        }

        void display(Message message) {
            textMessageAuthor.setText(AppData.getUsername(message.poster()));
            textMessageTimestamp.setText(AppData.formatTimestamp(message.timestamp()));
            textMessageState.setText(AppData.getMessageStatus(itemView.getContext(), message));
            textMessageContent.setText(message.message());
        }
    }
}

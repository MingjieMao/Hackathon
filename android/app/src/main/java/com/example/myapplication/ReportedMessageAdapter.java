package com.example.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.List;

import dao.model.Message;
import dao.model.Post;

public class ReportedMessageAdapter extends RecyclerView.Adapter<ReportedMessageAdapter.ViewHolder> {
    private final List<Message> messages;
    private OnOpenThreadListener onOpenThreadListener;
    private OnToggleHiddenListener onToggleHiddenListener;

    public ReportedMessageAdapter(List<Message> messages) {
        this.messages = messages;
    }

    public void setOnOpenThreadListener(OnOpenThreadListener onOpenThreadListener) {
        this.onOpenThreadListener = onOpenThreadListener;
    }

    public void setOnToggleHiddenListener(OnToggleHiddenListener onToggleHiddenListener) {
        this.onToggleHiddenListener = onToggleHiddenListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_reported_message, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Message message = messages.get(position);
        holder.display(message);
        holder.itemView.setOnClickListener(v -> {
            if (onOpenThreadListener != null) {
                onOpenThreadListener.onOpenThread(message);
            }
        });
        holder.buttonReportedAction.setOnClickListener(v -> {
            if (onToggleHiddenListener != null) {
                onToggleHiddenListener.onToggleHidden(message);
            }
        });
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public interface OnOpenThreadListener {
        void onOpenThread(Message message);
    }

    public interface OnToggleHiddenListener {
        void onToggleHidden(Message message);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView textReportedThread;
        private final TextView textReportedAuthor;
        private final TextView textReportedContent;
        private final TextView textReportedMeta;
        private final Button buttonReportedAction;

        ViewHolder(View view) {
            super(view);
            textReportedThread = view.findViewById(R.id.textReportedThread);
            textReportedAuthor = view.findViewById(R.id.textReportedAuthor);
            textReportedContent = view.findViewById(R.id.textReportedContent);
            textReportedMeta = view.findViewById(R.id.textReportedMeta);
            buttonReportedAction = view.findViewById(R.id.buttonReportedAction);
        }

        void display(Message message) {
            Post post = AppData.getPostForMessage(message);
            textReportedThread.setText(post == null ? AppData.getThreadUnknownLabel(itemView.getContext()) : AppData.getPostTitle(post));
            textReportedAuthor.setText(AppData.formatMessageAuthorLine(itemView.getContext(), message));
            textReportedContent.setText(message.message());
            textReportedMeta.setText(AppData.getReportedCardMeta(itemView.getContext(), message));
            buttonReportedAction.setText(AppData.getMessageActionLabel(itemView.getContext(), message));
            MaterialButton btn = (MaterialButton) buttonReportedAction;
            btn.setIcon(ContextCompat.getDrawable(itemView.getContext(), R.drawable.ic_hidden_24));
            int iconColor = AppData.isHidden(message)
                    ? ContextCompat.getColor(itemView.getContext(), R.color.danger_ink)
                    : ContextCompat.getColor(itemView.getContext(), R.color.ink_faint);
            btn.setIconTint(android.content.res.ColorStateList.valueOf(iconColor));
        }
    }
}

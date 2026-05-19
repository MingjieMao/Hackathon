package com.example.myapplication;

import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.util.Locale;

public class YouFragment extends Fragment implements RefreshablePage {
    private TextView textYouAvatar;
    private TextView textYouNickname;
    private TextView textYouUid;
    private TextView textYouMode;
    private Button buttonYouToggleMode;
    private Button buttonYouQueue;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_you, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ImageButton buttonYouDrawer = view.findViewById(R.id.buttonYouDrawer);
        textYouAvatar = view.findViewById(R.id.textYouAvatar);
        textYouNickname = view.findViewById(R.id.textYouNickname);
        textYouUid = view.findViewById(R.id.textYouUid);
        textYouMode = view.findViewById(R.id.textYouMode);
        Button buttonEditAvatar = view.findViewById(R.id.buttonEditAvatar);
        Button buttonEditNickname = view.findViewById(R.id.buttonEditNickname);
        buttonYouToggleMode = view.findViewById(R.id.buttonYouToggleMode);
        buttonYouQueue = view.findViewById(R.id.buttonYouQueue);

        buttonYouDrawer.setOnClickListener(v -> host().openDrawer());
        buttonEditAvatar.setOnClickListener(v -> host().showAvatarPicker());
        buttonEditNickname.setOnClickListener(v -> host().showNicknameDialog());
        buttonYouToggleMode.setOnClickListener(v -> host().toggleViewerMode());
        buttonYouQueue.setOnClickListener(v -> host().openModerationQueue());

        refreshContent();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshContent();
    }

    @Override
    public void refreshContent() {
        if (!isAdded() || getView() == null || textYouAvatar == null || buttonYouQueue == null) {
            return;
        }

        String nickname = UiPreferences.getProfileNickname(requireContext());
        String uid = UiPreferences.getProfileUid(requireContext());

        textYouNickname.setText(nickname);
        textYouUid.setText(getString(R.string.you_uid_format, uid));
        textYouMode.setText(AppData.getCurrentModeLabel(requireContext()));
        textYouAvatar.setText(getAvatarLetter(nickname));
        textYouAvatar.setBackground(makeAvatarBackground(UiPreferences.getAvatarIndex(requireContext())));

        buttonYouToggleMode.setText(getString(
                AppData.isAdminMode() ? R.string.drawer_switch_member : R.string.drawer_switch_admin
        ));
        buttonYouQueue.setEnabled(AppData.isAdminMode());
        buttonYouQueue.setAlpha(AppData.isAdminMode() ? 1.0f : 0.65f);
    }

    private MainActivity host() {
        return (MainActivity) requireActivity();
    }

    private GradientDrawable makeAvatarBackground(int avatarIndex) {
        GradientDrawable drawable = (GradientDrawable) ContextCompat.getDrawable(requireContext(), R.drawable.bg_avatar_circle).mutate();
        drawable.setColor(ContextCompat.getColor(requireContext(), getAvatarColorRes(avatarIndex)));
        return drawable;
    }

    private int getAvatarColorRes(int avatarIndex) {
        switch (avatarIndex) {
            case 1:
                return R.color.avatar_coral;
            case 2:
                return R.color.avatar_ocean;
            case 3:
                return R.color.avatar_plum;
            case 4:
                return R.color.avatar_sand;
            default:
                return R.color.avatar_moss;
        }
    }

    private String getAvatarLetter(String nickname) {
        String trimmed = nickname == null ? "" : nickname.trim();
        if (trimmed.isEmpty()) {
            return "?";
        }
        return trimmed.substring(0, 1).toUpperCase(Locale.getDefault());
    }
}

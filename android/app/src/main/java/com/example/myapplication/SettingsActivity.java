package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class SettingsActivity extends AppCompatActivity {
    private TextView textSettingsLanguageValue;
    private TextView textSettingsThemeValue;
    private TextView textSettingsMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        UiPreferences.applyAppearance(this);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);

        View root = findViewById(R.id.settingsRoot);
        int left = root.getPaddingLeft();
        int top = root.getPaddingTop();
        int right = root.getPaddingRight();
        int bottom = root.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(left, bars.top + top, right, bars.bottom + bottom);
            return insets;
        });

        ImageButton buttonBack = findViewById(R.id.buttonSettingsBack);
        LinearLayout rowLanguage = findViewById(R.id.rowSettingsLanguage);
        LinearLayout rowTheme = findViewById(R.id.rowSettingsTheme);
        LinearLayout layoutAdmin = findViewById(R.id.layoutSettingsAdmin);
        Button buttonQueue = findViewById(R.id.buttonSettingsModerationQueue);
        Button buttonSwitchAccount = findViewById(R.id.buttonSettingsSwitchAccount);
        Button buttonLogout = findViewById(R.id.buttonSettingsLogout);
        textSettingsMode = findViewById(R.id.textSettingsMode);
        textSettingsLanguageValue = findViewById(R.id.textSettingsLanguageValue);
        textSettingsThemeValue = findViewById(R.id.textSettingsThemeValue);

        buttonBack.setOnClickListener(v -> finish());
        rowLanguage.setOnClickListener(v -> showLanguageDialog());
        rowTheme.setOnClickListener(v -> showThemeDialog());
        layoutAdmin.setVisibility(View.VISIBLE);
        textSettingsMode.setText(AppData.isAdminMode() ? R.string.mode_admin : R.string.mode_member);
        buttonQueue.setVisibility(AppData.isAdminMode() ? View.VISIBLE : View.GONE);
        buttonQueue.setOnClickListener(v -> startActivity(new Intent(this, ModerationQueueActivity.class)));
        buttonSwitchAccount.setOnClickListener(v -> signOutToLogin());
        buttonLogout.setOnClickListener(v -> signOutToLogin());
        refreshLabels();
    }

    private void refreshLabels() {
        boolean chinese = "zh-CN".equals(UiPreferences.getLanguageTag(this));
        textSettingsLanguageValue.setText(chinese
                ? R.string.settings_language_zh
                : R.string.settings_language_en);
        textSettingsThemeValue.setText(UiPreferences.isDarkTheme(this)
                ? R.string.settings_theme_dark
                : R.string.settings_theme_light);
    }

    private void showLanguageDialog() {
        String[] labels = {
                getString(R.string.settings_language_en),
                getString(R.string.settings_language_zh)
        };
        int checked = "zh-CN".equals(UiPreferences.getLanguageTag(this)) ? 1 : 0;
        new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_App_MaterialAlertDialog)
                .setTitle(R.string.settings_language)
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    UiPreferences.setLanguageTag(this, which == 1 ? "zh-CN" : "en");
                    UiPreferences.applyAppearance(this);
                    dialog.dismiss();
                    recreate();
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void showThemeDialog() {
        String[] labels = {
                getString(R.string.settings_theme_light),
                getString(R.string.settings_theme_dark)
        };
        int checked = UiPreferences.isDarkTheme(this) ? 1 : 0;
        new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_App_MaterialAlertDialog)
                .setTitle(R.string.settings_theme)
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    UiPreferences.setDarkTheme(this, which == 1);
                    UiPreferences.applyAppearance(this);
                    dialog.dismiss();
                    recreate();
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void signOutToLogin() {
        UiPreferences.clearLoginSession(this);
        AppData.setAdminMode(false);
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}

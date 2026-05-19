package com.example.myapplication;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

public final class UiPreferences {
    private static final String PREFS = "ui_preferences";
    private static final String KEY_UID = "profile_uid";
    private static final String KEY_NICKNAME = "profile_nickname";
    private static final String KEY_AVATAR_INDEX = "profile_avatar_index";
    private static final String KEY_LANGUAGE_TAG = "language_tag";
    private static final String KEY_DARK_THEME = "dark_theme";

    private static final String DEFAULT_UID = "uid_2100_001";
    private static final String DEFAULT_NICKNAME = "Campus Buddy";
    private static final String DEFAULT_LANGUAGE_TAG = "en";

    private UiPreferences() {
    }

    public static void applyAppearance(Context context) {
        AppCompatDelegate.setDefaultNightMode(isDarkTheme(context)
                ? AppCompatDelegate.MODE_NIGHT_YES
                : AppCompatDelegate.MODE_NIGHT_NO);
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(getLanguageTag(context)));
    }

    public static String getProfileUid(Context context) {
        return prefs(context).getString(KEY_UID, DEFAULT_UID);
    }

    public static void setProfileUid(Context context, String uid) {
        prefs(context).edit().putString(KEY_UID, uid).apply();
    }

    public static String getProfileNickname(Context context) {
        return prefs(context).getString(KEY_NICKNAME, DEFAULT_NICKNAME);
    }

    public static void setProfileNickname(Context context, String nickname) {
        prefs(context).edit().putString(KEY_NICKNAME, nickname).apply();
    }

    public static int getAvatarIndex(Context context) {
        return prefs(context).getInt(KEY_AVATAR_INDEX, 0);
    }

    public static void setAvatarIndex(Context context, int avatarIndex) {
        prefs(context).edit().putInt(KEY_AVATAR_INDEX, avatarIndex).apply();
    }

    public static String getLanguageTag(Context context) {
        return prefs(context).getString(KEY_LANGUAGE_TAG, DEFAULT_LANGUAGE_TAG);
    }

    public static void setLanguageTag(Context context, String languageTag) {
        prefs(context).edit().putString(KEY_LANGUAGE_TAG, languageTag).apply();
    }

    public static boolean isDarkTheme(Context context) {
        return prefs(context).getBoolean(KEY_DARK_THEME, false);
    }

    public static void setDarkTheme(Context context, boolean darkTheme) {
        prefs(context).edit().putBoolean(KEY_DARK_THEME, darkTheme).apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}

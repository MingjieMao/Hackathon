package com.example.myapplication;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;

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
    private static final String DEFAULT_LANGUAGE_TAG = "en";
    private static final int[] GOOGLE_COLORS = {
            Color.rgb(205, 220, 255),
            Color.rgb(208, 234, 236),
            Color.rgb(135, 228, 215),
            Color.rgb(179, 239, 162),
            Color.rgb(255, 228, 126),
            Color.rgb(255, 213, 190),
            Color.rgb(255, 204, 188),
            Color.rgb(255, 206, 216),
            Color.rgb(255, 198, 238),
            Color.rgb(228, 205, 255)
    };

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
        return prefs(context).getString(KEY_NICKNAME, defaultNickname());
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

    public static int getAvatarColor(Context context) {
        return getGoogleColor(getAvatarIndex(context));
    }

    public static int getGoogleColor(int colorIndex) {
        return GOOGLE_COLORS[normalizeColorIndex(colorIndex)];
    }

    public static int getGoogleColorCount() {
        return GOOGLE_COLORS.length;
    }

    public static String getLanguageTag(Context context) {
        return prefs(context).getString(KEY_LANGUAGE_TAG, DEFAULT_LANGUAGE_TAG);
    }

    public static void setLanguageTag(Context context, String languageTag) {
        prefs(context).edit().putString(KEY_LANGUAGE_TAG, languageTag).commit();
    }

    public static boolean isDarkTheme(Context context) {
        return prefs(context).getBoolean(KEY_DARK_THEME, false);
    }

    public static void setDarkTheme(Context context, boolean darkTheme) {
        prefs(context).edit().putBoolean(KEY_DARK_THEME, darkTheme).commit();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static String defaultNickname() {
        return java.util.Locale.getDefault().getLanguage().equals("zh") ? "校园伙伴" : "Campus Buddy";
    }

    private static int normalizeColorIndex(int colorIndex) {
        if (colorIndex < 0) {
            return 0;
        }
        if (colorIndex >= GOOGLE_COLORS.length) {
            return GOOGLE_COLORS.length - 1;
        }
        return colorIndex;
    }
}

package com.example.myapplication;

import android.content.res.ColorStateList;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.PathInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.splashscreen.SplashScreen;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class MainActivity extends AppCompatActivity {
    private static final String EXTRA_START_PAGE = "start_page";
    private static final String STATE_CURRENT_PAGE = "current_page";
    private static final long TAB_ANIMATION_MS = 320L;

    private DrawerLayout drawerRoot;
    private View mainContent;
    private View drawerPanel;
    private ViewPager2 viewPager;
    private View navShell;
    private View navIndicator;
    private View tabChannels;
    private View tabNotifications;
    private View tabYou;
    private ImageView iconChannels;
    private TextView channelDots;
    private ImageView iconNotifications;
    private ImageView iconYou;
    private TextView labelChannels;
    private TextView labelNotifications;
    private TextView labelYou;
    private Button buttonDrawerForumAnu;
    private Button buttonDrawerForumUnsw;
    private Button buttonDrawerForumUsyd;
    private Button buttonDrawerForumUm;
    private Button buttonDrawerSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        UiPreferences.applyAppearance(this);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        AppData.ensurePopulated();

        drawerRoot = findViewById(R.id.drawerRoot);
        mainContent = findViewById(R.id.mainContent);
        drawerPanel = findViewById(R.id.drawerPanel);
        viewPager = findViewById(R.id.viewPager);
        navShell = findViewById(R.id.navShell);
        navIndicator = findViewById(R.id.navIndicator);
        tabChannels = findViewById(R.id.tabChannels);
        tabNotifications = findViewById(R.id.tabNotifications);
        tabYou = findViewById(R.id.tabYou);
        iconChannels = findViewById(R.id.iconChannels);
        channelDots = findViewById(R.id.channelDots);
        iconNotifications = findViewById(R.id.iconNotifications);
        iconYou = findViewById(R.id.iconYou);
        labelChannels = findViewById(R.id.labelChannels);
        labelNotifications = findViewById(R.id.labelNotifications);
        labelYou = findViewById(R.id.labelYou);

        buttonDrawerForumAnu = findViewById(R.id.buttonDrawerForumAnu);
        buttonDrawerForumUnsw = findViewById(R.id.buttonDrawerForumUnsw);
        buttonDrawerForumUsyd = findViewById(R.id.buttonDrawerForumUsyd);
        buttonDrawerForumUm = findViewById(R.id.buttonDrawerForumUm);
        buttonDrawerSettings = findViewById(R.id.buttonDrawerSettings);

        applyInsets(mainContent, true);
        applyInsets(drawerPanel, false);

        buttonDrawerForumAnu.setOnClickListener(v -> switchForum(AppData.FORUM_ANU));
        buttonDrawerForumUnsw.setOnClickListener(v -> switchForum(AppData.FORUM_UNSW));
        buttonDrawerForumUsyd.setOnClickListener(v -> switchForum(AppData.FORUM_USYD));
        buttonDrawerForumUm.setOnClickListener(v -> switchForum(AppData.FORUM_UM));
        buttonDrawerSettings.setOnClickListener(v -> showSettingsDialog());

        configurePager();
        configureTabBar();

        int startPage = resolveStartPage(savedInstanceState);
        setPage(startPage, false);
        refreshDrawerUi();
        notifyPagesChanged();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshDrawerUi();
        notifyPagesChanged();
    }

    @Override
    public void onBackPressed() {
        if (drawerRoot.isDrawerOpen(GravityCompat.START)) {
            drawerRoot.closeDrawer(GravityCompat.START);
            return;
        }
        if (viewPager.getCurrentItem() != MainPagerAdapter.PAGE_CHANNELS) {
            setPage(MainPagerAdapter.PAGE_CHANNELS, true);
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_CURRENT_PAGE, viewPager.getCurrentItem());
    }

    public void openDrawer() {
        drawerRoot.openDrawer(GravityCompat.START);
    }

    public void toggleViewerMode() {
        AppData.toggleViewerMode();
        refreshDrawerUi();
        notifyPagesChanged();
    }

    public void switchForum(String forumKey) {
        AppData.setSelectedForum(forumKey);
        refreshDrawerUi();
        drawerRoot.closeDrawer(GravityCompat.START);
        setPage(MainPagerAdapter.PAGE_CHANNELS, false);
        notifyPagesChanged();
    }

    public void openModerationQueue() {
        if (!AppData.isAdminMode()) {
            Toast.makeText(this, getString(R.string.toast_switch_admin_required), Toast.LENGTH_SHORT).show();
            return;
        }

        drawerRoot.closeDrawer(GravityCompat.START);
        startActivity(new Intent(getApplicationContext(), ModerationQueueActivity.class));
    }

    public void showAvatarPicker() {
        String[] labels = getResources().getStringArray(R.array.avatar_option_labels);
        int selected = UiPreferences.getAvatarIndex(this);

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_avatar_title)
                .setSingleChoiceItems(labels, selected, (dialog, which) -> {
                    UiPreferences.setAvatarIndex(this, which);
                    notifyPagesChanged();
                    Toast.makeText(this, getString(R.string.toast_profile_saved), Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    public void showNicknameDialog() {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        input.setHint(R.string.dialog_nickname_hint);
        input.setText(UiPreferences.getProfileNickname(this));
        input.setSelection(input.getText().length());
        int horizontal = dp(16);
        int vertical = dp(12);
        input.setPadding(horizontal, vertical, horizontal, vertical);

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_edit_nickname_title)
                .setView(input)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_save, null)
                .create();

        dialog.setOnShowListener(unused -> dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String value = input.getText().toString().trim();
                    if (value.isEmpty()) {
                        input.setError(getString(R.string.dialog_nickname_hint));
                        return;
                    }
                    UiPreferences.setProfileNickname(this, value);
                    notifyPagesChanged();
                    Toast.makeText(this, getString(R.string.toast_profile_saved), Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                }));
        dialog.show();
    }

    public void showSettingsDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_settings, drawerRoot, false);
        RadioButton radioLanguageEnglish = dialogView.findViewById(R.id.radioLanguageEnglish);
        RadioButton radioLanguageChinese = dialogView.findViewById(R.id.radioLanguageChinese);
        RadioButton radioThemeLight = dialogView.findViewById(R.id.radioThemeLight);
        RadioButton radioThemeDark = dialogView.findViewById(R.id.radioThemeDark);

        if ("zh-CN".equals(UiPreferences.getLanguageTag(this))) {
            radioLanguageChinese.setChecked(true);
        } else {
            radioLanguageEnglish.setChecked(true);
        }

        if (UiPreferences.isDarkTheme(this)) {
            radioThemeDark.setChecked(true);
        } else {
            radioThemeLight.setChecked(true);
        }

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.settings_title)
                .setView(dialogView)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_save, null)
                .create();

        dialog.setOnShowListener(unused -> dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String newLanguage = radioLanguageChinese.isChecked() ? "zh-CN" : "en";
                    boolean newDarkTheme = radioThemeDark.isChecked();

                    boolean changed = !newLanguage.equals(UiPreferences.getLanguageTag(this))
                            || newDarkTheme != UiPreferences.isDarkTheme(this);

                    UiPreferences.setLanguageTag(this, newLanguage);
                    UiPreferences.setDarkTheme(this, newDarkTheme);
                    dialog.dismiss();
                    drawerRoot.closeDrawer(GravityCompat.START);

                    if (changed) {
                        restartForAppearanceChange();
                    }
                }));

        dialog.show();
    }

    public void showLanguageDialog() {
        String[] languages = {
                getString(R.string.settings_language_en),
                getString(R.string.settings_language_zh)
        };
        int checked = "zh-CN".equals(UiPreferences.getLanguageTag(this)) ? 1 : 0;

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.settings_language)
                .setSingleChoiceItems(languages, checked, (dialog, which) -> {
                    String newLanguage = which == 1 ? "zh-CN" : "en";
                    boolean changed = !newLanguage.equals(UiPreferences.getLanguageTag(this));
                    UiPreferences.setLanguageTag(this, newLanguage);
                    dialog.dismiss();
                    if (changed) {
                        restartForAppearanceChange();
                    }
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    public void showThemeDialog() {
        String[] themes = {
                getString(R.string.settings_theme_light),
                getString(R.string.settings_theme_dark)
        };
        int checked = UiPreferences.isDarkTheme(this) ? 1 : 0;

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.settings_theme)
                .setSingleChoiceItems(themes, checked, (dialog, which) -> {
                    boolean newDarkTheme = which == 1;
                    boolean changed = newDarkTheme != UiPreferences.isDarkTheme(this);
                    UiPreferences.setDarkTheme(this, newDarkTheme);
                    dialog.dismiss();
                    if (changed) {
                        restartForAppearanceChange();
                    }
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void configurePager() {
        viewPager.setAdapter(new MainPagerAdapter(this));
        viewPager.setOffscreenPageLimit(3);
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels);
                syncIndicatorTo(position + positionOffset, false);
            }

            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateTabSelection(position);
            }
        });
    }

    private void playChannelDotsHint() {
        if (channelDots == null) {
            return;
        }

        channelDots.animate().cancel();
        iconChannels.animate().cancel();

        channelDots.setAlpha(0f);
        channelDots.setScaleX(0.85f);
        channelDots.setScaleY(0.85f);
        channelDots.setTranslationY(0f);

        iconChannels.setScaleX(1.0f);
        iconChannels.setScaleY(1.0f);

        ObjectAnimator dotsAlpha = ObjectAnimator.ofFloat(
                channelDots,
                View.ALPHA,
                0f,
                1f,
                1f,
                0f
        );

        ObjectAnimator dotsScaleX = ObjectAnimator.ofFloat(
                channelDots,
                View.SCALE_X,
                0.85f,
                1.35f,
                1.35f,
                0.85f
        );

        ObjectAnimator dotsScaleY = ObjectAnimator.ofFloat(
                channelDots,
                View.SCALE_Y,
                0.85f,
                1.35f,
                1.35f,
                0.85f
        );

        ObjectAnimator iconScaleX = ObjectAnimator.ofFloat(
                iconChannels,
                View.SCALE_X,
                1.0f,
                1.28f,
                1.28f,
                1.0f
        );

        ObjectAnimator iconScaleY = ObjectAnimator.ofFloat(
                iconChannels,
                View.SCALE_Y,
                1.0f,
                1.28f,
                1.28f,
                1.0f
        );

        AnimatorSet set = new AnimatorSet();
        set.playTogether(dotsAlpha, dotsScaleX, dotsScaleY, iconScaleX, iconScaleY);
        set.setDuration(1000L);
        set.setInterpolator(new PathInterpolator(0.22f, 1f, 0.36f, 1f));
        set.start();
    }

    private void playNotificationShakeHint() {
        iconNotifications.animate().cancel();
        iconNotifications.setTranslationX(0f);

        ObjectAnimator shake = ObjectAnimator.ofFloat(
                iconNotifications,
                View.TRANSLATION_X,
                0f,
                -dp(6),
                dp(6),
                -dp(6),
                dp(6),
                0f,
                -dp(6),
                dp(6),
                -dp(6),
                dp(6),
                0f
        );

        shake.setDuration(720L);
        shake.setInterpolator(new PathInterpolator(0.36f, 0f, 0.66f, -0.56f));
        shake.start();
    }

    private void playYouSlideHint() {
        iconYou.animate().cancel();

        iconYou.setAlpha(0f);
        iconYou.setTranslationX(-dp(22));

        ObjectAnimator slide = ObjectAnimator.ofFloat(
                iconYou,
                View.TRANSLATION_X,
                -dp(22),
                0f
        );

        ObjectAnimator fade = ObjectAnimator.ofFloat(
                iconYou,
                View.ALPHA,
                0f,
                1f
        );

        AnimatorSet set = new AnimatorSet();
        set.playTogether(slide, fade);
        set.setDuration(1000L);
        set.setInterpolator(new PathInterpolator(0.22f, 1f, 0.36f, 1f));
        set.start();
    }

    private void playTabHint(int page) {
        if (page == MainPagerAdapter.PAGE_CHANNELS) {
            playChannelDotsHint();
        } else if (page == MainPagerAdapter.PAGE_NOTIFICATIONS) {
            playNotificationShakeHint();
        } else if (page == MainPagerAdapter.PAGE_YOU) {
            playYouSlideHint();
        }
    }

    private void configureTabBar() {
        tabChannels.setOnClickListener(v -> {
            setPage(MainPagerAdapter.PAGE_CHANNELS, true);
            playTabHint(MainPagerAdapter.PAGE_CHANNELS);
        });

        tabNotifications.setOnClickListener(v -> {
            setPage(MainPagerAdapter.PAGE_NOTIFICATIONS, true);
            playTabHint(MainPagerAdapter.PAGE_NOTIFICATIONS);
        });

        tabYou.setOnClickListener(v -> {
            setPage(MainPagerAdapter.PAGE_YOU, true);
            playTabHint(MainPagerAdapter.PAGE_YOU);
        });

        navShell.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            if (left != oldLeft || right != oldRight) {
                syncIndicatorTo(viewPager.getCurrentItem(), false);
            }
        });
    }

    private void setPage(int page, boolean smoothScroll) {
        if (viewPager.getCurrentItem() != page) {
            viewPager.setCurrentItem(page, smoothScroll);
            return;
        }

        updateTabSelection(page);
        syncIndicatorTo(page, true);
    }

    private void refreshDrawerUi() {
        styleForumButton(buttonDrawerForumAnu, AppData.FORUM_ANU);
        styleForumButton(buttonDrawerForumUnsw, AppData.FORUM_UNSW);
        styleForumButton(buttonDrawerForumUsyd, AppData.FORUM_USYD);
        styleForumButton(buttonDrawerForumUm, AppData.FORUM_UM);
    }

    private void notifyPagesChanged() {
        for (Fragment fragment : getSupportFragmentManager().getFragments()) {
            if (fragment instanceof RefreshablePage && fragment.getView() != null) {
                ((RefreshablePage) fragment).refreshContent();
            }
        }
    }

    private void restartForAppearanceChange() {
        UiPreferences.applyAppearance(this);
        recreate();
    }

    private int resolveStartPage(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            return savedInstanceState.getInt(STATE_CURRENT_PAGE, MainPagerAdapter.PAGE_CHANNELS);
        }
        return getIntent().getIntExtra(EXTRA_START_PAGE, MainPagerAdapter.PAGE_CHANNELS);
    }

    private void applyInsets(View target, boolean includeBottom) {
        int start = target.getPaddingStart();
        int top = target.getPaddingTop();
        int end = target.getPaddingEnd();
        int bottom = target.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(target, (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPaddingRelative(
                    start,
                    top + systemBars.top,
                    end,
                    bottom + (includeBottom ? systemBars.bottom : 0)
            );
            return insets;
        });
    }

    private void updateTabSelection(int page) {
        updateTab(tabChannels, iconChannels, labelChannels, page == MainPagerAdapter.PAGE_CHANNELS);
        updateTab(tabNotifications, iconNotifications, labelNotifications, page == MainPagerAdapter.PAGE_NOTIFICATIONS);
        updateTab(tabYou, iconYou, labelYou, page == MainPagerAdapter.PAGE_YOU);
    }

    private void updateTab(View tab, ImageView icon, TextView label, boolean selected) {
        int activeColor = ContextCompat.getColor(this, R.color.nav_active);
        int inactiveColor = ContextCompat.getColor(this, R.color.nav_inactive);

        icon.setColorFilter(selected ? activeColor : inactiveColor);
        label.setTextColor(selected ? activeColor : inactiveColor);
        label.setTypeface(selected ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        tab.setAlpha(selected ? 1.0f : 0.82f);
        if (selected) {
            icon.animate()
                    .scaleX(1.18f)
                    .scaleY(1.18f)
                    .setDuration(120L)
                    .withEndAction(() -> icon.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(180L)
                            .setInterpolator(new PathInterpolator(0.22f, 1f, 0.36f, 1f))
                            .start())
                    .start();
        } else {
            icon.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120L).start();
        }
        tab.animate()
                .scaleX(selected ? 1.0f : 0.98f)
                .scaleY(selected ? 1.0f : 0.98f)
                .setDuration(TAB_ANIMATION_MS)
                .setInterpolator(new PathInterpolator(0.22f, 1f, 0.36f, 1f))
                .start();
    }

    private void styleForumButton(Button button, String forumKey) {
        boolean selected = AppData.isSelectedForum(forumKey);
        int backgroundColor = ContextCompat.getColor(this, selected ? R.color.accent_strong : R.color.surface_alt);
        int textColor = ContextCompat.getColor(this, selected ? R.color.white : R.color.ink_primary);
        button.setText(AppData.getForumLabel(this, forumKey));
        Drawable avatar = ContextCompat.getDrawable(this, AppData.getForumAvatarResId(forumKey));
        if (avatar != null) {
            int size = dp(28);
            avatar.setBounds(0, 0, size, size);
        }
        button.setCompoundDrawables(avatar, null, null, null);
        button.setCompoundDrawablePadding(dp(12));
        button.setGravity(android.view.Gravity.CENTER_VERTICAL);
        button.setBackgroundTintList(ColorStateList.valueOf(backgroundColor));
        button.setTextColor(textColor);
        button.setAlpha(selected ? 1.0f : 0.92f);
    }

    private void syncIndicatorTo(float pageOffset, boolean animate) {
        if (navShell.getWidth() == 0) {
            navShell.post(() -> syncIndicatorTo(pageOffset, animate));
            return;
        }

        int innerWidth = navShell.getWidth() - navShell.getPaddingLeft() - navShell.getPaddingRight();
        int slotWidth = innerWidth / 3;
        int indicatorInset = dp(1);
        int indicatorWidth = slotWidth - (indicatorInset * 2);
        int centeredOffset = indicatorInset;

        ViewGroup.LayoutParams layoutParams = navIndicator.getLayoutParams();
        if (layoutParams.width != indicatorWidth) {
            layoutParams.width = indicatorWidth;
            navIndicator.setLayoutParams(layoutParams);
        }

        float targetX = centeredOffset + (slotWidth * pageOffset);
        if (animate) {
            navIndicator.animate()
                    .translationX(targetX)
                    .setDuration(TAB_ANIMATION_MS)
                    .setInterpolator(new PathInterpolator(0.22f, 1f, 0.36f, 1f))
                    .start();
            return;
        }

        navIndicator.setTranslationX(targetX);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}

package com.example.myapplication;

import android.content.Intent;
import android.graphics.Typeface;
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
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

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
    private ImageView iconNotifications;
    private ImageView iconYou;
    private TextView labelChannels;
    private TextView labelNotifications;
    private TextView labelYou;
    private Button buttonDrawerQueue;
    private Button buttonDrawerMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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
        iconNotifications = findViewById(R.id.iconNotifications);
        iconYou = findViewById(R.id.iconYou);
        labelChannels = findViewById(R.id.labelChannels);
        labelNotifications = findViewById(R.id.labelNotifications);
        labelYou = findViewById(R.id.labelYou);

        Button buttonDrawerHome = findViewById(R.id.buttonDrawerHome);
        buttonDrawerQueue = findViewById(R.id.buttonDrawerQueue);
        buttonDrawerMode = findViewById(R.id.buttonDrawerMode);
        Button buttonDrawerDrafts = findViewById(R.id.buttonDrawerDrafts);
        Button buttonDrawerTeamSpace = findViewById(R.id.buttonDrawerTeamSpace);
        Button buttonDrawerSettings = findViewById(R.id.buttonDrawerSettings);

        applyInsets(mainContent, true);
        applyInsets(drawerPanel, false);

        buttonDrawerHome.setOnClickListener(v -> {
            drawerRoot.closeDrawer(GravityCompat.START);
            setPage(MainPagerAdapter.PAGE_CHANNELS, true);
        });
        buttonDrawerQueue.setOnClickListener(v -> openModerationQueue());
        buttonDrawerMode.setOnClickListener(v -> {
            toggleViewerMode();
            drawerRoot.closeDrawer(GravityCompat.START);
        });
        buttonDrawerDrafts.setOnClickListener(v ->
                Toast.makeText(this, getString(R.string.toast_feature_coming_soon), Toast.LENGTH_SHORT).show());
        buttonDrawerTeamSpace.setOnClickListener(v ->
                Toast.makeText(this, getString(R.string.toast_feature_coming_soon), Toast.LENGTH_SHORT).show());
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

    private void configurePager() {
        viewPager.setAdapter(new MainPagerAdapter(this));
        viewPager.setOffscreenPageLimit(3);
        viewPager.setPageTransformer((page, position) -> {
            float abs = Math.abs(position);
            float emphasis = 1f - Math.min(abs, 1f);
            page.setAlpha(0.78f + (emphasis * 0.22f));
            page.setTranslationX(-position * page.getWidth() * 0.05f);
            page.setScaleX(0.97f + (emphasis * 0.03f));
            page.setScaleY(0.97f + (emphasis * 0.03f));
        });
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

    private void configureTabBar() {
        tabChannels.setOnClickListener(v -> setPage(MainPagerAdapter.PAGE_CHANNELS, true));
        tabNotifications.setOnClickListener(v -> setPage(MainPagerAdapter.PAGE_NOTIFICATIONS, true));
        tabYou.setOnClickListener(v -> setPage(MainPagerAdapter.PAGE_YOU, true));

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
        buttonDrawerMode.setText(getString(
                AppData.isAdminMode() ? R.string.drawer_switch_member : R.string.drawer_switch_admin
        ));
        buttonDrawerQueue.setEnabled(AppData.isAdminMode());
        buttonDrawerQueue.setAlpha(AppData.isAdminMode() ? 1.0f : 0.72f);
    }

    private void notifyPagesChanged() {
        for (Fragment fragment : getSupportFragmentManager().getFragments()) {
            if (fragment instanceof RefreshablePage && fragment.getView() != null) {
                ((RefreshablePage) fragment).refreshContent();
            }
        }
    }

    private void restartForAppearanceChange() {
        recreate();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
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
        tab.animate()
                .scaleX(selected ? 1.0f : 0.98f)
                .scaleY(selected ? 1.0f : 0.98f)
                .setDuration(TAB_ANIMATION_MS)
                .setInterpolator(new PathInterpolator(0.22f, 1f, 0.36f, 1f))
                .start();
    }

    private void syncIndicatorTo(float pageOffset, boolean animate) {
        if (navShell.getWidth() == 0) {
            navShell.post(() -> syncIndicatorTo(pageOffset, animate));
            return;
        }

        int innerWidth = navShell.getWidth() - navShell.getPaddingLeft() - navShell.getPaddingRight();
        int slotWidth = innerWidth / 3;
        int indicatorWidth = slotWidth - dp(10);
        int centeredOffset = (slotWidth - indicatorWidth) / 2;

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

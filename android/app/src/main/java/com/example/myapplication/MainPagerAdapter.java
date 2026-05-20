package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class MainPagerAdapter extends FragmentStateAdapter {
    public static final int PAGE_CHANNELS = 0;
    public static final int PAGE_NOTIFICATIONS = 1;
    public static final int PAGE_YOU = 2;

    public MainPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case PAGE_NOTIFICATIONS:
                return new NotificationsFragment();
            case PAGE_YOU:
                return new YouFragment();
            case PAGE_CHANNELS:
            default:
                return new ChannelsFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 3;
    }
}

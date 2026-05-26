package com.example.myapplication;

import android.content.Context;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class CampusMarketRepository {
    private static final ArrayList<SchoolMarket> MARKETS = new ArrayList<>();
    private static final ArrayList<LeaderboardEntry> LEADERBOARD = new ArrayList<>();

    static {
        MARKETS.add(new SchoolMarket(
                AppData.FORUM_ANU,
                32,
                148,
                740,
                R.string.market_anu_pitch,
                listOf(R.string.market_anu_trigger_1, R.string.market_anu_trigger_2, R.string.market_anu_trigger_3),
                candles(
                        candle(126, 138, 120, 132),
                        candle(132, 148, 129, 144),
                        candle(144, 153, 138, 149),
                        candle(149, 161, 145, 158),
                        candle(158, 167, 154, 162),
                        candle(162, 170, 159, 164)
                )
        ));
        MARKETS.add(new SchoolMarket(
                AppData.FORUM_UNSW,
                41,
                176,
                820,
                R.string.market_unsw_pitch,
                listOf(R.string.market_unsw_trigger_1, R.string.market_unsw_trigger_2, R.string.market_unsw_trigger_3),
                candles(
                        candle(184, 196, 180, 192),
                        candle(192, 208, 189, 205),
                        candle(205, 226, 201, 221),
                        candle(221, 234, 214, 229),
                        candle(229, 233, 218, 225),
                        candle(225, 228, 206, 211)
                )
        ));
        MARKETS.add(new SchoolMarket(
                AppData.FORUM_USYD,
                36,
                131,
                910,
                R.string.market_usyd_pitch,
                listOf(R.string.market_usyd_trigger_1, R.string.market_usyd_trigger_2, R.string.market_usyd_trigger_3),
                candles(
                        candle(154, 166, 151, 161),
                        candle(161, 170, 158, 167),
                        candle(167, 171, 160, 164),
                        candle(164, 176, 162, 173),
                        candle(173, 182, 171, 180),
                        candle(180, 191, 178, 188)
                )
        ));
        MARKETS.add(new SchoolMarket(
                AppData.FORUM_UM,
                28,
                118,
                610,
                R.string.market_um_pitch,
                listOf(R.string.market_um_trigger_1, R.string.market_um_trigger_2, R.string.market_um_trigger_3),
                candles(
                        candle(112, 124, 109, 118),
                        candle(118, 129, 116, 126),
                        candle(126, 132, 121, 123),
                        candle(123, 136, 121, 131),
                        candle(131, 141, 129, 137),
                        candle(137, 148, 135, 143)
                )
        ));

        LEADERBOARD.add(new LeaderboardEntry("KoalaQuant", AppData.FORUM_UNSW, 1488, 9));
        LEADERBOARD.add(new LeaderboardEntry("LateNightLong", AppData.FORUM_USYD, 1412, 7));
        LEADERBOARD.add(new LeaderboardEntry("DuckLiquidity", AppData.FORUM_ANU, 1368, 6));
        LEADERBOARD.add(new LeaderboardEntry("StudioBreakout", AppData.FORUM_UM, 1337, 5));
        LEADERBOARD.add(new LeaderboardEntry("MidtermWhale", AppData.FORUM_UNSW, 1294, 4));
        LEADERBOARD.add(new LeaderboardEntry("CafeGamma", AppData.FORUM_USYD, 1226, 4));
        LEADERBOARD.add(new LeaderboardEntry("QuietAccumulator", AppData.FORUM_ANU, 1162, 3));
        LEADERBOARD.add(new LeaderboardEntry("LanewayScalper", AppData.FORUM_UM, 1114, 2));
    }

    private CampusMarketRepository() {
    }

    @NonNull
    public static List<SchoolMarket> getMarkets() {
        return Collections.unmodifiableList(MARKETS);
    }

    @NonNull
    public static SchoolMarket getMarket(String forumKey) {
        for (SchoolMarket market : MARKETS) {
            if (market.getForumKey().equals(forumKey)) {
                return market;
            }
        }
        return MARKETS.get(0);
    }

    @NonNull
    public static List<LeaderboardEntry> getLeaderboard() {
        return Collections.unmodifiableList(LEADERBOARD);
    }

    private static ArrayList<Integer> listOf(int... values) {
        ArrayList<Integer> list = new ArrayList<>();
        for (int value : values) {
            list.add(value);
        }
        return list;
    }

    private static ArrayList<MarketCandle> candles(MarketCandle... candles) {
        ArrayList<MarketCandle> items = new ArrayList<>();
        Collections.addAll(items, candles);
        return items;
    }

    private static MarketCandle candle(int open, int high, int low, int close) {
        return new MarketCandle(open, high, low, close);
    }

    public static final class SchoolMarket {
        private final String forumKey;
        private final int postsToday;
        private final int repliesToday;
        private final int likesToday;
        private final int pitchResId;
        private final ArrayList<Integer> triggerResIds;
        private final ArrayList<MarketCandle> candles;

        private SchoolMarket(
                String forumKey,
                int postsToday,
                int repliesToday,
                int likesToday,
                int pitchResId,
                ArrayList<Integer> triggerResIds,
                ArrayList<MarketCandle> candles
        ) {
            this.forumKey = forumKey;
            this.postsToday = postsToday;
            this.repliesToday = repliesToday;
            this.likesToday = likesToday;
            this.pitchResId = pitchResId;
            this.triggerResIds = triggerResIds;
            this.candles = candles;
        }

        public String getForumKey() {
            return forumKey;
        }

        public int getPostsToday() {
            return postsToday;
        }

        public int getRepliesToday() {
            return repliesToday;
        }

        public int getLikesToday() {
            return likesToday;
        }

        public int getCurrentPrice() {
            return candles.get(candles.size() - 1).close;
        }

        /**
         * Heat Index = 100 + 2×posts + 1.5×replies + 0.8×netVotes + 5×activityMomentum
         * activityMomentum = max(0, (posts + replies/5 - 30) / 10)
         */
        public int getHeatIndex() {
            float momentum = Math.max(0f, (postsToday + repliesToday / 5f - 30f) / 10f);
            return Math.round(100 + 2f * postsToday + 1.5f * repliesToday + 0.8f * likesToday + 5f * momentum);
        }

        public double getDayChangePercent() {
            if (candles.size() < 2) {
                return 0d;
            }
            int previousClose = candles.get(candles.size() - 2).close;
            if (previousClose == 0) {
                return 0d;
            }
            return ((double) (getCurrentPrice() - previousClose) * 100d) / previousClose;
        }

        @NonNull
        public List<MarketCandle> getCandles() {
            return Collections.unmodifiableList(candles);
        }

        @NonNull
        public String getPitch(Context context) {
            return context.getString(pitchResId);
        }

        @NonNull
        public List<String> getTriggers(Context context) {
            ArrayList<String> values = new ArrayList<>();
            for (int resId : triggerResIds) {
                values.add(context.getString(resId));
            }
            return values;
        }
    }

    public static final class MarketCandle {
        public final int open;
        public final int high;
        public final int low;
        public final int close;

        public MarketCandle(int open, int high, int low, int close) {
            this.open = open;
            this.high = high;
            this.low = low;
            this.close = close;
        }
    }

    public static final class LeaderboardEntry {
        private final String username;
        private final String forumKey;
        private final int totalAssets;
        private final int streakDays;

        private LeaderboardEntry(String username, String forumKey, int totalAssets, int streakDays) {
            this.username = username;
            this.forumKey = forumKey;
            this.totalAssets = totalAssets;
            this.streakDays = streakDays;
        }

        public String getUsername() {
            return username;
        }

        public String getForumKey() {
            return forumKey;
        }

        public int getTotalAssets() {
            return totalAssets;
        }

        public int getProfit() {
            return totalAssets - MarketPortfolioStore.DAILY_FLOOR_TOKENS;
        }

        public double getReturnPercent() {
            return ((double) getProfit() * 100d) / MarketPortfolioStore.DAILY_FLOOR_TOKENS;
        }

        public int getStreakDays() {
            return streakDays;
        }
    }
}

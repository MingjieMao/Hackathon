package com.example.myapplication;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

public final class MarketPortfolioStore {
    public static final int DAILY_FLOOR_TOKENS = 1000;
    public static final long LIVE_TICK_INTERVAL_MS = 1800L;

    private static final String PREFS = "market_portfolio";
    private static final String KEY_CASH_BALANCE = "cash_balance";
    private static final String KEY_LAST_RESET_DATE = "last_reset_date";
    private static final String KEY_POSITION_UNITS_PREFIX = "position_units_";
    private static final String KEY_POSITION_COST_PREFIX = "position_cost_";
    private static final String KEY_PRICE_OFFSET_PREFIX = "price_offset_";
    private static final String KEY_TRADE_COUNT_PREFIX = "trade_count_";
    private static final String KEY_LAST_LIVE_TICK_AT = "last_live_tick_at";
    private static final String KEY_LIVE_TICK_COUNT_PREFIX = "live_tick_count_";
    private static final String KEY_SHORT_UNITS_PREFIX = "short_units_";
    private static final String KEY_SHORT_COST_PREFIX = "short_cost_";

    private MarketPortfolioStore() {
    }

    public static void ensureDailyReset(Context context) {
        SharedPreferences prefs = prefs(context);
        String today = todayStamp();
        String savedDay = prefs.getString(KEY_LAST_RESET_DATE, "");
        if (today.equals(savedDay)) {
            if (!prefs.contains(KEY_CASH_BALANCE)) {
                prefs.edit().putInt(KEY_CASH_BALANCE, DAILY_FLOOR_TOKENS).apply();
            }
            return;
        }

        int currentCash = prefs.getInt(KEY_CASH_BALANCE, DAILY_FLOOR_TOKENS);
        if (currentCash < DAILY_FLOOR_TOKENS) {
            currentCash = DAILY_FLOOR_TOKENS;
        }

        prefs.edit()
                .putInt(KEY_CASH_BALANCE, currentCash)
                .putString(KEY_LAST_RESET_DATE, today)
                .apply();
    }

    public static int getCashBalance(Context context) {
        ensureDailyReset(context);
        return prefs(context).getInt(KEY_CASH_BALANCE, DAILY_FLOOR_TOKENS);
    }

    public static int getLivePrice(Context context, String forumKey, int basePrice) {
        ensureDailyReset(context);
        int offset = prefs(context).getInt(KEY_PRICE_OFFSET_PREFIX + forumKey, 0);
        return Math.max(1, basePrice + offset);
    }

    public static boolean advanceMarketTick(Context context) {
        ensureDailyReset(context);

        SharedPreferences prefs = prefs(context);
        long now = System.currentTimeMillis();
        long lastTickAt = prefs.getLong(KEY_LAST_LIVE_TICK_AT, 0L);
        if (lastTickAt > 0L && now - lastTickAt < LIVE_TICK_INTERVAL_MS) {
            return false;
        }

        int stepCount = lastTickAt <= 0L
                ? 1
                : Math.max(1, Math.min(6, (int) ((now - lastTickAt) / LIVE_TICK_INTERVAL_MS)));

        SharedPreferences.Editor editor = prefs.edit();
        for (String forumKey : AppData.getForumKeys()) {
            CampusMarketRepository.SchoolMarket market = CampusMarketRepository.getMarket(forumKey);
            int basePrice = market.getCurrentPrice();
            int currentOffset = prefs.getInt(KEY_PRICE_OFFSET_PREFIX + forumKey, 0);
            int tickCount = prefs.getInt(KEY_LIVE_TICK_COUNT_PREFIX + forumKey, 0);
            int maxOffset = Math.max(26, Math.round(basePrice * 0.22f));

            for (int step = 0; step < stepCount; step++) {
                currentOffset = clamp(
                        currentOffset + computeLiveDelta(forumKey, tickCount + step, currentOffset, basePrice, maxOffset),
                        -maxOffset,
                        maxOffset
                );
            }

            editor.putInt(KEY_PRICE_OFFSET_PREFIX + forumKey, currentOffset);
            editor.putInt(KEY_LIVE_TICK_COUNT_PREFIX + forumKey, tickCount + stepCount);
        }

        editor.putLong(KEY_LAST_LIVE_TICK_AT, now).apply();
        return true;
    }

    @NonNull
    public static PortfolioSnapshot getPortfolio(Context context) {
        ensureDailyReset(context);

        SharedPreferences prefs = prefs(context);
        ArrayList<PositionSnapshot> positions = new ArrayList<>();
        ArrayList<ShortPositionSnapshot> shortPositions = new ArrayList<>();
        int marketValue = 0;
        int costBasis = 0;
        int shortPnl = 0;

        for (String forumKey : AppData.getForumKeys()) {
            int units = prefs.getInt(KEY_POSITION_UNITS_PREFIX + forumKey, 0);
            int totalCost = prefs.getInt(KEY_POSITION_COST_PREFIX + forumKey, 0);

            CampusMarketRepository.SchoolMarket market = CampusMarketRepository.getMarket(forumKey);
            int currentPrice = getLivePrice(context, forumKey, market.getCurrentPrice());

            if (units > 0 && totalCost > 0) {
                int currentValue = units * currentPrice;
                positions.add(new PositionSnapshot(forumKey, units, totalCost, currentPrice, currentValue));
                marketValue += currentValue;
                costBasis += totalCost;
            }

            int shortUnits = prefs.getInt(KEY_SHORT_UNITS_PREFIX + forumKey, 0);
            int shortTotalCost = prefs.getInt(KEY_SHORT_COST_PREFIX + forumKey, 0);
            if (shortUnits > 0 && shortTotalCost > 0) {
                int entryPrice = Math.round((float) shortTotalCost / shortUnits);
                int pnl = (entryPrice - currentPrice) * shortUnits;
                shortPositions.add(new ShortPositionSnapshot(forumKey, shortUnits, entryPrice, currentPrice, pnl));
                shortPnl += pnl;
            }
        }

        positions.sort(Comparator.comparingInt(PositionSnapshot::getCurrentValue).reversed());

        int cashBalance = prefs.getInt(KEY_CASH_BALANCE, DAILY_FLOOR_TOKENS);
        int totalAssets = cashBalance + marketValue;
        int openPnl = marketValue - costBasis + shortPnl;

        return new PortfolioSnapshot(cashBalance, marketValue, totalAssets, openPnl, positions, shortPositions);
    }

    @NonNull
    public static TradeResult openShort(Context context, String forumKey, int requestedSpend, int unitPrice) {
        ensureDailyReset(context);
        if (requestedSpend <= 0 || unitPrice <= 0) {
            return new TradeResult(TradeResult.STATUS_INVALID_AMOUNT, requestedSpend, 0, 0, 0, getCashBalance(context), unitPrice, 0);
        }
        int cashBefore = getCashBalance(context);
        if (requestedSpend > cashBefore) {
            return new TradeResult(TradeResult.STATUS_INSUFFICIENT_BALANCE, requestedSpend, 0, 0, cashBefore, cashBefore, unitPrice, 0);
        }
        int filledUnits = requestedSpend / unitPrice;
        if (filledUnits <= 0) {
            return new TradeResult(TradeResult.STATUS_BELOW_UNIT_PRICE, requestedSpend, 0, 0, cashBefore, cashBefore, unitPrice, 0);
        }
        int actualCost = filledUnits * unitPrice;
        int cashAfter = cashBefore - actualCost;
        SharedPreferences prefs = prefs(context);
        int oldShortUnits = prefs.getInt(KEY_SHORT_UNITS_PREFIX + forumKey, 0);
        int oldShortCost = prefs.getInt(KEY_SHORT_COST_PREFIX + forumKey, 0);
        prefs.edit()
                .putInt(KEY_CASH_BALANCE, cashAfter)
                .putInt(KEY_SHORT_UNITS_PREFIX + forumKey, oldShortUnits + filledUnits)
                .putInt(KEY_SHORT_COST_PREFIX + forumKey, oldShortCost + actualCost)
                .apply();
        int priceAfterTrade = applyTradePulse(context, forumKey, unitPrice, filledUnits, requestedSpend, false);
        return new TradeResult(TradeResult.STATUS_SUCCESS, requestedSpend, filledUnits, actualCost,
                cashBefore, cashAfter, priceAfterTrade, priceAfterTrade - unitPrice);
    }

    @NonNull
    public static TradeResult closeShort(Context context, String forumKey, int requestedValue, int unitPrice) {
        ensureDailyReset(context);
        if (requestedValue <= 0 || unitPrice <= 0) {
            return new TradeResult(TradeResult.STATUS_INVALID_AMOUNT, requestedValue, 0, 0, getCashBalance(context), getCashBalance(context), unitPrice, 0);
        }
        SharedPreferences prefs = prefs(context);
        int shortUnits = prefs.getInt(KEY_SHORT_UNITS_PREFIX + forumKey, 0);
        int shortCost = prefs.getInt(KEY_SHORT_COST_PREFIX + forumKey, 0);
        int cashBefore = getCashBalance(context);
        if (shortUnits <= 0 || shortCost <= 0) {
            return new TradeResult(TradeResult.STATUS_NO_SHORT_POSITION, requestedValue, 0, 0, cashBefore, cashBefore, unitPrice, 0);
        }
        int entryPrice = Math.round((float) shortCost / shortUnits);
        int filledUnits = requestedValue / entryPrice;
        if (filledUnits <= 0) {
            return new TradeResult(TradeResult.STATUS_BELOW_UNIT_PRICE, requestedValue, 0, 0, cashBefore, cashBefore, unitPrice, 0);
        }
        if (filledUnits > shortUnits) {
            return new TradeResult(TradeResult.STATUS_INSUFFICIENT_SHORT_POSITION, requestedValue, 0, 0, cashBefore, cashBefore, unitPrice, 0);
        }
        // Closing short: return margin + profit (margin = filledUnits × entryPrice; profit = (entry - current) × units)
        int proceeds = Math.max(0, filledUnits * (2 * entryPrice - unitPrice));
        int cashAfter = cashBefore + proceeds;
        int remainingUnits = shortUnits - filledUnits;
        int remainingCost = remainingUnits == 0 ? 0
                : Math.round((float) shortCost * remainingUnits / shortUnits);
        prefs.edit()
                .putInt(KEY_CASH_BALANCE, cashAfter)
                .putInt(KEY_SHORT_UNITS_PREFIX + forumKey, remainingUnits)
                .putInt(KEY_SHORT_COST_PREFIX + forumKey, remainingCost)
                .apply();
        int priceAfterTrade = applyTradePulse(context, forumKey, unitPrice, filledUnits, requestedValue, true);
        return new TradeResult(TradeResult.STATUS_SUCCESS, requestedValue, filledUnits, proceeds,
                cashBefore, cashAfter, priceAfterTrade, priceAfterTrade - unitPrice);
    }

    @NonNull
    public static TradeResult buy(Context context, String forumKey, int requestedSpend, int unitPrice) {
        ensureDailyReset(context);

        if (requestedSpend <= 0 || unitPrice <= 0) {
            return new TradeResult(TradeResult.STATUS_INVALID_AMOUNT, requestedSpend, 0, 0, 0, getCashBalance(context), unitPrice, 0);
        }

        int cashBefore = getCashBalance(context);
        if (requestedSpend > cashBefore) {
            return new TradeResult(TradeResult.STATUS_INSUFFICIENT_BALANCE, requestedSpend, 0, 0, cashBefore, cashBefore, unitPrice, 0);
        }

        int filledUnits = requestedSpend / unitPrice;
        if (filledUnits <= 0) {
            return new TradeResult(TradeResult.STATUS_BELOW_UNIT_PRICE, requestedSpend, 0, 0, cashBefore, cashBefore, unitPrice, 0);
        }

        int actualCost = filledUnits * unitPrice;
        int cashAfter = cashBefore - actualCost;

        SharedPreferences prefs = prefs(context);
        int oldUnits = prefs.getInt(KEY_POSITION_UNITS_PREFIX + forumKey, 0);
        int oldCost = prefs.getInt(KEY_POSITION_COST_PREFIX + forumKey, 0);

        prefs.edit()
                .putInt(KEY_CASH_BALANCE, cashAfter)
                .putInt(KEY_POSITION_UNITS_PREFIX + forumKey, oldUnits + filledUnits)
                .putInt(KEY_POSITION_COST_PREFIX + forumKey, oldCost + actualCost)
                .apply();

        int priceAfterTrade = applyTradePulse(context, forumKey, unitPrice, filledUnits, requestedSpend, true);
        return new TradeResult(
                TradeResult.STATUS_SUCCESS,
                requestedSpend,
                filledUnits,
                actualCost,
                cashBefore,
                cashAfter,
                priceAfterTrade,
                priceAfterTrade - unitPrice
        );
    }

    @NonNull
    public static TradeResult sell(Context context, String forumKey, int requestedValue, int unitPrice) {
        ensureDailyReset(context);

        if (requestedValue <= 0 || unitPrice <= 0) {
            return new TradeResult(TradeResult.STATUS_INVALID_AMOUNT, requestedValue, 0, 0, getCashBalance(context), getCashBalance(context), unitPrice, 0);
        }

        SharedPreferences prefs = prefs(context);
        int oldUnits = prefs.getInt(KEY_POSITION_UNITS_PREFIX + forumKey, 0);
        int oldCost = prefs.getInt(KEY_POSITION_COST_PREFIX + forumKey, 0);
        int cashBefore = getCashBalance(context);

        if (oldUnits <= 0 || oldCost <= 0) {
            return new TradeResult(TradeResult.STATUS_NO_POSITION, requestedValue, 0, 0, cashBefore, cashBefore, unitPrice, 0);
        }

        int filledUnits = requestedValue / unitPrice;
        if (filledUnits <= 0) {
            return new TradeResult(TradeResult.STATUS_BELOW_UNIT_PRICE, requestedValue, 0, 0, cashBefore, cashBefore, unitPrice, 0);
        }

        if (filledUnits > oldUnits || requestedValue > oldUnits * unitPrice) {
            return new TradeResult(TradeResult.STATUS_INSUFFICIENT_POSITION, requestedValue, 0, 0, cashBefore, cashBefore, unitPrice, 0);
        }

        int actualProceeds = filledUnits * unitPrice;
        int releasedCost = filledUnits == oldUnits
                ? oldCost
                : Math.max(0, Math.min(oldCost, Math.round((float) oldCost * filledUnits / oldUnits)));
        int remainingUnits = oldUnits - filledUnits;
        int remainingCost = remainingUnits == 0 ? 0 : Math.max(0, oldCost - releasedCost);
        int cashAfter = cashBefore + actualProceeds;

        prefs.edit()
                .putInt(KEY_CASH_BALANCE, cashAfter)
                .putInt(KEY_POSITION_UNITS_PREFIX + forumKey, remainingUnits)
                .putInt(KEY_POSITION_COST_PREFIX + forumKey, remainingCost)
                .apply();

        int priceAfterTrade = applyTradePulse(context, forumKey, unitPrice, filledUnits, actualProceeds, false);
        return new TradeResult(
                TradeResult.STATUS_SUCCESS,
                requestedValue,
                filledUnits,
                actualProceeds,
                cashBefore,
                cashAfter,
                priceAfterTrade,
                priceAfterTrade - unitPrice
        );
    }

    // ── Unit-based convenience wrappers ──────────────────────────────────────

    /** Buy `units` of a long position at `unitPrice` each. */
    public static TradeResult buyUnits(Context context, String forumKey, int units, int unitPrice) {
        if (units <= 0) {
            return new TradeResult(TradeResult.STATUS_INVALID_AMOUNT, 0, 0, 0, getCashBalance(context), getCashBalance(context), unitPrice, 0);
        }
        return buy(context, forumKey, units * unitPrice, unitPrice);
    }

    /** Close `units` of a long position at `unitPrice` each. */
    public static TradeResult sellUnits(Context context, String forumKey, int units, int unitPrice) {
        if (units <= 0) {
            return new TradeResult(TradeResult.STATUS_INVALID_AMOUNT, 0, 0, 0, getCashBalance(context), getCashBalance(context), unitPrice, 0);
        }
        return sell(context, forumKey, units * unitPrice, unitPrice);
    }

    /** Open a short on `units` at `unitPrice` each (margin = units × unitPrice). */
    public static TradeResult openShortUnits(Context context, String forumKey, int units, int unitPrice) {
        if (units <= 0) {
            return new TradeResult(TradeResult.STATUS_INVALID_AMOUNT, 0, 0, 0, getCashBalance(context), getCashBalance(context), unitPrice, 0);
        }
        return openShort(context, forumKey, units * unitPrice, unitPrice);
    }

    /** Close `units` of an existing short position. Entry price is derived from stored cost/units. */
    public static TradeResult closeShortUnits(Context context, String forumKey, int units, int unitPrice) {
        if (units <= 0) {
            return new TradeResult(TradeResult.STATUS_INVALID_AMOUNT, 0, 0, 0, getCashBalance(context), getCashBalance(context), unitPrice, 0);
        }
        SharedPreferences prefs = prefs(context);
        int shortUnits = prefs.getInt(KEY_SHORT_UNITS_PREFIX + forumKey, 0);
        int shortCost = prefs.getInt(KEY_SHORT_COST_PREFIX + forumKey, 0);
        int entryPrice = (shortUnits > 0) ? Math.round((float) shortCost / shortUnits) : unitPrice;
        if (entryPrice <= 0) entryPrice = unitPrice;
        return closeShort(context, forumKey, units * entryPrice, unitPrice);
    }

    // ─────────────────────────────────────────────────────────────────────────

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static int applyTradePulse(
            Context context,
            String forumKey,
            int anchorPrice,
            int filledUnits,
            int requestedValue,
            boolean isBuy
    ) {
        SharedPreferences prefs = prefs(context);
        int tradeCount = prefs.getInt(KEY_TRADE_COUNT_PREFIX + forumKey, 0);
        int oldOffset = prefs.getInt(KEY_PRICE_OFFSET_PREFIX + forumKey, 0);

        int direction = isBuy
                ? (tradeCount % 4 == 2 ? -1 : 1)
                : (tradeCount % 4 == 2 ? 1 : -1);
        int impulse = Math.max(
                2,
                Math.min(
                        18,
                        Math.round(anchorPrice * 0.018f)
                                + Math.round(filledUnits * 0.55f)
                                + Math.round(requestedValue / 420f)
                )
        );
        int maxOffset = Math.max(26, Math.round(anchorPrice * 0.22f));
        int newOffset = clamp(oldOffset + (direction * impulse), -maxOffset, maxOffset);

        prefs.edit()
                .putInt(KEY_PRICE_OFFSET_PREFIX + forumKey, newOffset)
                .putInt(KEY_TRADE_COUNT_PREFIX + forumKey, tradeCount + 1)
                .apply();

        return Math.max(1, anchorPrice + (newOffset - oldOffset));
    }

    private static int computeLiveDelta(String forumKey, int tickCount, int currentOffset, int basePrice, int maxOffset) {
        int[] wave = waveFor(forumKey);
        int raw = wave[tickCount % wave.length];
        int delta = raw > 0 ? (raw == 2 ? 2 : 1) : (raw == -2 ? -2 : -1);

        if (currentOffset > (int) (maxOffset * 0.62f) && delta > 0) {
            delta = -Math.max(1, Math.abs(delta));
        } else if (currentOffset < (int) (-maxOffset * 0.62f) && delta < 0) {
            delta = Math.max(1, Math.abs(delta));
        } else if (Math.abs(currentOffset) > (int) (maxOffset * 0.85f)) {
            delta = currentOffset > 0 ? -2 : 2;
        }

        int scaled = Math.max(1, Math.round(basePrice * 0.0045f));
        if (Math.abs(delta) == 2) {
            scaled += 1;
        }
        return delta > 0 ? scaled : -scaled;
    }

    private static int[] waveFor(String forumKey) {
        if (AppData.FORUM_UNSW.equals(forumKey)) {
            return new int[]{2, 1, -2, 2, -1, -2, 1, -1};
        }
        if (AppData.FORUM_USYD.equals(forumKey)) {
            return new int[]{1, 2, 1, -1, 2, -1, -2, 1};
        }
        if (AppData.FORUM_UM.equals(forumKey)) {
            return new int[]{-1, 2, 1, -2, 1, 2, -1, -1};
        }
        return new int[]{1, 1, 2, -1, 1, -2, 1, -1};
    }

    private static int clamp(int value, int min, int max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }

    private static String todayStamp() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    public static final class PortfolioSnapshot {
        private final int cashBalance;
        private final int marketValue;
        private final int totalAssets;
        private final int openPnl;
        private final ArrayList<PositionSnapshot> positions;
        private final ArrayList<ShortPositionSnapshot> shortPositions;

        private PortfolioSnapshot(
                int cashBalance,
                int marketValue,
                int totalAssets,
                int openPnl,
                ArrayList<PositionSnapshot> positions,
                ArrayList<ShortPositionSnapshot> shortPositions
        ) {
            this.cashBalance = cashBalance;
            this.marketValue = marketValue;
            this.totalAssets = totalAssets;
            this.openPnl = openPnl;
            this.positions = positions;
            this.shortPositions = shortPositions;
        }

        public int getCashBalance() { return cashBalance; }
        public int getMarketValue() { return marketValue; }
        public int getTotalAssets() { return totalAssets; }
        public int getOpenPnl() { return openPnl; }

        @NonNull
        public ArrayList<PositionSnapshot> getPositions() {
            return new ArrayList<>(positions);
        }

        @NonNull
        public ArrayList<ShortPositionSnapshot> getShortPositions() {
            return new ArrayList<>(shortPositions);
        }
    }

    public static final class ShortPositionSnapshot {
        private final String forumKey;
        private final int units;
        private final int entryPrice;
        private final int currentPrice;
        private final int openPnl;

        private ShortPositionSnapshot(String forumKey, int units, int entryPrice, int currentPrice, int openPnl) {
            this.forumKey = forumKey;
            this.units = units;
            this.entryPrice = entryPrice;
            this.currentPrice = currentPrice;
            this.openPnl = openPnl;
        }

        public String getForumKey() { return forumKey; }
        public int getUnits() { return units; }
        public int getEntryPrice() { return entryPrice; }
        public int getCurrentPrice() { return currentPrice; }
        public int getOpenPnl() { return openPnl; }
        public int getCurrentValue() { return units * entryPrice; }
    }

    public static final class PositionSnapshot {
        private final String forumKey;
        private final int units;
        private final int totalCost;
        private final int currentPrice;
        private final int currentValue;

        private PositionSnapshot(String forumKey, int units, int totalCost, int currentPrice, int currentValue) {
            this.forumKey = forumKey;
            this.units = units;
            this.totalCost = totalCost;
            this.currentPrice = currentPrice;
            this.currentValue = currentValue;
        }

        public String getForumKey() {
            return forumKey;
        }

        public int getUnits() {
            return units;
        }

        public int getTotalCost() {
            return totalCost;
        }

        public int getCurrentPrice() {
            return currentPrice;
        }

        public int getCurrentValue() {
            return currentValue;
        }

        public int getAverageCost() {
            return units == 0 ? 0 : Math.round((float) totalCost / (float) units);
        }

        public int getOpenPnl() {
            return currentValue - totalCost;
        }
    }

    public static final class TradeResult {
        public static final int STATUS_SUCCESS = 0;
        public static final int STATUS_INVALID_AMOUNT = 1;
        public static final int STATUS_INSUFFICIENT_BALANCE = 2;
        public static final int STATUS_BELOW_UNIT_PRICE = 3;
        public static final int STATUS_NO_POSITION = 4;
        public static final int STATUS_INSUFFICIENT_POSITION = 5;
        public static final int STATUS_NO_SHORT_POSITION = 6;
        public static final int STATUS_INSUFFICIENT_SHORT_POSITION = 7;

        private final int status;
        private final int requestedSpend;
        private final int filledUnits;
        private final int actualCost;
        private final int cashBefore;
        private final int cashAfter;
        private final int priceAfterTrade;
        private final int priceDelta;

        private TradeResult(
                int status,
                int requestedSpend,
                int filledUnits,
                int actualCost,
                int cashBefore,
                int cashAfter,
                int priceAfterTrade,
                int priceDelta
        ) {
            this.status = status;
            this.requestedSpend = requestedSpend;
            this.filledUnits = filledUnits;
            this.actualCost = actualCost;
            this.cashBefore = cashBefore;
            this.cashAfter = cashAfter;
            this.priceAfterTrade = priceAfterTrade;
            this.priceDelta = priceDelta;
        }

        public int getStatus() {
            return status;
        }

        public int getRequestedSpend() {
            return requestedSpend;
        }

        public int getFilledUnits() {
            return filledUnits;
        }

        public int getActualCost() {
            return actualCost;
        }

        public int getCashBefore() {
            return cashBefore;
        }

        public int getCashAfter() {
            return cashAfter;
        }

        public int getPriceAfterTrade() {
            return priceAfterTrade;
        }

        public int getPriceDelta() {
            return priceDelta;
        }

        public int getUnfilledCash() {
            return requestedSpend - actualCost;
        }
    }
}

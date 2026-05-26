package com.example.myapplication;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class MarketFragment extends Fragment implements RefreshablePage {
    private static final String STATE_SELECTED_MARKET = "selected_market";
    private static final String STATE_LAST_TRADE_MESSAGE = "last_trade_message";
    private static final String STATE_LAST_TRADE_STATUS = "last_trade_status";

    private TextView textMarketBalanceValue;
    private TextView textMarketHeldValue;
    private TextView textMarketOpenPnlValue;
    private TextView textMarketResetRule;
    private LinearLayout layoutMarketSelectors;
    private LinearLayout layoutMarketHero;
    private ImageView imageMarketAvatar;
    private TextView textMarketDemoBadge;
    private TextView textMarketForumName;
    private TextView textMarketIndex;
    private TextView textMarketChange;
    private TextView textMarketFormulaLabel;
    private TextView textMarketFormulaValue;
    private TextView textMarketTriggerLabel;
    private MarketCandleChartView viewMarketChart;
    private LinearLayout layoutMarketTriggers;
    private LinearLayout layoutChartPeriod;
    private LinearLayout layoutTradeMode;
    private String selectedPeriod = "day";
    private String tradeMode = "long"; // "long" or "short"
    private TextView textTradePrice;
    private EditText inputTradeAmount;
    private TextView textTradeEstimate;
    private Button buttonTradeBuy;
    private Button buttonTradeSell;
    private TextView textTradeResult;
    private LinearLayout layoutMarketPositions;
    private TextView textPositionEmpty;
    private TextView textTradeExplain;
    private String selectedMarketKey;
    private String lastTradeMessage;
    private int lastTradeStatus = -1;
    private final Handler marketTickerHandler = new Handler(Looper.getMainLooper());
    private final Runnable marketTicker = new Runnable() {
        @Override
        public void run() {
            if (!isAdded()) {
                return;
            }
            MarketPortfolioStore.advanceMarketTick(requireContext());
            refreshContent();
            marketTickerHandler.postDelayed(this, MarketPortfolioStore.LIVE_TICK_INTERVAL_MS);
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_market, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        textMarketBalanceValue = view.findViewById(R.id.textMarketBalanceValue);
        textMarketHeldValue = view.findViewById(R.id.textMarketHeldValue);
        textMarketOpenPnlValue = view.findViewById(R.id.textMarketOpenPnlValue);
        textMarketResetRule = view.findViewById(R.id.textMarketResetRule);
        layoutMarketSelectors = view.findViewById(R.id.layoutMarketSelectors);
        layoutMarketHero = view.findViewById(R.id.layoutMarketHero);
        imageMarketAvatar = view.findViewById(R.id.imageMarketAvatar);
        textMarketDemoBadge = view.findViewById(R.id.textMarketDemoBadge);
        textMarketForumName = view.findViewById(R.id.textMarketForumName);
        textMarketIndex = view.findViewById(R.id.textMarketIndex);
        textMarketChange = view.findViewById(R.id.textMarketChange);
        textMarketFormulaLabel = view.findViewById(R.id.textMarketFormulaLabel);
        textMarketFormulaValue = view.findViewById(R.id.textMarketFormulaValue);
        textMarketTriggerLabel = view.findViewById(R.id.textMarketTriggerLabel);
        viewMarketChart = view.findViewById(R.id.viewMarketChart);
        layoutMarketTriggers = view.findViewById(R.id.layoutMarketTriggers);
        layoutChartPeriod = view.findViewById(R.id.layoutChartPeriod);
        layoutTradeMode = view.findViewById(R.id.layoutTradeMode);
        textTradePrice = view.findViewById(R.id.textTradePrice);
        inputTradeAmount = view.findViewById(R.id.inputTradeAmount);
        textTradeEstimate = view.findViewById(R.id.textTradeEstimate);
        buttonTradeBuy = view.findViewById(R.id.buttonTradeBuy);
        buttonTradeSell = view.findViewById(R.id.buttonTradeSell);
        textTradeResult = view.findViewById(R.id.textTradeResult);
        layoutMarketPositions = view.findViewById(R.id.layoutMarketPositions);
        textPositionEmpty = view.findViewById(R.id.textPositionEmpty);
        textTradeExplain = view.findViewById(R.id.textTradeExplain);

        if (savedInstanceState != null) {
            selectedMarketKey = savedInstanceState.getString(STATE_SELECTED_MARKET);
            lastTradeMessage = savedInstanceState.getString(STATE_LAST_TRADE_MESSAGE);
            lastTradeStatus = savedInstanceState.getInt(STATE_LAST_TRADE_STATUS, -1);
        }
        if (selectedMarketKey == null) {
            selectedMarketKey = AppData.getSelectedForumKey();
        }

        inputTradeAmount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateTradeEstimate();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        buttonTradeBuy.setOnClickListener(v -> handleBuy());
        buttonTradeSell.setOnClickListener(v -> handleSell());

        refreshContent();
    }

    @Override
    public void onResume() {
        super.onResume();
        MarketPortfolioStore.advanceMarketTick(requireContext());
        refreshContent();
        startTicker();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopTicker();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_SELECTED_MARKET, selectedMarketKey);
        outState.putString(STATE_LAST_TRADE_MESSAGE, lastTradeMessage);
        outState.putInt(STATE_LAST_TRADE_STATUS, lastTradeStatus);
    }

    @Override
    public void refreshContent() {
        if (!isAdded() || getView() == null) {
            return;
        }

        if (selectedMarketKey == null) {
            selectedMarketKey = AppData.getSelectedForumKey();
        }

        MarketPortfolioStore.PortfolioSnapshot snapshot = MarketPortfolioStore.getPortfolio(requireContext());
        textMarketBalanceValue.setText(formatTokens(snapshot.getCashBalance()));
        textMarketHeldValue.setText(formatTokens(snapshot.getMarketValue()));
        textMarketOpenPnlValue.setText(formatSignedTokens(snapshot.getOpenPnl()));
        textMarketOpenPnlValue.setTextColor(resolvePnlColor(snapshot.getOpenPnl()));
        textMarketResetRule.setText(getString(
                R.string.market_reset_rule,
                MarketPortfolioStore.DAILY_FLOOR_TOKENS,
                MarketPortfolioStore.DAILY_FLOOR_TOKENS
        ));

        renderSelectorRow();
        renderSelectedMarket();
        renderPositions(snapshot);
        renderTradeResult();
        updateTradeEstimate();
    }

    private void renderSelectorRow() {
        if (!isAdded() || layoutMarketSelectors == null) {
            return;
        }

        layoutMarketSelectors.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        List<CampusMarketRepository.SchoolMarket> markets = CampusMarketRepository.getMarkets();
        for (CampusMarketRepository.SchoolMarket market : markets) {
            View chip = inflater.inflate(R.layout.item_market_selector, layoutMarketSelectors, false);
            LinearLayout root = chip.findViewById(R.id.layoutSelectorRoot);
            ImageView avatar = chip.findViewById(R.id.imageSelectorAvatar);
            TextView label = chip.findViewById(R.id.textSelectorLabel);
            TextView price = chip.findViewById(R.id.textSelectorPrice);

            String forumKey = market.getForumKey();
            boolean selected = forumKey.equals(selectedMarketKey);
            int fillColor = ContextCompat.getColor(requireContext(),
                    selected ? AppData.getForumHeaderColorResId(forumKey) : R.color.surface);
            int primaryColor = ContextCompat.getColor(requireContext(),
                    selected ? R.color.forum_header_on : R.color.ink_primary);
            int secondaryColor = ContextCompat.getColor(requireContext(),
                    selected ? R.color.forum_header_on_secondary : R.color.ink_secondary);

            root.setBackgroundTintList(android.content.res.ColorStateList.valueOf(fillColor));
            root.setAlpha(selected ? 1.0f : 0.88f);
            avatar.setImageResource(AppData.getForumAvatarResId(forumKey));
            avatar.setImageTintList(null);
            if (AppData.FORUM_UM.equals(forumKey)) {
                avatar.setBackgroundResource(R.drawable.bg_community_avatar_um);
            }
            label.setText(AppData.getForumLabel(requireContext(), forumKey));
            label.setTextColor(primaryColor);
            label.setTypeface(selected ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
            price.setText(getString(R.string.market_selector_price, getLivePrice(market)));
            price.setTextColor(secondaryColor);
            root.setOnClickListener(v -> {
                selectedMarketKey = forumKey;
                refreshContent();
            });
            layoutMarketSelectors.addView(chip);
        }
    }

    private void renderSelectedMarket() {
        if (!isAdded() || layoutMarketHero == null) {
            return;
        }

        CampusMarketRepository.SchoolMarket market = CampusMarketRepository.getMarket(selectedMarketKey);
        int headerColor = ContextCompat.getColor(requireContext(), AppData.getForumHeaderColorResId(selectedMarketKey));
        int onColor = ContextCompat.getColor(requireContext(), R.color.forum_header_on);
        int onSecondary = ContextCompat.getColor(requireContext(), R.color.forum_header_on_secondary);

        GradientDrawable hero = new GradientDrawable();
        hero.setShape(GradientDrawable.RECTANGLE);
        hero.setCornerRadius(dp(28));
        hero.setColor(headerColor);
        layoutMarketHero.setBackground(hero);

        textMarketDemoBadge.setBackground(makePill(Color.argb(42, 255, 255, 255), 999));
        textMarketDemoBadge.setTextColor(onColor);

        imageMarketAvatar.setImageResource(AppData.getForumAvatarResId(selectedMarketKey));
        imageMarketAvatar.setImageTintList(null);
        if (AppData.FORUM_UM.equals(selectedMarketKey)) {
            imageMarketAvatar.setBackgroundResource(R.drawable.bg_community_avatar_um);
        } else {
            imageMarketAvatar.setBackgroundResource(R.drawable.bg_community_avatar);
        }
        textMarketForumName.setText(AppData.getForumLabel(requireContext(), selectedMarketKey));
        textMarketForumName.setTextColor(onColor);
        int livePrice = getLivePrice(market);
        int heatIndex = market.getHeatIndex();
        textMarketIndex.setText(getString(R.string.market_index_format, heatIndex));
        textMarketIndex.setTextColor(onColor);
        textMarketChange.setText(getString(R.string.market_change_today, formatPercent(getLiveDayChangePercent(market, livePrice))));
        textMarketChange.setTextColor(onColor);
        textMarketChange.setBackground(makePill(Color.argb(40, 255, 255, 255), 999));
        textMarketFormulaLabel.setTextColor(onSecondary);
        textMarketFormulaValue.setTextColor(onSecondary);
        textMarketFormulaValue.setText(getString(
                R.string.market_formula_value,
                market.getPostsToday(),
                market.getRepliesToday(),
                market.getLikesToday()
        ));
        textMarketTriggerLabel.setTextColor(onSecondary);

        setupPeriodChips(onColor, onSecondary);
        List<CampusMarketRepository.MarketCandle> periodCandles = buildCandlesForPeriod(market, livePrice);
        viewMarketChart.setData(periodCandles, buildLabelsForPeriod(periodCandles.size()));
        renderTriggers(market.getTriggers(requireContext()), onColor);

        setupTradeModeChips();
        textTradePrice.setText(getString(R.string.market_trade_price_format, livePrice));
    }

    private void renderTriggers(List<String> triggers, int textColor) {
        layoutMarketTriggers.removeAllViews();
        for (String trigger : triggers) {
            TextView chip = new TextView(requireContext());
            chip.setText(trigger);
            chip.setTextColor(textColor);
            chip.setTextSize(13f);
            chip.setBackground(makePill(Color.argb(34, 255, 255, 255), 18));
            chip.setPadding(dp(12), dp(10), dp(12), dp(10));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.topMargin = layoutMarketTriggers.getChildCount() == 0 ? 0 : dp(8);
            layoutMarketTriggers.addView(chip, params);
        }
    }

    private void updateTradeEstimate() {
        if (!isAdded() || inputTradeAmount == null) {
            return;
        }

        CampusMarketRepository.SchoolMarket market = CampusMarketRepository.getMarket(selectedMarketKey);
        MarketPortfolioStore.PortfolioSnapshot snapshot = MarketPortfolioStore.getPortfolio(requireContext());
        int currentCash = snapshot.getCashBalance();
        int units = parsePositiveInt(inputTradeAmount.getText().toString());

        if (units <= 0) {
            textTradeEstimate.setText(R.string.market_estimate_placeholder);
            setTradeButtonStates(false, false);
            return;
        }

        int unitPrice = getLivePrice(market);
        int totalCost = units * unitPrice;

        if ("short".equals(tradeMode)) {
            // 做空: Open = open short, Close = close short
            MarketPortfolioStore.ShortPositionSnapshot shortPos =
                    findShortPosition(snapshot.getShortPositions(), selectedMarketKey);
            boolean canOpenShort = totalCost <= currentCash;
            boolean canCloseShort = shortPos != null && units <= shortPos.getUnits();

            if (canOpenShort || canCloseShort) {
                if (canOpenShort) {
                    textTradeEstimate.setText(getString(
                            R.string.market_estimate_open, totalCost, units, unitPrice));
                } else {
                    // close short estimate
                    int pnl = (shortPos.getEntryPrice() - unitPrice) * units;
                    int receive = units * shortPos.getEntryPrice(); // margin returned
                    textTradeEstimate.setText(getString(
                            R.string.market_estimate_close, receive, formatSignedTokens(pnl)));
                }
            } else {
                textTradeEstimate.setText(R.string.market_transaction_insufficient_balance);
            }
            setTradeButtonStates(canOpenShort, canCloseShort);
            return;
        }

        // 做多: Open = buy, Close = sell
        MarketPortfolioStore.PositionSnapshot longPos =
                findPosition(snapshot.getPositions(), selectedMarketKey);
        boolean canBuy = totalCost <= currentCash;
        boolean canSell = longPos != null && units <= longPos.getUnits();

        if (canBuy || canSell) {
            if (canBuy) {
                textTradeEstimate.setText(getString(
                        R.string.market_estimate_open, totalCost, units, unitPrice));
            } else {
                // close long estimate
                int pnl = (unitPrice - longPos.getAverageCost()) * units;
                int receive = units * unitPrice;
                textTradeEstimate.setText(getString(
                        R.string.market_estimate_close, receive, formatSignedTokens(pnl)));
            }
        } else {
            textTradeEstimate.setText(R.string.market_transaction_insufficient_balance);
        }

        setTradeButtonStates(canBuy, canSell);
    }

    private void handleBuy() {
        // Buy / Open: in 做多 mode → open long; in 做空 mode → open short
        CampusMarketRepository.SchoolMarket market = CampusMarketRepository.getMarket(selectedMarketKey);
        int units = parsePositiveInt(inputTradeAmount.getText().toString());
        int unitPrice = getLivePrice(market);

        MarketPortfolioStore.TradeResult result;
        if ("short".equals(tradeMode)) {
            result = MarketPortfolioStore.openShortUnits(requireContext(), selectedMarketKey, units, unitPrice);
        } else {
            result = MarketPortfolioStore.buyUnits(requireContext(), selectedMarketKey, units, unitPrice);
        }

        if (result.getStatus() == MarketPortfolioStore.TradeResult.STATUS_SUCCESS) {
            lastTradeMessage = getString(
                    R.string.market_transaction_success,
                    result.getFilledUnits(),
                    AppData.getForumLabel(requireContext(), selectedMarketKey),
                    result.getCashBefore(),
                    result.getCashAfter(),
                    result.getPriceAfterTrade()
            );
            lastTradeStatus = result.getStatus();
            inputTradeAmount.setText("");
            Toast.makeText(requireContext(), lastTradeMessage, Toast.LENGTH_SHORT).show();
            refreshContent();
            host().refreshAllPages();
            return;
        }

        if (result.getStatus() == MarketPortfolioStore.TradeResult.STATUS_INSUFFICIENT_BALANCE) {
            lastTradeMessage = getString(R.string.market_transaction_insufficient_balance);
        } else if (result.getStatus() == MarketPortfolioStore.TradeResult.STATUS_INVALID_AMOUNT) {
            lastTradeMessage = getString(R.string.market_transaction_invalid_amount);
        } else {
            lastTradeMessage = getString(R.string.market_transaction_invalid_amount);
        }
        lastTradeStatus = result.getStatus();
        renderTradeResult();
    }

    private void handleSell() {
        // Sell / Close: in 做多 mode → close long; in 做空 mode → close short
        CampusMarketRepository.SchoolMarket market = CampusMarketRepository.getMarket(selectedMarketKey);
        int units = parsePositiveInt(inputTradeAmount.getText().toString());
        int unitPrice = getLivePrice(market);

        MarketPortfolioStore.TradeResult result;
        if ("short".equals(tradeMode)) {
            result = MarketPortfolioStore.closeShortUnits(requireContext(), selectedMarketKey, units, unitPrice);
        } else {
            result = MarketPortfolioStore.sellUnits(requireContext(), selectedMarketKey, units, unitPrice);
        }

        if (result.getStatus() == MarketPortfolioStore.TradeResult.STATUS_SUCCESS) {
            lastTradeMessage = getString(
                    R.string.market_sell_success,
                    result.getFilledUnits(),
                    AppData.getForumLabel(requireContext(), selectedMarketKey),
                    result.getCashBefore(),
                    result.getCashAfter(),
                    result.getPriceAfterTrade()
            );
            lastTradeStatus = result.getStatus();
            inputTradeAmount.setText("");
            Toast.makeText(requireContext(), lastTradeMessage, Toast.LENGTH_SHORT).show();
            refreshContent();
            host().refreshAllPages();
            return;
        }

        if (result.getStatus() == MarketPortfolioStore.TradeResult.STATUS_NO_POSITION) {
            lastTradeMessage = getString(R.string.market_transaction_no_position);
        } else if (result.getStatus() == MarketPortfolioStore.TradeResult.STATUS_INSUFFICIENT_POSITION) {
            lastTradeMessage = getString(R.string.market_transaction_insufficient_position);
        } else if (result.getStatus() == MarketPortfolioStore.TradeResult.STATUS_NO_SHORT_POSITION) {
            lastTradeMessage = getString(R.string.market_transaction_no_short_position);
        } else if (result.getStatus() == MarketPortfolioStore.TradeResult.STATUS_INSUFFICIENT_SHORT_POSITION) {
            lastTradeMessage = getString(R.string.market_transaction_insufficient_short_position);
        } else {
            lastTradeMessage = getString(R.string.market_transaction_invalid_amount);
        }
        lastTradeStatus = result.getStatus();
        renderTradeResult();
    }

    private void renderTradeResult() {
        if (textTradeResult == null) {
            return;
        }
        if (lastTradeMessage == null || lastTradeMessage.trim().isEmpty()) {
            textTradeResult.setVisibility(View.GONE);
            return;
        }

        textTradeResult.setVisibility(View.VISIBLE);
        textTradeResult.setText(lastTradeMessage);
        int colorRes = lastTradeStatus == MarketPortfolioStore.TradeResult.STATUS_SUCCESS
                ? R.color.market_gain
                : R.color.market_loss;
        textTradeResult.setTextColor(ContextCompat.getColor(requireContext(), colorRes));
    }

    private void renderPositions(MarketPortfolioStore.PortfolioSnapshot snapshot) {
        List<MarketPortfolioStore.PositionSnapshot> positions = snapshot.getPositions();
        List<MarketPortfolioStore.ShortPositionSnapshot> shorts = snapshot.getShortPositions();
        layoutMarketPositions.removeAllViews();
        boolean noPositions = positions.isEmpty() && shorts.isEmpty();
        textPositionEmpty.setVisibility(noPositions ? View.VISIBLE : View.GONE);

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (MarketPortfolioStore.PositionSnapshot position : positions) {
            View item = inflater.inflate(R.layout.item_market_position, layoutMarketPositions, false);
            ImageView avatar = item.findViewById(R.id.imagePositionAvatar);
            TextView forum = item.findViewById(R.id.textPositionForum);
            TextView meta = item.findViewById(R.id.textPositionMeta);
            TextView value = item.findViewById(R.id.textPositionValue);
            TextView units = item.findViewById(R.id.textPositionUnits);
            TextView pnl = item.findViewById(R.id.textPositionPnl);

            avatar.setImageResource(AppData.getForumAvatarResId(position.getForumKey()));
            avatar.setImageTintList(null);
            forum.setText(getString(R.string.market_position_type_long) + " · "
                    + AppData.getForumLabel(requireContext(), position.getForumKey()));
            meta.setText(getString(R.string.market_position_meta, position.getUnits(), position.getAverageCost()));
            value.setText(getString(R.string.market_position_value, position.getCurrentValue(), position.getCurrentPrice()));
            units.setText(getString(R.string.market_position_units, position.getUnits()));
            pnl.setText(getString(R.string.market_position_pnl, formatSignedTokens(position.getOpenPnl())));
            pnl.setTextColor(resolvePnlColor(position.getOpenPnl()));
            layoutMarketPositions.addView(item);
        }

        for (MarketPortfolioStore.ShortPositionSnapshot sp : shorts) {
            View item = inflater.inflate(R.layout.item_market_position, layoutMarketPositions, false);
            ImageView avatar = item.findViewById(R.id.imagePositionAvatar);
            TextView forum = item.findViewById(R.id.textPositionForum);
            TextView meta = item.findViewById(R.id.textPositionMeta);
            TextView value = item.findViewById(R.id.textPositionValue);
            TextView units = item.findViewById(R.id.textPositionUnits);
            TextView pnl = item.findViewById(R.id.textPositionPnl);

            avatar.setImageResource(AppData.getForumAvatarResId(sp.getForumKey()));
            avatar.setImageTintList(null);
            forum.setText(getString(R.string.market_position_type_short) + " · "
                    + AppData.getForumLabel(requireContext(), sp.getForumKey()));
            meta.setText(getString(R.string.market_position_short_meta, sp.getUnits(), sp.getEntryPrice()));
            value.setText(getString(R.string.market_position_value, sp.getCurrentValue(), sp.getCurrentPrice()));
            units.setText(getString(R.string.market_position_units, sp.getUnits()));
            pnl.setText(getString(R.string.market_position_pnl, formatSignedTokens(sp.getOpenPnl())));
            pnl.setTextColor(resolvePnlColor(sp.getOpenPnl()));
            layoutMarketPositions.addView(item);
        }
    }


    private void setupTradeModeChips() {
        if (layoutTradeMode == null) return;
        layoutTradeMode.removeAllViews();
        String[] modes = {"long", "short"};
        int[] labelRes = {R.string.market_trade_long, R.string.market_trade_short};
        int[] colors = {
                ContextCompat.getColor(requireContext(), R.color.accent_strong),
                ContextCompat.getColor(requireContext(), R.color.market_loss)
        };
        for (int i = 0; i < modes.length; i++) {
            final String mode = modes[i];
            boolean sel = mode.equals(tradeMode);
            TextView chip = new TextView(requireContext());
            chip.setText(labelRes[i]);
            chip.setTextSize(14f);
            chip.setTypeface(Typeface.DEFAULT_BOLD);
            chip.setTextColor(sel ? ContextCompat.getColor(requireContext(), R.color.white) : colors[i]);
            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.RECTANGLE);
            bg.setCornerRadius(dp(999));
            bg.setColor(sel ? colors[i] : Color.TRANSPARENT);
            bg.setStroke(dp(1), colors[i]);
            chip.setBackground(bg);
            chip.setPadding(dp(20), dp(8), dp(20), dp(8));
            chip.setOnClickListener(v -> {
                tradeMode = mode;
                setupTradeModeChips();
                updateTradeModeButtons();
                updateTradeEstimate();
            });
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            p.leftMargin = i == 0 ? 0 : dp(10);
            layoutTradeMode.addView(chip, p);
        }
        updateTradeModeButtons();
    }

    private void updateTradeModeButtons() {
        if (buttonTradeBuy == null || buttonTradeSell == null) return;
        // Labels are always 买入开仓 / 卖出平仓; just refresh estimate on mode change
        updateTradeEstimate();
    }

    private void setTradeButtonStates(boolean buyEnabled, boolean sellEnabled) {
        buttonTradeBuy.setEnabled(buyEnabled);
        buttonTradeBuy.setAlpha(buyEnabled ? 1.0f : 0.62f);
        buttonTradeSell.setEnabled(sellEnabled);
        buttonTradeSell.setAlpha(sellEnabled ? 1.0f : 0.62f);
    }

    private void startTicker() {
        marketTickerHandler.removeCallbacks(marketTicker);
        marketTickerHandler.postDelayed(marketTicker, MarketPortfolioStore.LIVE_TICK_INTERVAL_MS);
    }

    private void stopTicker() {
        marketTickerHandler.removeCallbacks(marketTicker);
    }

    private GradientDrawable makePill(int color, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setCornerRadius(dp(radiusDp));
        drawable.setColor(color);
        return drawable;
    }

    private int resolvePnlColor(int value) {
        if (value > 0) {
            return ContextCompat.getColor(requireContext(), R.color.market_gain);
        }
        if (value < 0) {
            return ContextCompat.getColor(requireContext(), R.color.market_loss);
        }
        return ContextCompat.getColor(requireContext(), R.color.ink_primary);
    }

    private String formatTokens(int value) {
        return getString(R.string.market_tokens_format, value);
    }

    private String formatSignedTokens(int value) {
        return String.format(Locale.getDefault(), "%+d", value);
    }

    private String formatPercent(double value) {
        return String.format(Locale.getDefault(), "%+.1f%%", value);
    }

    private int getLivePrice(CampusMarketRepository.SchoolMarket market) {
        return MarketPortfolioStore.getLivePrice(requireContext(), market.getForumKey(), market.getCurrentPrice());
    }

    @Nullable
    private MarketPortfolioStore.PositionSnapshot findPosition(
            List<MarketPortfolioStore.PositionSnapshot> positions,
            String forumKey
    ) {
        for (MarketPortfolioStore.PositionSnapshot position : positions) {
            if (position.getForumKey().equals(forumKey)) {
                return position;
            }
        }
        return null;
    }

    private double getLiveDayChangePercent(CampusMarketRepository.SchoolMarket market, int livePrice) {
        List<CampusMarketRepository.MarketCandle> candles = market.getCandles();
        if (candles.size() < 2) {
            return 0d;
        }

        int previousClose = candles.get(candles.size() - 2).close;
        if (previousClose == 0) {
            return 0d;
        }
        return ((double) (livePrice - previousClose) * 100d) / previousClose;
    }

    private List<CampusMarketRepository.MarketCandle> buildDisplayCandles(
            CampusMarketRepository.SchoolMarket market,
            int livePrice
    ) {
        ArrayList<CampusMarketRepository.MarketCandle> displayCandles = new ArrayList<>(market.getCandles());
        if (displayCandles.isEmpty()) {
            return displayCandles;
        }

        int lastIndex = displayCandles.size() - 1;
        CampusMarketRepository.MarketCandle last = displayCandles.get(lastIndex);
        displayCandles.set(lastIndex, new CampusMarketRepository.MarketCandle(
                last.open,
                Math.max(last.high, livePrice),
                Math.min(last.low, livePrice),
                livePrice
        ));
        return displayCandles;
    }

    private int parsePositiveInt(String raw) {
        if (raw == null) {
            return 0;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(trimmed);
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private void setupPeriodChips(int onColor, int onSecondary) {
        if (layoutChartPeriod == null) return;
        layoutChartPeriod.removeAllViews();
        String[] periods = {"intraday", "day", "week", "month"};
        int[] labelRes = {
            R.string.chart_period_intraday,
            R.string.chart_period_day,
            R.string.chart_period_week,
            R.string.chart_period_month
        };
        for (int i = 0; i < periods.length; i++) {
            final String period = periods[i];
            boolean sel = period.equals(selectedPeriod);
            TextView chip = new TextView(requireContext());
            chip.setText(labelRes[i]);
            chip.setTextSize(13f);
            chip.setTypeface(sel ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
            chip.setTextColor(sel ? onColor : Color.argb(160,
                    Color.red(onColor), Color.green(onColor), Color.blue(onColor)));
            chip.setBackground(makePill(Color.argb(sel ? 90 : 34, 255, 255, 255), 999));
            chip.setPadding(dp(12), dp(6), dp(12), dp(6));
            chip.setOnClickListener(v -> {
                selectedPeriod = period;
                CampusMarketRepository.SchoolMarket m = CampusMarketRepository.getMarket(selectedMarketKey);
                List<CampusMarketRepository.MarketCandle> c = buildCandlesForPeriod(m, getLivePrice(m));
                viewMarketChart.setData(c, buildLabelsForPeriod(c.size()));
                int oc = ContextCompat.getColor(requireContext(), R.color.forum_header_on);
                int os = ContextCompat.getColor(requireContext(), R.color.forum_header_on_secondary);
                setupPeriodChips(oc, os);
            });
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            p.leftMargin = i == 0 ? 0 : dp(8);
            layoutChartPeriod.addView(chip, p);
        }
    }

    private List<CampusMarketRepository.MarketCandle> buildCandlesForPeriod(
            CampusMarketRepository.SchoolMarket market, int livePrice) {
        long seed = Math.abs((long) market.getForumKey().hashCode());
        Random rng = new Random(seed);
        switch (selectedPeriod) {
            case "intraday": {
                List<CampusMarketRepository.MarketCandle> daily = market.getCandles();
                int base = daily.size() >= 2 ? daily.get(daily.size() - 2).close : livePrice;
                List<CampusMarketRepository.MarketCandle> result = new ArrayList<>();
                int prev = base;
                for (int i = 0; i < 20; i++) {
                    if (i == 19) {
                        int open = prev;
                        int swing = Math.max(1, (int)(Math.abs(livePrice - open) * 0.3) + (int)(open * 0.002));
                        result.add(new CampusMarketRepository.MarketCandle(
                                open,
                                Math.max(open, livePrice) + swing,
                                Math.min(open, livePrice) - swing,
                                livePrice));
                    } else {
                        int maxMove = Math.max(1, (int)(prev * 0.012));
                        int delta = (int)(rng.nextDouble() * maxMove * 2) - (int)(maxMove * 0.9);
                        int close = Math.max(prev / 2, prev + delta);
                        int swing = Math.max(1, (int)(Math.abs(delta) * 0.4) + (int)(prev * 0.003));
                        result.add(new CampusMarketRepository.MarketCandle(
                                prev,
                                Math.max(prev, close) + rng.nextInt(swing + 1),
                                Math.min(prev, close) - rng.nextInt(swing + 1),
                                close));
                        prev = close;
                    }
                }
                return result;
            }
            case "week": {
                int base = (int)(market.getCandles().get(0).open * 0.78);
                List<CampusMarketRepository.MarketCandle> result = new ArrayList<>();
                int prev = base;
                for (int i = 0; i < 12; i++) {
                    int maxMove = Math.max(1, (int)(prev * 0.055));
                    int delta = (int)(rng.nextDouble() * maxMove * 2) - (int)(maxMove * 0.65);
                    int close = (i == 11) ? livePrice : Math.max(prev / 2, prev + delta);
                    int swing = Math.max(2, (int)(Math.abs(close - prev) * 0.6) + (int)(prev * 0.014));
                    result.add(new CampusMarketRepository.MarketCandle(
                            prev,
                            Math.max(prev, close) + swing,
                            Math.min(prev, close) - swing,
                            close));
                    prev = close;
                }
                return result;
            }
            case "month": {
                int base = (int)(market.getCandles().get(0).open * 0.52);
                List<CampusMarketRepository.MarketCandle> result = new ArrayList<>();
                int prev = base;
                for (int i = 0; i < 18; i++) {
                    int maxMove = Math.max(1, (int)(prev * 0.1));
                    int delta = (int)(rng.nextDouble() * maxMove * 2) - (int)(maxMove * 0.55);
                    int close = (i == 17) ? livePrice : Math.max(prev / 2, prev + delta);
                    int swing = Math.max(3, (int)(Math.abs(close - prev) * 0.7) + (int)(prev * 0.022));
                    result.add(new CampusMarketRepository.MarketCandle(
                            prev,
                            Math.max(prev, close) + swing,
                            Math.min(prev, close) - swing,
                            close));
                    prev = close;
                }
                return result;
            }
            default:
                return buildDisplayCandles(market, livePrice);
        }
    }

    private List<String> buildLabelsForPeriod(int candleCount) {
        Calendar cal = Calendar.getInstance();
        boolean isChinese = "zh-CN".equals(UiPreferences.getLanguageTag(requireContext()));
        List<String> labels = new ArrayList<>();
        switch (selectedPeriod) {
            case "intraday": {
                // Last candle = current system time; go back ~30min per candle
                int nowMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE);
                int stepMinutes = 30;
                for (int i = 0; i < candleCount; i++) {
                    int offset = (candleCount - 1 - i) * stepMinutes;
                    int totalMin = nowMinutes - offset;
                    if (totalMin < 0) totalMin += 24 * 60;
                    labels.add(String.format(Locale.US, "%d:%02d", totalMin / 60, totalMin % 60));
                }
                break;
            }
            case "week": {
                // Weekly: show Monday dates going back
                for (int i = candleCount - 1; i >= 0; i--) {
                    Calendar c = Calendar.getInstance();
                    c.add(Calendar.WEEK_OF_YEAR, -i);
                    labels.add(0, String.format(Locale.US, "%d/%d", c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH)));
                }
                break;
            }
            case "month": {
                // Monthly: each candle is ~2 weeks
                for (int i = candleCount - 1; i >= 0; i--) {
                    Calendar c = Calendar.getInstance();
                    c.add(Calendar.WEEK_OF_YEAR, -i * 2);
                    String label = isChinese
                            ? (c.get(Calendar.MONTH) + 1) + "月"
                            : String.format(Locale.US, "%d/%d", c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
                    labels.add(0, label);
                }
                break;
            }
            default: { // "day"
                // Daily: past candleCount days
                for (int i = candleCount - 1; i >= 0; i--) {
                    Calendar c = Calendar.getInstance();
                    c.add(Calendar.DAY_OF_YEAR, -i);
                    labels.add(0, String.format(Locale.US, "%d/%d", c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH)));
                }
                break;
            }
        }
        return labels;
    }

    @Nullable
    private MarketPortfolioStore.ShortPositionSnapshot findShortPosition(
            List<MarketPortfolioStore.ShortPositionSnapshot> shorts, String forumKey) {
        for (MarketPortfolioStore.ShortPositionSnapshot s : shorts) {
            if (s.getForumKey().equals(forumKey)) return s;
        }
        return null;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private MainActivity host() {
        return (MainActivity) requireActivity();
    }
}

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
import java.util.List;
import java.util.Locale;

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
    private TextView textMarketPitch;
    private TextView textMarketIndex;
    private TextView textMarketChange;
    private TextView textMarketFormulaLabel;
    private TextView textMarketFormulaValue;
    private TextView textMarketTriggerLabel;
    private MarketCandleChartView viewMarketChart;
    private LinearLayout layoutMarketTriggers;
    private TextView textTradeHint;
    private TextView textTradePrice;
    private EditText inputTradeAmount;
    private TextView textTradeEstimate;
    private Button buttonTradeBuy;
    private Button buttonTradeSell;
    private TextView textTradeResult;
    private LinearLayout layoutMarketPositions;
    private TextView textPositionEmpty;
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
        textMarketPitch = view.findViewById(R.id.textMarketPitch);
        textMarketIndex = view.findViewById(R.id.textMarketIndex);
        textMarketChange = view.findViewById(R.id.textMarketChange);
        textMarketFormulaLabel = view.findViewById(R.id.textMarketFormulaLabel);
        textMarketFormulaValue = view.findViewById(R.id.textMarketFormulaValue);
        textMarketTriggerLabel = view.findViewById(R.id.textMarketTriggerLabel);
        viewMarketChart = view.findViewById(R.id.viewMarketChart);
        layoutMarketTriggers = view.findViewById(R.id.layoutMarketTriggers);
        textTradeHint = view.findViewById(R.id.textTradeHint);
        textTradePrice = view.findViewById(R.id.textTradePrice);
        inputTradeAmount = view.findViewById(R.id.inputTradeAmount);
        textTradeEstimate = view.findViewById(R.id.textTradeEstimate);
        buttonTradeBuy = view.findViewById(R.id.buttonTradeBuy);
        buttonTradeSell = view.findViewById(R.id.buttonTradeSell);
        textTradeResult = view.findViewById(R.id.textTradeResult);
        layoutMarketPositions = view.findViewById(R.id.layoutMarketPositions);
        textPositionEmpty = view.findViewById(R.id.textPositionEmpty);

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
        renderPositions(snapshot.getPositions());
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
        textMarketForumName.setText(AppData.getForumLabel(requireContext(), selectedMarketKey));
        textMarketForumName.setTextColor(onColor);
        textMarketPitch.setText(market.getPitch(requireContext()));
        textMarketPitch.setTextColor(onSecondary);
        int livePrice = getLivePrice(market);
        textMarketIndex.setText(getString(R.string.market_index_format, livePrice));
        textMarketIndex.setTextColor(onColor);
        textMarketChange.setText(getString(R.string.market_change_today, formatPercent(getLiveDayChangePercent(market, livePrice))));
        textMarketChange.setTextColor(onColor);
        textMarketChange.setBackground(makePill(Color.argb(40, 255, 255, 255), 999));
        textMarketFormulaLabel.setTextColor(onSecondary);
        textMarketFormulaValue.setTextColor(onColor);
        textMarketFormulaValue.setText(getString(
                R.string.market_formula_value,
                market.getPostsToday(),
                market.getRepliesToday(),
                market.getLikesToday()
        ));
        textMarketTriggerLabel.setTextColor(onSecondary);

        viewMarketChart.setCandles(buildDisplayCandles(market, livePrice));
        renderTriggers(market.getTriggers(requireContext()), onColor);

        textTradeHint.setText(getString(R.string.market_buy_hint, AppData.getForumLabel(requireContext(), selectedMarketKey)));
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
        MarketPortfolioStore.PositionSnapshot selectedPosition = findPosition(snapshot.getPositions(), selectedMarketKey);
        int requestedSpend = parsePositiveInt(inputTradeAmount.getText().toString());

        if (requestedSpend <= 0) {
            textTradeEstimate.setText(R.string.market_estimate_placeholder);
            setTradeButtonStates(false, false);
            return;
        }

        int unitPrice = getLivePrice(market);
        int filledUnits = requestedSpend / unitPrice;
        if (filledUnits <= 0) {
            textTradeEstimate.setText(R.string.market_transaction_low_amount);
            setTradeButtonStates(false, false);
            return;
        }

        int unfilled = requestedSpend - (filledUnits * unitPrice);
        boolean canBuy = requestedSpend <= currentCash;
        boolean canSell = selectedPosition != null && requestedSpend <= selectedPosition.getCurrentValue();

        if (canBuy || canSell) {
            textTradeEstimate.setText(getString(
                    R.string.market_estimate_format,
                    requestedSpend,
                    filledUnits,
                    unitPrice,
                    unfilled
            ));
        } else if (selectedPosition == null || selectedPosition.getUnits() <= 0) {
            textTradeEstimate.setText(R.string.market_transaction_insufficient_balance);
        } else {
            textTradeEstimate.setText(R.string.market_transaction_insufficient_trade_capacity);
        }

        setTradeButtonStates(canBuy, canSell);
    }

    private void handleBuy() {
        CampusMarketRepository.SchoolMarket market = CampusMarketRepository.getMarket(selectedMarketKey);
        int requestedSpend = parsePositiveInt(inputTradeAmount.getText().toString());
        int unitPrice = getLivePrice(market);
        MarketPortfolioStore.TradeResult result = MarketPortfolioStore.buy(
                requireContext(),
                selectedMarketKey,
                requestedSpend,
                unitPrice
        );

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
        } else if (result.getStatus() == MarketPortfolioStore.TradeResult.STATUS_BELOW_UNIT_PRICE) {
            lastTradeMessage = getString(R.string.market_transaction_low_amount);
        } else {
            lastTradeMessage = getString(R.string.market_transaction_invalid_amount);
        }
        lastTradeStatus = result.getStatus();
        renderTradeResult();
    }

    private void handleSell() {
        CampusMarketRepository.SchoolMarket market = CampusMarketRepository.getMarket(selectedMarketKey);
        int requestedValue = parsePositiveInt(inputTradeAmount.getText().toString());
        int unitPrice = getLivePrice(market);
        MarketPortfolioStore.TradeResult result = MarketPortfolioStore.sell(
                requireContext(),
                selectedMarketKey,
                requestedValue,
                unitPrice
        );

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
        } else if (result.getStatus() == MarketPortfolioStore.TradeResult.STATUS_BELOW_UNIT_PRICE) {
            lastTradeMessage = getString(R.string.market_transaction_low_amount);
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

    private void renderPositions(List<MarketPortfolioStore.PositionSnapshot> positions) {
        layoutMarketPositions.removeAllViews();
        textPositionEmpty.setVisibility(positions.isEmpty() ? View.VISIBLE : View.GONE);

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
            forum.setText(AppData.getForumLabel(requireContext(), position.getForumKey()));
            meta.setText(getString(
                    R.string.market_position_meta,
                    position.getUnits(),
                    position.getAverageCost()
            ));
            value.setText(getString(
                    R.string.market_position_value,
                    position.getCurrentValue(),
                    position.getCurrentPrice()
            ));
            units.setText(getString(R.string.market_position_units, position.getUnits()));
            pnl.setText(getString(R.string.market_position_pnl, formatSignedTokens(position.getOpenPnl())));
            pnl.setTextColor(resolvePnlColor(position.getOpenPnl()));
            layoutMarketPositions.addView(item);
        }
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

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private MainActivity host() {
        return (MainActivity) requireActivity();
    }
}

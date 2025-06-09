package com.jbqneto.dev.botdev.strategy;

import com.jbqneto.dev.botdev.config.StrategyConfig;
import com.jbqneto.dev.botdev.dto.BollingerBandsValues;
import com.jbqneto.dev.botdev.dto.Candlestick;
import com.jbqneto.dev.botdev.dto.TradingDecision;
import com.jbqneto.dev.botdev.service.ExchangeDataService;
import com.jbqneto.dev.botdev.service.TechnicalIndicatorService;
import lombok.extern.slf4j.Slf4j; // Added
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ta4j.core.num.Num;
import org.ta4j.core.num.DecimalNum; // Using DecimalNum for comparisons

import java.util.List;

@Service
@Slf4j
public class DayTradingStrategyImpl implements TradingStrategy {

    private final ExchangeDataService exchangeDataService;
    private final TechnicalIndicatorService technicalIndicatorService;
    private final StrategyConfig strategyConfig;

    @Autowired
    public DayTradingStrategyImpl(ExchangeDataService exchangeDataService, // Changed
                                  TechnicalIndicatorService technicalIndicatorService,
                                  StrategyConfig strategyConfig) {
        this.exchangeDataService = exchangeDataService; // Changed
        this.technicalIndicatorService = technicalIndicatorService;
        this.strategyConfig = strategyConfig;
    }

    @Override
    public TradingDecision generateSignal(String symbol, String timeframe) {
        // For this strategy, timeframe parameter might be fixed (e.g. 15m as primary) or used from config.
        // Let's use the defaultTimeframe from config as the primary.
        String primaryTimeframe = strategyConfig.getDefaultTimeframe();
        int candleFetchLimit = 200; // Enough for common indicators like EMA200 or BollingerBands(20) + lead-in data

        log.info("Generating signal for {} on primary timeframe {}", symbol, primaryTimeframe); // logger to log

        // 1. Fetch Data
        List<Candlestick> candlesticks = exchangeDataService.getCandlestickBars(symbol, primaryTimeframe, candleFetchLimit);
        if (candlesticks == null || candlesticks.size() < strategyConfig.getEmaLongPeriod()) { // Ensure enough for longest EMA
            log.warn("Not enough candlestick data for symbol {} on timeframe {}. Found {} candles, need at least {}.", // logger to log
                    symbol, primaryTimeframe, candlesticks == null ? 0 : candlesticks.size(), strategyConfig.getEmaLongPeriod());
            return new TradingDecision(TradeSignal.HOLD, symbol, null, "Insufficient data");
        }

        // 2. Calculate Indicators
        Num vwap = technicalIndicatorService.calculateVWAP(candlesticks, 20); // Using a common period for VWAP
        Num rsi = technicalIndicatorService.calculateRSI(candlesticks, strategyConfig.getRsiPeriod());
        Num emaShort = technicalIndicatorService.calculateEMA(candlesticks, strategyConfig.getEmaShortPeriod());
        Num emaLong = technicalIndicatorService.calculateEMA(candlesticks, strategyConfig.getEmaLongPeriod());
        BollingerBandsValues bb = technicalIndicatorService.calculateBollingerBands(
                candlesticks,
                strategyConfig.getBollingerPeriod(),
                strategyConfig.getBollingerStdDev()
        );
        Num avgVolume = technicalIndicatorService.calculateAverageVolume(candlesticks, strategyConfig.getVolumeAvgPeriod());

        if (vwap == null || rsi == null || emaShort == null || emaLong == null || bb == null || avgVolume == null) {
            log.warn("One or more core indicators could not be calculated for {} on {}.", symbol, primaryTimeframe); // logger to log
            return new TradingDecision(TradeSignal.HOLD, symbol, null, "Indicator calculation error");
        }

        // 3. Access Current and Previous Candle Data
        Candlestick currentCandle = candlesticks.get(candlesticks.size() - 1);
        Candlestick prevCandle = candlesticks.size() > 1 ? candlesticks.get(candlesticks.size() - 2) : null;

        Num currentClose = DecimalNum.valueOf(currentCandle.getClosePrice());
        Num currentVolume = DecimalNum.valueOf(currentCandle.getVolume());

        // Previous values for indicators (requires at least 2 values from indicator calculation, so more history)
        // This part needs careful handling of indicator list sizes or re-calculating for previous point.
        // For simplicity, TA4J indicators are calculated on the whole series, and we get the latest value.
        // To get previous, we'd need the indicator value at series.getEndIndex() - 1.
        // This means the indicator services should be flexible or we calculate here.
        // For now, let's assume we need to re-calculate for previous point if needed, or this logic is simplified.
        // Simplified: Compare current EMA values. For crossover, we need previous EMAs.
        // Let's fetch slightly more data and then get indicator values at -1 and -2 index from series.
        // This is complex with current TechnicalIndicatorService which returns only latest.
        // WORKAROUND: For EMA crossover, re-calculate with one less candle for previous values.
        // This is inefficient but fits current service design.
        // TODO: Refactor TechnicalIndicatorService to return multiple recent values or full Indicator objects
        // to avoid recalculating indicators for previous candle states.
        List<Candlestick> prevCandlesticks = candlesticks.subList(0, candlesticks.size() -1);
        Num prevEmaShort = null;
        Num prevEmaLong = null;
        Num prevRsi = null;
        Num prevVwap = null;

        if (prevCandle != null && prevCandlesticks.size() >= Math.max(strategyConfig.getEmaLongPeriod(), strategyConfig.getRsiPeriod())) {
            prevEmaShort = technicalIndicatorService.calculateEMA(prevCandlesticks, strategyConfig.getEmaShortPeriod());
            prevEmaLong = technicalIndicatorService.calculateEMA(prevCandlesticks, strategyConfig.getEmaLongPeriod());
            prevRsi = technicalIndicatorService.calculateRSI(prevCandlesticks, strategyConfig.getRsiPeriod());
            prevVwap = technicalIndicatorService.calculateVWAP(prevCandlesticks, 20);
        }


        // --- LONG ENTRY CONDITIONS ---
        TradingDecision longEntryDecision = checkForLongEntry(symbol, currentCandle, prevCandle, candlesticks, prevCandlesticks, vwap, prevVwap, rsi, prevRsi, emaShort, prevEmaShort, emaLong, prevEmaLong, currentVolume, avgVolume);
        if (longEntryDecision.getSignal() == TradeSignal.LONG_ENTRY) {
            return longEntryDecision;
        }

        // --- SHORT ENTRY CONDITIONS ---
        TradingDecision shortEntryDecision = checkForShortEntry(symbol, currentCandle, prevCandle, candlesticks, prevCandlesticks, vwap, prevVwap, rsi, prevRsi, emaShort, prevEmaShort, emaLong, prevEmaLong, currentVolume, avgVolume);
        if (shortEntryDecision.getSignal() == TradeSignal.SHORT_ENTRY) {
            return shortEntryDecision;
        }

        // --- EXIT CONDITIONS (Highly simplified and placeholder - requires state of current position) ---
        // These require knowing the state of an active position (entry price, type).
        // This service is stateless for now. A real system would have a PositionManagementService.
        // For example, if a long position is active:
        // Num entryPrice = ...; // Would come from active position data
        // if (currentClose.isGreaterThanOrEqual(entryPrice.multipliedBy(DecimalNum.valueOf(1 + strategyConfig.getLongExitTarget1Percent()/100.0)))) {
        //    return new TradingDecision(TradeSignal.LONG_EXIT, symbol, currentClose, "Target 1 profit hit.");
        // }
        // if (currentClose.isLessThanOrEqual(entryPrice.multipliedBy(DecimalNum.valueOf(1 - strategyConfig.getLongStopLossPercent()/100.0)))) {
        //    return new TradingDecision(TradeSignal.LONG_EXIT, symbol, currentClose, "Stop loss hit.");
        // }

        log.info("No clear signal for {}. Conditions: VWAP={}, RSI={}, EMA9={}, EMA21={}, Vol={}, AvgVol={}", // logger to log
                symbol, vwap != null ? vwap.doubleValue() : "N/A",
                rsi != null ? rsi.doubleValue() : "N/A",
                emaShort != null ? emaShort.doubleValue() : "N/A",
                emaLong != null ? emaLong.doubleValue() : "N/A",
                currentVolume != null ? currentVolume.doubleValue() : "N/A",
                avgVolume != null ? avgVolume.doubleValue() : "N/A");
        return new TradingDecision(TradeSignal.HOLD, symbol, currentClose, "No entry conditions met");
    }

    private TradingDecision checkForLongEntry(String symbol, Candlestick currentCandle, Candlestick prevCandle,
                                            List<Candlestick> candlesticks, List<Candlestick> prevCandlesticks,
                                            Num vwap, Num prevVwap, Num rsi, Num prevRsi,
                                            Num emaShort, Num prevEmaShort, Num emaLong, Num prevEmaLong,
                                            Num currentVolume, Num avgVolume) {

        Num currentClose = DecimalNum.valueOf(currentCandle.getClosePrice());
        StringBuilder longReasonBuilder = new StringBuilder();

        boolean priceCrossedAboveVwap = false;
        if (prevVwap != null && prevCandle != null && vwap != null && currentClose != null) {
            Num prevClose = DecimalNum.valueOf(prevCandle.getClosePrice());
            if (prevClose.isLessThan(prevVwap) && currentClose.isGreaterThan(vwap)) {
                priceCrossedAboveVwap = true;
                longReasonBuilder.append("Price crossed above VWAP. ");
            }
        } else if (vwap != null && currentClose != null && currentClose.isGreaterThan(vwap)) {
            priceCrossedAboveVwap = true;
            longReasonBuilder.append("Price above VWAP (current data only). ");
        }

        boolean rsiConditionMet = false;
        if (prevRsi != null && rsi != null) {
            if (prevRsi.isLessThan(DecimalNum.valueOf(30)) && rsi.isGreaterThan(prevRsi)) {
                rsiConditionMet = true;
                longReasonBuilder.append("RSI rising from oversold (<30). ");
            }
        }

        boolean emaCrossoverMet = false;
        if (prevEmaShort != null && prevEmaLong != null && emaShort != null && emaLong != null) {
            if (prevEmaShort.isLessThan(prevEmaLong) && emaShort.isGreaterThan(emaLong)) {
                emaCrossoverMet = true;
                longReasonBuilder.append("EMA9 crossed above EMA21. ");
            }
        } else if (emaShort != null && emaLong != null && emaShort.isGreaterThan(emaLong)) {
            emaCrossoverMet = true;
            longReasonBuilder.append("EMA9 currently above EMA21. ");
        }

        boolean volumeConditionMet = false;
        if (currentVolume != null && avgVolume != null && currentVolume.isGreaterThan(avgVolume)) {
            volumeConditionMet = true;
            longReasonBuilder.append("Volume above average. ");
        }

        // Optional: 5-minute candle closes above VWAP (Not implemented)

        if (priceCrossedAboveVwap && rsiConditionMet && emaCrossoverMet && volumeConditionMet) {
            log.info("LONG_ENTRY signal for {}: {}", symbol, longReasonBuilder.toString().trim()); // logger to log
            return new TradingDecision(TradeSignal.LONG_ENTRY, symbol, currentClose, longReasonBuilder.toString().trim());
        }
        return new TradingDecision(TradeSignal.HOLD, symbol); // Default to HOLD if conditions not met
    }

    private TradingDecision checkForShortEntry(String symbol, Candlestick currentCandle, Candlestick prevCandle,
                                             List<Candlestick> candlesticks, List<Candlestick> prevCandlesticks,
                                             Num vwap, Num prevVwap, Num rsi, Num prevRsi,
                                             Num emaShort, Num prevEmaShort, Num emaLong, Num prevEmaLong,
                                             Num currentVolume, Num avgVolume) {

        Num currentClose = DecimalNum.valueOf(currentCandle.getClosePrice());
        StringBuilder shortReasonBuilder = new StringBuilder();

        boolean priceCrossedBelowVwap = false;
        if (prevVwap != null && prevCandle != null && vwap != null && currentClose != null) {
            Num prevClose = DecimalNum.valueOf(prevCandle.getClosePrice());
            if (prevClose.isGreaterThan(prevVwap) && currentClose.isLessThan(vwap)) {
                priceCrossedBelowVwap = true;
                shortReasonBuilder.append("Price crossed below VWAP. ");
            }
        } else if (vwap != null && currentClose != null && currentClose.isLessThan(vwap)) {
            priceCrossedBelowVwap = true;
            shortReasonBuilder.append("Price below VWAP (current data only). ");
        }

        boolean rsiShortConditionMet = false;
        if (prevRsi != null && rsi != null) {
            if (prevRsi.isGreaterThan(DecimalNum.valueOf(70)) && rsi.isLessThan(prevRsi)) {
                rsiShortConditionMet = true;
                shortReasonBuilder.append("RSI falling from overbought (>70). ");
            }
        }

        boolean emaCrossdownMet = false;
        if (prevEmaShort != null && prevEmaLong != null && emaShort != null && emaLong != null) {
            if (prevEmaShort.isGreaterThan(prevEmaLong) && emaShort.isLessThan(emaLong)) {
                emaCrossdownMet = true;
                shortReasonBuilder.append("EMA9 crossed below EMA21. ");
            }
        } else if (emaShort != null && emaLong != null && emaShort.isLessThan(emaLong)) {
            emaCrossdownMet = true;
            shortReasonBuilder.append("EMA9 currently below EMA21. ");
        }

        boolean volumeBearishConditionMet = false;
        if (currentVolume != null && avgVolume != null && currentClose != null && currentCandle.getOpenPrice() != null &&
            currentVolume.isGreaterThan(avgVolume) && currentClose.isLessThan(DecimalNum.valueOf(currentCandle.getOpenPrice()))) {
            volumeBearishConditionMet = true;
            shortReasonBuilder.append("Increased volume on bearish candle. ");
        }

        if (priceCrossedBelowVwap && rsiShortConditionMet && emaCrossdownMet && volumeBearishConditionMet) {
            log.info("SHORT_ENTRY signal for {}: {}", symbol, shortReasonBuilder.toString().trim()); // logger to log
            return new TradingDecision(TradeSignal.SHORT_ENTRY, symbol, currentClose, shortReasonBuilder.toString().trim());
        }
        return new TradingDecision(TradeSignal.HOLD, symbol); // Default to HOLD
    }
}

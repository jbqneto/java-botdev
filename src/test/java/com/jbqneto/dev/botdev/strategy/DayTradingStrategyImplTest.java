package com.jbqneto.dev.botdev.strategy;

import com.jbqneto.dev.botdev.config.TradingConfig;
import com.jbqneto.dev.botdev.dto.BollingerBandsValues;
import com.jbqneto.dev.botdev.dto.Candlestick;
import com.jbqneto.dev.botdev.dto.TradingDecision;
import com.jbqneto.dev.botdev.service.BinanceDataService;
import com.jbqneto.dev.botdev.service.TechnicalIndicatorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ta4j.core.num.DecimalNum;
import org.ta4j.core.num.Num;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DayTradingStrategyImplTest {

    @Mock
    private BinanceDataService mockBinanceDataService;

    @Mock
    private TechnicalIndicatorService mockTechnicalIndicatorService;

    @Mock
    private TradingConfig mockTradingConfig;

    @InjectMocks
    private DayTradingStrategyImpl dayTradingStrategy;

    private Candlestick currentCandle;
    private Candlestick prevCandle;
    private List<Candlestick> candlesticks;
    private List<Candlestick> prevCandlesticks;

    @BeforeEach
    void setUp() {
        // Default TradingConfig setup
        TradingConfig.Trading nestedTradingConfig = new TradingConfig.Trading();
        nestedTradingConfig.setDefaultTimeframe("15m");
        nestedTradingConfig.setRsiPeriod(14);
        nestedTradingConfig.setEmaShortPeriod(9);
        nestedTradingConfig.setEmaLongPeriod(21);
        nestedTradingConfig.setBollingerPeriod(20);
        nestedTradingConfig.setBollingerStdDev(2.0);
        nestedTradingConfig.setVolumeAvgPeriod(10);
        // Exit targets are not used in this stateless signal generation directly
        // TradingHours also not used in this iteration of tests

        when(mockTradingConfig.getTrading()).thenReturn(nestedTradingConfig);

        // Prepare mock candlesticks
        // Create 200 candles to ensure enough data for all indicators and previous values
        candlesticks = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            Candlestick candle = new Candlestick();
            candle.setOpenTime(System.currentTimeMillis() - (200L - i) * 900000); // 15 min interval
            candle.setCloseTime(System.currentTimeMillis() - (200L - i - 1) * 900000);
            candle.setOpenPrice(String.valueOf(100 + i));
            candle.setHighPrice(String.valueOf(105 + i));
            candle.setLowPrice(String.valueOf(95 + i));
            candle.setClosePrice(String.valueOf(102 + i));
            candle.setVolume(String.valueOf(1000 + i * 10));
            candle.setNumberOfTrades(100 + i);
            candle.setQuoteAssetVolume(String.valueOf((102 + i) * (1000 + i * 10)));
            candle.setTakerBuyBaseAssetVolume(String.valueOf(500 + i * 5));
            candle.setTakerBuyQuoteAssetVolume(String.valueOf((102 + i) * (500 + i * 5)));
            candlesticks.add(candle);
        }
        currentCandle = candlesticks.get(candlesticks.size() - 1);
        prevCandle = candlesticks.get(candlesticks.size() - 2);
        prevCandlesticks = candlesticks.subList(0, candlesticks.size() - 1);

        when(mockBinanceDataService.getCandlestickBars(anyString(), anyString(), anyInt()))
                .thenReturn(candlesticks);
    }

    private void setupCommonIndicatorMocks() {
        // Current values
        when(mockTechnicalIndicatorService.calculateVWAP(eq(candlesticks), anyInt())).thenReturn(DecimalNum.valueOf(currentCandle.getClosePrice()));
        when(mockTechnicalIndicatorService.calculateRSI(eq(candlesticks), anyInt())).thenReturn(DecimalNum.valueOf(50));
        when(mockTechnicalIndicatorService.calculateEMA(eq(candlesticks), eq(mockTradingConfig.getTrading().getEmaShortPeriod()))).thenReturn(DecimalNum.valueOf(100));
        when(mockTechnicalIndicatorService.calculateEMA(eq(candlesticks), eq(mockTradingConfig.getTrading().getEmaLongPeriod()))).thenReturn(DecimalNum.valueOf(98));
        when(mockTechnicalIndicatorService.calculateBollingerBands(eq(candlesticks), anyInt(), anyDouble()))
                .thenReturn(new BollingerBandsValues(DecimalNum.valueOf(110), DecimalNum.valueOf(100), DecimalNum.valueOf(90)));
        when(mockTechnicalIndicatorService.calculateAverageVolume(eq(candlesticks), anyInt())).thenReturn(DecimalNum.valueOf(1000));

        // Previous values (for crossovers, etc.)
        when(mockTechnicalIndicatorService.calculateVWAP(eq(prevCandlesticks), anyInt())).thenReturn(DecimalNum.valueOf(prevCandle.getClosePrice()));
        when(mockTechnicalIndicatorService.calculateRSI(eq(prevCandlesticks), anyInt())).thenReturn(DecimalNum.valueOf(25)); // RSI was low
        when(mockTechnicalIndicatorService.calculateEMA(eq(prevCandlesticks), eq(mockTradingConfig.getTrading().getEmaShortPeriod()))).thenReturn(DecimalNum.valueOf(95)); // EMA9 was below
        when(mockTechnicalIndicatorService.calculateEMA(eq(prevCandlesticks), eq(mockTradingConfig.getTrading().getEmaLongPeriod()))).thenReturn(DecimalNum.valueOf(96)); // EMA21
    }


    @Test
    void generateSignal_shouldReturnHold_whenNoConditionsMet() {
        setupCommonIndicatorMocks(); // Default mocks, usually won't trigger signals

        TradingDecision decision = dayTradingStrategy.generateSignal("BTCUSDT", "15m");
        assertEquals(TradeSignal.HOLD, decision.getSignal());
    }

    @Test
    void generateSignal_shouldReturnLongEntry_whenAllConditionsMet() {
        setupCommonIndicatorMocks();
        // Override specific mocks for LONG_ENTRY
        // 1. Price crossed above VWAP
        when(mockTechnicalIndicatorService.calculateVWAP(eq(candlesticks), anyInt())).thenReturn(DecimalNum.valueOf(currentCandle.getClosePrice())); // current close > current VWAP (simplified)
        when(mockTechnicalIndicatorService.calculateVWAP(eq(prevCandlesticks), anyInt())).thenReturn(DecimalNum.valueOf(Double.parseDouble(prevCandle.getClosePrice()) + 1)); // prev close < prev VWAP

        // 2. RSI was below 30 and is now rising
        when(mockTechnicalIndicatorService.calculateRSI(eq(prevCandlesticks), anyInt())).thenReturn(DecimalNum.valueOf(25)); // prevRSI < 30
        when(mockTechnicalIndicatorService.calculateRSI(eq(candlesticks), anyInt())).thenReturn(DecimalNum.valueOf(35));    // currentRSI > prevRSI

        // 3. EMA9 crosses above EMA21
        when(mockTechnicalIndicatorService.calculateEMA(eq(prevCandlesticks), eq(mockTradingConfig.getTrading().getEmaShortPeriod()))).thenReturn(DecimalNum.valueOf(95)); // prevEMA9
        when(mockTechnicalIndicatorService.calculateEMA(eq(prevCandlesticks), eq(mockTradingConfig.getTrading().getEmaLongPeriod()))).thenReturn(DecimalNum.valueOf(96)); // prevEMA21 (prevEMA9 < prevEMA21)
        when(mockTechnicalIndicatorService.calculateEMA(eq(candlesticks), eq(mockTradingConfig.getTrading().getEmaShortPeriod()))).thenReturn(DecimalNum.valueOf(98));   // currentEMA9
        when(mockTechnicalIndicatorService.calculateEMA(eq(candlesticks), eq(mockTradingConfig.getTrading().getEmaLongPeriod()))).thenReturn(DecimalNum.valueOf(97));   // currentEMA21 (currentEMA9 > currentEMA21)

        // 4. Current candle's volume > average volume
        currentCandle.setVolume(String.valueOf(1500)); // current volume
        when(mockTechnicalIndicatorService.calculateAverageVolume(eq(candlesticks), anyInt())).thenReturn(DecimalNum.valueOf(1000)); // avg volume

        TradingDecision decision = dayTradingStrategy.generateSignal("BTCUSDT", "15m");
        assertEquals(TradeSignal.LONG_ENTRY, decision.getSignal());
        assertTrue(decision.getReason().contains("Price crossed above VWAP"));
        assertTrue(decision.getReason().contains("RSI rising from oversold"));
        assertTrue(decision.getReason().contains("EMA9 crossed above EMA21"));
        assertTrue(decision.getReason().contains("Volume above average"));
    }

    @Test
    void generateSignal_shouldReturnShortEntry_whenAllConditionsMet() {
        setupCommonIndicatorMocks();
        // Override specific mocks for SHORT_ENTRY
        // 1. Price crossed below VWAP
        when(mockTechnicalIndicatorService.calculateVWAP(eq(candlesticks), anyInt())).thenReturn(DecimalNum.valueOf(currentCandle.getClosePrice())); // current close < current VWAP (simplified for test)
        when(mockTechnicalIndicatorService.calculateVWAP(eq(prevCandlesticks), anyInt())).thenReturn(DecimalNum.valueOf(Double.parseDouble(prevCandle.getClosePrice()) - 1)); // prev close > prev VWAP
        currentCandle.setClosePrice(String.valueOf(Double.parseDouble(currentCandle.getClosePrice()) - 2)); // Ensure current close < VWAP

        // 2. RSI was above 70 and is now falling
        when(mockTechnicalIndicatorService.calculateRSI(eq(prevCandlesticks), anyInt())).thenReturn(DecimalNum.valueOf(75)); // prevRSI > 70
        when(mockTechnicalIndicatorService.calculateRSI(eq(candlesticks), anyInt())).thenReturn(DecimalNum.valueOf(65));    // currentRSI < prevRSI

        // 3. EMA9 crosses below EMA21
        when(mockTechnicalIndicatorService.calculateEMA(eq(prevCandlesticks), eq(mockTradingConfig.getTrading().getEmaShortPeriod()))).thenReturn(DecimalNum.valueOf(98)); // prevEMA9
        when(mockTechnicalIndicatorService.calculateEMA(eq(prevCandlesticks), eq(mockTradingConfig.getTrading().getEmaLongPeriod()))).thenReturn(DecimalNum.valueOf(97)); // prevEMA21 (prevEMA9 > prevEMA21)
        when(mockTechnicalIndicatorService.calculateEMA(eq(candlesticks), eq(mockTradingConfig.getTrading().getEmaShortPeriod()))).thenReturn(DecimalNum.valueOf(95));   // currentEMA9
        when(mockTechnicalIndicatorService.calculateEMA(eq(candlesticks), eq(mockTradingConfig.getTrading().getEmaLongPeriod()))).thenReturn(DecimalNum.valueOf(96));   // currentEMA21 (currentEMA9 < currentEMA21)

        // 4. Current candle's volume > average volume AND candle is bearish
        currentCandle.setVolume(String.valueOf(1500)); // current volume
        currentCandle.setOpenPrice(String.valueOf(Double.parseDouble(currentCandle.getClosePrice()) + 1)); // Ensure bearish candle (close < open)
        when(mockTechnicalIndicatorService.calculateAverageVolume(eq(candlesticks), anyInt())).thenReturn(DecimalNum.valueOf(1000)); // avg volume

        TradingDecision decision = dayTradingStrategy.generateSignal("BTCUSDT", "15m");
        assertEquals(TradeSignal.SHORT_ENTRY, decision.getSignal());
        assertTrue(decision.getReason().contains("Price crossed below VWAP"));
        assertTrue(decision.getReason().contains("RSI falling from overbought"));
        assertTrue(decision.getReason().contains("EMA9 crossed below EMA21"));
        assertTrue(decision.getReason().contains("Increased volume on bearish candle"));
    }

    @Test
    void generateSignal_shouldReturnHold_whenDataInsufficient() {
        when(mockBinanceDataService.getCandlestickBars(anyString(), anyString(), anyInt()))
                .thenReturn(candlesticks.subList(0, 10)); // Not enough for EMA21

        TradingDecision decision = dayTradingStrategy.generateSignal("BTCUSDT", "15m");
        assertEquals(TradeSignal.HOLD, decision.getSignal());
        assertEquals("Insufficient data", decision.getReason());
    }

    @Test
    void generateSignal_shouldReturnHold_whenIndicatorsNull() {
        // Setup a specific indicator to be null
        when(mockTechnicalIndicatorService.calculateVWAP(eq(candlesticks), anyInt())).thenReturn(null);
        // Other indicators might be fine
        when(mockTechnicalIndicatorService.calculateRSI(eq(candlesticks), anyInt())).thenReturn(DecimalNum.valueOf(50));
        when(mockTechnicalIndicatorService.calculateEMA(eq(candlesticks), eq(mockTradingConfig.getTrading().getEmaShortPeriod()))).thenReturn(DecimalNum.valueOf(100));
        when(mockTechnicalIndicatorService.calculateEMA(eq(candlesticks), eq(mockTradingConfig.getTrading().getEmaLongPeriod()))).thenReturn(DecimalNum.valueOf(98));
        when(mockTechnicalIndicatorService.calculateBollingerBands(eq(candlesticks), anyInt(), anyDouble()))
                .thenReturn(new BollingerBandsValues(DecimalNum.valueOf(110), DecimalNum.valueOf(100), DecimalNum.valueOf(90)));
        when(mockTechnicalIndicatorService.calculateAverageVolume(eq(candlesticks), anyInt())).thenReturn(DecimalNum.valueOf(1000));


        TradingDecision decision = dayTradingStrategy.generateSignal("BTCUSDT", "15m");
        assertEquals(TradeSignal.HOLD, decision.getSignal());
        assertEquals("Indicator calculation error", decision.getReason());
    }
}

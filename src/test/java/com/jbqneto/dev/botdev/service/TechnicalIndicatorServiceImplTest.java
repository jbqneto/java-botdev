package com.jbqneto.dev.botdev.service;

import com.jbqneto.dev.botdev.dto.BollingerBandsValues;
import com.jbqneto.dev.botdev.dto.Candlestick;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeries;
import org.ta4j.core.num.Num;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TechnicalIndicatorServiceImplTest {

    private TechnicalIndicatorServiceImpl technicalIndicatorService;
    private List<Candlestick> mockCandlesticks;

    @BeforeEach
    void setUp() {
        technicalIndicatorService = new TechnicalIndicatorServiceImpl();
        mockCandlesticks = createMockCandlestickData(25); // Create enough data for common periods
    }

    private List<Candlestick> createMockCandlestickData(int count) {
        List<Candlestick> candlesticks = new ArrayList<>();
        long currentTime = Instant.now().toEpochMilli();
        long intervalMillis = 60000 * 15; // 15 minutes

        for (int i = 0; i < count; i++) {
            double open = 100.0 + i;
            double high = 105.0 + i;
            double low = 95.0 + i;
            double close = 102.0 + i;
            double volume = 10.0 + i * 0.5;
            long openTime = currentTime - (count - i -1) * intervalMillis - intervalMillis; // Ensure open time is before close time
            long closeTime = currentTime - (count - i - 1) * intervalMillis;


            candlesticks.add(new Candlestick(
                    openTime,
                    String.valueOf(open),
                    String.valueOf(high),
                    String.valueOf(low),
                    String.valueOf(close),
                    String.valueOf(volume),
                    closeTime,
                    String.valueOf(volume * close), // Quote asset volume
                    100 + i, // Number of trades
                    String.valueOf(volume * 0.6), // Taker buy base
                    String.valueOf(volume * 0.6 * close) // Taker buy quote
            ));
        }
        return candlesticks;
    }

    @Test
    void testConvertToBarSeries() {
        BarSeries series = technicalIndicatorService.convertToBarSeries(mockCandlesticks, "TestSeries");
        assertNotNull(series);
        assertEquals("TestSeries", series.getName());
        assertEquals(mockCandlesticks.size(), series.getBarCount());

        if (!mockCandlesticks.isEmpty() && series.getBarCount() > 0) {
            Candlestick firstCandle = mockCandlesticks.get(0);
            org.ta4j.core.Bar firstBar = series.getBar(0);

            assertEquals(firstCandle.getClosePrice(), firstBar.getClosePrice().toString());
            assertEquals(firstCandle.getOpenPrice(), firstBar.getOpenPrice().toString());
            assertEquals(firstCandle.getHighPrice(), firstBar.getHighPrice().toString());
            assertEquals(firstCandle.getLowPrice(), firstBar.getLowPrice().toString());
            assertEquals(firstCandle.getVolume(), firstBar.getVolume().toString());

            ZonedDateTime expectedZonedDateTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(firstCandle.getCloseTime()), ZoneId.systemDefault());
            assertEquals(expectedZonedDateTime, firstBar.getEndTime());
        }
    }

    @Test
    void testConvertToBarSeries_emptyList() {
        List<Candlestick> emptyList = new ArrayList<>();
        BarSeries series = technicalIndicatorService.convertToBarSeries(emptyList, "EmptySeries");
        assertNotNull(series);
        assertEquals("EmptySeries", series.getName());
        assertEquals(0, series.getBarCount());
    }

    @Test
    void testCalculateVWAP() {
        Num vwap = technicalIndicatorService.calculateVWAP(mockCandlesticks, 14);
        assertNotNull(vwap);
        // VWAP should be a positive value if prices/volumes are positive
        assertTrue(vwap.isPositiveOrZero());

        // Test with insufficient data
        Num vwapShort = technicalIndicatorService.calculateVWAP(createMockCandlestickData(10), 14);
        assertNull(vwapShort);
    }

    @Test
    void testCalculateRSI() {
        Num rsi = technicalIndicatorService.calculateRSI(mockCandlesticks, 14);
        assertNotNull(rsi);
        assertTrue(rsi.isGreaterThanOrEqual(DecimalNum.valueOf(0)) && rsi.isLessThanOrEqual(DecimalNum.valueOf(100)));

        Num rsiShort = technicalIndicatorService.calculateRSI(createMockCandlestickData(10), 14);
        assertNull(rsiShort);
    }

    @Test
    void testCalculateEMA() {
        Num ema9 = technicalIndicatorService.calculateEMA(mockCandlesticks, 9);
        assertNotNull(ema9);
        assertTrue(ema9.isPositive());

        Num ema21 = technicalIndicatorService.calculateEMA(mockCandlesticks, 21);
        assertNotNull(ema21);
        assertTrue(ema21.isPositive());

        Num emaShort = technicalIndicatorService.calculateEMA(createMockCandlestickData(5), 9);
        assertNull(emaShort);
    }

    @Test
    void testCalculateBollingerBands() {
        BollingerBandsValues bb = technicalIndicatorService.calculateBollingerBands(mockCandlesticks, 20, 2.0);
        assertNotNull(bb);
        assertNotNull(bb.getUpper());
        assertNotNull(bb.getMiddle());
        assertNotNull(bb.getLower());
        assertTrue(bb.getUpper().isGreaterThanOrEqual(bb.getMiddle()));
        assertTrue(bb.getMiddle().isGreaterThanOrEqual(bb.getLower()));

        BollingerBandsValues bbShort = technicalIndicatorService.calculateBollingerBands(createMockCandlestickData(10), 20, 2);
        assertNull(bbShort);
    }

    @Test
    void testCalculateAverageVolume() {
        Num avgVolume = technicalIndicatorService.calculateAverageVolume(mockCandlesticks, 10);
        assertNotNull(avgVolume);
        assertTrue(avgVolume.isPositiveOrZero());

        Num avgVolShort = technicalIndicatorService.calculateAverageVolume(createMockCandlestickData(5), 10);
        assertNull(avgVolShort);
    }
}

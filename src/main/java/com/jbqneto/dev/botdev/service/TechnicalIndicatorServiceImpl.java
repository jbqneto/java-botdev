package com.jbqneto.dev.botdev.service;

import com.jbqneto.dev.botdev.dto.BollingerBandsValues;
import com.jbqneto.dev.botdev.dto.Candlestick;
import lombok.extern.slf4j.Slf4j; // Added
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.ta4j.core.*;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.VWAPIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsLowerIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsMiddleIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsUpperIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.statistics.StandardDeviationIndicator;
import org.ta4j.core.indicators.volume.VolumeIndicator;
import org.ta4j.core.num.DecimalNum;
import org.ta4j.core.num.Num;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j // Added
public class TechnicalIndicatorServiceImpl implements TechnicalIndicatorService {

    // private static final Logger logger = LoggerFactory.getLogger(TechnicalIndicatorServiceImpl.class); // Removed

    private BarSeries convertToBarSeries(List<Candlestick> candlesticks, String seriesName) {
        if (candlesticks == null || candlesticks.isEmpty()) {
            return new BaseBarSeriesBuilder().withName(seriesName).build();
        }

        List<Bar> bars = candlesticks.stream()
                .map(candle -> {
                    try {
                        ZonedDateTime closeTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(candle.getCloseTime()), ZoneId.systemDefault());
                        // TA4J expects the duration of the bar. If openTime is available and represents the start of the period:
                        // Duration duration = Duration.between(
                        // ZonedDateTime.ofInstant(Instant.ofEpochMilli(candle.getOpenTime()), ZoneId.systemDefault()),
                        // closeTime
                        // );
                        // If not, and we only have close time, we might need to infer duration based on interval,
                        // or use a default if intervals are consistent. For now, let's assume a fixed duration (e.g., 1 minute)
                        // if openTime is not used to derive it.
                        // However, kline data usually implies the duration from the interval between open times.
                        // For simplicity in TA4J bar creation if duration is tricky: use endTime and a common period.
                        // Let's assume candlesticks are ordered and represent a consistent interval.
                        // The addBar method used below (the one with 6 args) does not require duration but end time.

                        return BaseBar.builder(DecimalNum::valueOf, String.class)
                                .timePeriod(closeTime) // TA4J uses end time of the period for ZonedDateTime
                                .openPrice(candle.getOpenPrice())
                                .highPrice(candle.getHighPrice())
                                .lowPrice(candle.getLowPrice())
                                .closePrice(candle.getClosePrice())
                                .volume(candle.getVolume())
                                .build();
                    } catch (Exception e) {
                        log.error("Error converting candlestick to bar: {}. Candlestick: {}", e.getMessage(), candle, e); // logger to log
                        return null;
                    }
                })
                .filter(bar -> bar != null)
                .collect(Collectors.toList());

        BarSeries barSeries = new BaseBarSeriesBuilder().withName(seriesName).withBars(bars).build();
        // barSeries.setMaximumBarCount(Integer.MAX_VALUE); // Optional: to keep all bars, default is often limited
        return barSeries;
    }


    @Override
    public Num calculateVWAP(List<Candlestick> candlesticks, int period) {
        if (candlesticks == null || candlesticks.size() < period) {
            log.warn("Not enough candlesticks to calculate VWAP for period {}. Need {}, got {}.", period, period, candlesticks == null ? 0 : candlesticks.size()); // logger to log
            return null;
        }
        BarSeries series = convertToBarSeries(candlesticks, "VWAP_Series");
        if (series.isEmpty()) return null;
        VWAPIndicator vwap = new VWAPIndicator(series, period);
        return vwap.getValue(series.getEndIndex());
    }

    @Override
    public Num calculateRSI(List<Candlestick> candlesticks, int period) {
        if (candlesticks == null || candlesticks.size() < period) {
            log.warn("Not enough candlesticks to calculate RSI for period {}. Need {}, got {}.", period, period, candlesticks == null ? 0 : candlesticks.size()); // logger to log
            return null;
        }
        BarSeries series = convertToBarSeries(candlesticks, "RSI_Series");
        if (series.isEmpty()) return null;
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        RSIIndicator rsi = new RSIIndicator(closePrice, period);
        return rsi.getValue(series.getEndIndex());
    }

    @Override
    public Num calculateEMA(List<Candlestick> candlesticks, int period) {
        if (candlesticks == null || candlesticks.size() < period) {
            log.warn("Not enough candlesticks to calculate EMA for period {}. Need {}, got {}.", period, period, candlesticks == null ? 0 : candlesticks.size()); // logger to log
            return null;
        }
        BarSeries series = convertToBarSeries(candlesticks, "EMA_Series");
        if (series.isEmpty()) return null;
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        EMAIndicator ema = new EMAIndicator(closePrice, period);
        return ema.getValue(series.getEndIndex());
    }

    @Override
    public BollingerBandsValues calculateBollingerBands(List<Candlestick> candlesticks, int period, double stdDevMultiplier) {
        if (candlesticks == null || candlesticks.size() < period) {
             log.warn("Not enough candlesticks to calculate Bollinger Bands for period {}. Need {}, got {}.", period, period, candlesticks == null ? 0 : candlesticks.size()); // logger to log
            return null;
        }
        BarSeries series = convertToBarSeries(candlesticks, "BB_Series");
        if (series.isEmpty()) return null;
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        SMAIndicator sma = new SMAIndicator(closePrice, period); // Middle band
        StandardDeviationIndicator stdDev = new StandardDeviationIndicator(closePrice, period);

        BollingerBandsMiddleIndicator middleBand = new BollingerBandsMiddleIndicator(sma);
        BollingerBandsLowerIndicator lowerBand = new BollingerBandsLowerIndicator(middleBand, stdDev, DecimalNum.valueOf(stdDevMultiplier));
        BollingerBandsUpperIndicator upperBand = new BollingerBandsUpperIndicator(middleBand, stdDev, DecimalNum.valueOf(stdDevMultiplier));

        return new BollingerBandsValues(
                upperBand.getValue(series.getEndIndex()),
                middleBand.getValue(series.getEndIndex()),
                lowerBand.getValue(series.getEndIndex())
        );
    }

    @Override
    public Num calculateAverageVolume(List<Candlestick> candlesticks, int period) {
        if (candlesticks == null || candlesticks.size() < period) {
            log.warn("Not enough candlesticks to calculate Average Volume for period {}. Need {}, got {}.", period, period, candlesticks == null ? 0 : candlesticks.size()); // logger to log
            return null;
        }
        BarSeries series = convertToBarSeries(candlesticks, "AvgVol_Series");
        if (series.isEmpty()) return null;
        VolumeIndicator volume = new VolumeIndicator(series);
        SMAIndicator avgVolume = new SMAIndicator(volume, period);
        return avgVolume.getValue(series.getEndIndex());
    }
}

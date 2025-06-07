package com.jbqneto.dev.botdev.service;

import com.jbqneto.dev.botdev.dto.Candlestick;
import com.jbqneto.dev.botdev.dto.BollingerBandsValues; // Will create this DTO later
import org.ta4j.core.num.Num; // For return types

import java.util.List;

/**
 * Interface for calculating technical indicators.
 */
public interface TechnicalIndicatorService {

    /**
     * Calculates the Volume Weighted Average Price (VWAP).
     * @param candlesticks List of candlestick data.
     * @param period The period for VWAP calculation.
     * @return The VWAP value for the most recent bar, or null if not calculable.
     */
    Num calculateVWAP(List<Candlestick> candlesticks, int period);

    /**
     * Calculates the Relative Strength Index (RSI).
     * @param candlesticks List of candlestick data.
     * @param period The period for RSI calculation (e.g., 14).
     * @return The RSI value for the most recent bar, or null if not calculable.
     */
    Num calculateRSI(List<Candlestick> candlesticks, int period);

    /**
     * Calculates the Exponential Moving Average (EMA).
     * @param candlesticks List of candlestick data.
     * @param period The period for EMA calculation (e.g., 9, 21).
     * @return The EMA value for the most recent bar, or null if not calculable.
     */
    Num calculateEMA(List<Candlestick> candlesticks, int period);

    /**
     * Calculates Bollinger Bands.
     * @param candlesticks List of candlestick data.
     * @param period The period for Bollinger Bands calculation (e.g., 20).
     * @param stdDevMultiplier The standard deviation multiplier (e.g., 2).
     * @return BollingerBandsValues DTO containing upper, middle, and lower band values for the most recent bar, or null.
     */
    BollingerBandsValues calculateBollingerBands(List<Candlestick> candlesticks, int period, double stdDevMultiplier);

    /**
     * Calculates the average trading volume.
     * @param candlesticks List of candlestick data.
     * @param period The period for calculating average volume (e.g., 10).
     * @return The average volume for the most recent bar, or null if not calculable.
     */
    Num calculateAverageVolume(List<Candlestick> candlesticks, int period);
}

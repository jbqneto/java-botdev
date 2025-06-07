package com.jbqneto.dev.botdev.strategy;

import com.jbqneto.dev.botdev.dto.TradingDecision;

/**
 * Interface for defining trading strategies.
 */
public interface TradingStrategy {

    /**
     * Generates a trading signal based on market data and strategy rules.
     *
     * @param symbol The trading symbol (e.g., "BTCUSDT").
     * @param timeframe The primary timeframe for the strategy (e.g., "15m").
     *                  The strategy might use other timeframes for confirmation.
     * @return A TradingDecision object containing the signal and relevant information.
     */
    TradingDecision generateSignal(String symbol, String timeframe);
}

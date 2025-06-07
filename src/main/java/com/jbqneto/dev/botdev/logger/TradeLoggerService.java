package com.jbqneto.dev.botdev.logger;

import com.jbqneto.dev.botdev.dto.NewOrderResponseDto;
import com.jbqneto.dev.botdev.dto.TradingDecision;

/**
 * Interface for logging trading activities.
 */
public interface TradeLoggerService {

    /**
     * Logs the details of a trading decision and its execution outcome.
     *
     * @param decision The trading decision made (signal, symbol, intended price).
     * @param orderResponse The response from the exchange after attempting to place the order.
     *                      This can be null if the order placement failed before sending to exchange.
     * @param notes Additional notes, which could include reasons for failure if orderResponse is null,
     *              or other contextual information.
     */
    void logTrade(TradingDecision decision, NewOrderResponseDto orderResponse, String notes);
}

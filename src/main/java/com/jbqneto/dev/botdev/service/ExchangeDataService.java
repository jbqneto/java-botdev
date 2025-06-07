package com.jbqneto.dev.botdev.service;

import com.binance.connector.client.impl.spot.Market; // Assuming this is the right import for Market data later
import com.jbqneto.dev.botdev.dto.Candlestick; // Will create this DTO later

import java.util.List;

/**
 * Interface for fetching data from a generic exchange.
 */
public interface ExchangeDataService {

    /**
     * Fetches candlestick bars for a given symbol and interval.
     *
     * @param symbol the trading symbol (e.g., BTCUSDT)
     * @param interval the interval for candlesticks (e.g., 15m, 1h, 1d)
     * @param limit the number of candlesticks to retrieve (max 1000)
     * @return a list of Candlestick objects
     */
    List<Candlestick> getCandlestickBars(String symbol, String interval, Integer limit);

}

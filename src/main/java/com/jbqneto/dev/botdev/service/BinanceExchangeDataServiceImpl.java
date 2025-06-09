package com.jbqneto.dev.botdev.service;

import com.binance.connector.client.SpotClient;
import com.binance.connector.client.impl.SpotClientImpl;
import com.binance.connector.client.impl.SpotClientImpl;
import com.jbqneto.dev.botdev.config.BinanceConfig;
import com.jbqneto.dev.botdev.dto.Candlestick;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j; // Added for @Slf4j

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class BinanceExchangeDataServiceImpl implements ExchangeDataService {

    private final SpotClient spotClient;
    private final BinanceConfig binanceConfig;

    @Autowired
    public BinanceDataServiceImpl(BinanceConfig binanceConfig) {
        this.binanceConfig = binanceConfig;
        if (binanceConfig.getApiKey() != null && !binanceConfig.getApiKey().equals("YOUR_BINANCE_API_KEY") &&
            binanceConfig.getApiSecret() != null && !binanceConfig.getApiSecret().equals("YOUR_BINANCE_API_SECRET")) {
            this.spotClient = new SpotClientImpl(binanceConfig.getApiKey(), binanceConfig.getApiSecret());
        } else {
            log.warn("Binance API key/secret not configured or using placeholder values. Binance client will use no-args constructor (limited functionality)."); // logger to log
            this.spotClient = new SpotClientImpl();
        }
    }

    @Override
    public List<Candlestick> getCandlestickBars(String symbol, String interval, Integer limit) {
        // spotClient is guaranteed to be non-null due to constructor logic
        com.binance.connector.client.impl.spot.Market market = spotClient.createMarket();
        LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("symbol", symbol);
        parameters.put("interval", interval);
        if (limit != null) {
            parameters.put("limit", limit);
        }

        try {
            String result = market.klines(parameters);
            // The result is a JSON string representing List<List<Object>>
            // We need to parse this. The Binance connector library might not directly return a typed list of objects.
            // For simplicity, this example assumes the raw string needs parsing or that a more direct method exists.
            // Let's assume the actual library call for klines (or a similar method) might return a more structured format
            // or that we'd use a JSON parsing library here.
            // The raw response from Binance API is List<List<Object>>
            // where each inner list represents a candlestick:
            // [
            //   [
            //     1499040000000,      // Open time
            //     "0.01634790",       // Open
            //     "0.80000000",       // High
            //     "0.01575800",       // Low
            //     "0.01577100",       // Close
            //     "148976.11427815",  // Volume
            //     1499644799999,      // Close time
            //     "2434.19055334",    // Quote asset volume
            //     308,                // Number of trades
            //     "1756.87402397",    // Taker buy base asset volume
            //     "28.46694368",      // Taker buy quote asset volume
            //     "17928899.62484339" // Ignore
            //   ]
            // ]
            // This part requires a proper JSON parser (like Jackson or Gson) if SpotClient doesn't return a typed list.
            // The `market.klines(parameters)` from `binance-connector-java` returns a String.
            // We need to parse this string.

            // For now, let's simulate a parsed list.
            // In a real scenario, use a JSON library (e.g., Jackson) to parse `result`
            // ObjectMapper objectMapper = new ObjectMapper();
            // List<List<Object>> klineData = objectMapper.readValue(result, new TypeReference<List<List<Object>>>(){});

            // Let's return an empty list as parsing is complex without adding a new JSON dependency right now.
            // And it's better to handle this parsing carefully.
            logger.info("Fetched klines string for {}: {}", symbol, result.substring(0, Math.min(result.length(), 200)) + "..."); // Log first 200 chars

            // Placeholder for actual parsing - this is a complex part.
            // The binance-connector-java klines method returns a String, which is a JSON array of arrays.
            // The result is a JSON string representing List<List<Object>>
            log.debug("Fetched klines string for {}: {}", symbol, result.substring(0, Math.min(result.length(), 500)) + "..."); // logger to log

            ObjectMapper objectMapper = new ObjectMapper();
            List<List<Object>> klineData = objectMapper.readValue(result, new TypeReference<List<List<Object>>>(){});

            return klineData.stream()
                            .map(klineEntry -> mapKlineEntryToCandlestick(klineEntry, symbol))
                            .filter(candlestick -> candlestick != null)
                            .collect(Collectors.toList());

        } catch (Exception e) { // Catches JsonProcessingException from objectMapper and other exceptions
            log.error("Error fetching or parsing candlestick data for symbol {}: {}", symbol, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    private Candlestick mapKlineEntryToCandlestick(List<Object> klineEntry, String symbolForLogging) {
        try {
            return new Candlestick(
                ((Number) klineEntry.get(0)).longValue(),    // Open time
                klineEntry.get(1).toString(),                // Open
                klineEntry.get(2).toString(),                // High
                klineEntry.get(3).toString(),                // Low
                klineEntry.get(4).toString(),                // Close
                klineEntry.get(5).toString(),                // Volume
                ((Number) klineEntry.get(6)).longValue(),    // Close time
                klineEntry.get(7).toString(),                // Quote asset volume
                ((Number) klineEntry.get(8)).intValue(),     // Number of trades
                klineEntry.get(9).toString(),                // Taker buy base asset volume
                klineEntry.get(10).toString()                // Taker buy quote asset volume
                // klineEntry.get(11) is "Ignore."
            );
        } catch (Exception e) {
            log.error("Error parsing individual kline entry for symbol {}: {}. Entry: {}", symbolForLogging, e.getMessage(), klineEntry, e);
            return null;
        }
    }
}

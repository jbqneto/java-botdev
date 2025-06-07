package com.jbqneto.dev.botdev.service;

import com.binance.connector.client.SpotClient;
import com.binance.connector.client.impl.SpotClientImpl;
import com.binance.connector.client.impl.spot.Trade;
import com.jbqneto.dev.botdev.config.BinanceConfig;
import com.jbqneto.dev.botdev.dto.NewOrderResponseDto;
import com.fasterxml.jackson.databind.ObjectMapper; // Added
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;

@Service
public class OrderServiceImpl implements OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderServiceImpl.class);

    private final SpotClient spotClient;
    private final BinanceConfig binanceConfig; // Changed from TradingConfig

    @Autowired
    public OrderServiceImpl(BinanceConfig binanceConfig) { // Changed from TradingConfig
        this.binanceConfig = binanceConfig;

        if (binanceConfig.getApiKey() != null && !binanceConfig.getApiKey().equals("YOUR_BINANCE_API_KEY") &&
            binanceConfig.getApiSecret() != null && !binanceConfig.getApiSecret().equals("YOUR_BINANCE_API_SECRET")) {
            this.spotClient = new SpotClientImpl(binanceConfig.getApiKey(), binanceConfig.getApiSecret());
            logger.info("OrderService: SpotClient initialized with API key and secret.");
        } else {
            logger.warn("OrderService: Binance API key/secret not configured or using placeholder values. SpotClient initialized without credentials.");
            // This client can only access public endpoints. Order placement will fail.
            this.spotClient = new SpotClientImpl();
        }
    }

    @Override
    public NewOrderResponseDto placeNewOrder(String symbol, String side, String type, String timeInForce, Double quantity, Double price) {
        // spotClient is guaranteed to be non-null due to constructor logic

        // Check if client was initialized with placeholder keys and trying to place an order
        if (binanceConfig.getApiKey() == null || "YOUR_BINANCE_API_KEY".equals(binanceConfig.getApiKey())) {
            logger.error("Cannot place order with placeholder or missing API keys. Please configure actual API keys.");
            NewOrderResponseDto errorResponse = new NewOrderResponseDto();
            errorResponse.setSymbol(symbol);
            errorResponse.setStatus("ERROR_PLACEHOLDER_KEYS");
            return errorResponse;
        }

        Trade trade = spotClient.createTrade();
        LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("symbol", symbol.toUpperCase());
        parameters.put("side", side.toUpperCase());
        parameters.put("type", type.toUpperCase());
        parameters.put("newOrderRespType", "RESULT"); // ACK, RESULT, or FULL. RESULT gives more info than ACK.

        if (quantity == null || quantity <= 0) {
            logger.error("Order quantity must be positive: {}", quantity);
            return null; // Or throw IllegalArgumentException
        }
        parameters.put("quantity", quantity.toString());


        if ("LIMIT".equalsIgnoreCase(type)) {
            if (price == null || price <= 0) {
                logger.error("Price must be positive for LIMIT orders: {}", price);
                return null; // Or throw IllegalArgumentException
            }
            parameters.put("price", price.toString());
            if (timeInForce == null || timeInForce.isEmpty()) {
                parameters.put("timeInForce", "GTC"); // Default GTC for LIMIT if not specified
            } else {
                parameters.put("timeInForce", timeInForce.toUpperCase());
            }
        } else if ("MARKET".equalsIgnoreCase(type)) {
            // Price not needed for MARKET order. timeInForce is not applicable.
            parameters.remove("price");
            parameters.remove("timeInForce");
        } else {
            logger.error("Unsupported order type: {}", type);
            return null; // Or throw
        }

        NewOrderResponseDto responseDto = new NewOrderResponseDto();
        try {
            logger.info("Placing new order with parameters: {}", parameters);
            String rawResponse = trade.newOrder(parameters);
            logger.info("Raw order response for {}: {}", symbol, rawResponse);

            ObjectMapper objectMapper = new ObjectMapper();
            // Configure ObjectMapper to ignore unknown properties if Binance adds new fields
            // objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false); // Done at DTO level now

            responseDto = objectMapper.readValue(rawResponse, NewOrderResponseDto.class);
            // Ensure essential fields from the request are part of the DTO if not in every response type
            if (responseDto.getSymbol() == null) responseDto.setSymbol(symbol);
            if (responseDto.getSide() == null) responseDto.setSide(side.toUpperCase());
            if (responseDto.getType() == null) responseDto.setType(type.toUpperCase());
            if (responseDto.getOrigQty() == null && quantity != null) responseDto.setOrigQty(quantity.toString());

            // Check if status is null (can happen for ACK response type, though we use RESULT)
            // and fill with a placeholder if needed, or rely on actual API response.
            if (responseDto.getStatus() == null) {
                 responseDto.setStatus("UNKNOWN_FROM_API"); // Should ideally come from API
            }
            logger.info("Parsed order response for {}: {}", symbol, responseDto);

        } catch (Exception e) {
            logger.error("Error placing or parsing order response for symbol {}: {}", symbol, e.getMessage(), e);
            // If it's a BinanceClientException, it might contain a JSON error response
            // Example: com.binance.connector.client.exceptions.BinanceClientException
            // For now, a general catch.
            if (responseDto == null) responseDto = new NewOrderResponseDto(); // Ensure responseDto is not null
            responseDto.setSymbol(symbol);
            responseDto.setStatus("ERROR_API");
            responseDto.setReason(e.getMessage());
            return responseDto; // Return DTO with error status
        }
        return responseDto;
    }
}

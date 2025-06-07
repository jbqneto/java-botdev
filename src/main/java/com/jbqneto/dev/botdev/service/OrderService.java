package com.jbqneto.dev.botdev.service;

import com.jbqneto.dev.botdev.dto.NewOrderResponseDto;
// import com.jbqneto.dev.botdev.dto.CancelOrderResponseDto; // Future
// import com.jbqneto.dev.botdev.dto.OrderStatusDto; // Future

/**
 * Interface for placing and managing trading orders on Binance.
 */
public interface OrderService {

    /**
     * Places a new order on Binance.
     *
     * @param symbol The trading symbol (e.g., "BTCUSDT").
     * @param side The order side ("BUY" or "SELL").
     * @param type The order type ("LIMIT", "MARKET", etc.).
     * @param timeInForce The time in force for the order (e.g., "GTC" for LIMIT orders). Can be null for MARKET orders.
     * @param quantity The quantity of the asset to trade.
     * @param price The price for the order (required for LIMIT orders). Can be null for MARKET orders.
     * @return A NewOrderResponseDto containing details of the placed order, or null if order placement failed.
     */
    NewOrderResponseDto placeNewOrder(String symbol, String side, String type, String timeInForce, Double quantity, Double price);

    // Future methods:
    // CancelOrderResponseDto cancelOrder(String symbol, Long orderId);
    // OrderStatusDto getOrderStatus(String symbol, Long orderId);
}

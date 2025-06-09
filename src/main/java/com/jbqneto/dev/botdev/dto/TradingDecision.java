package com.jbqneto.dev.botdev.dto;

import com.jbqneto.dev.botdev.strategy.TradeSignal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.ta4j.core.num.Num;

@Data
@NoArgsConstructor
// @AllArgsConstructor // We have custom constructor logic for timestamp
public class TradingDecision {
    private TradeSignal signal;
    private String symbol;
    private Num price; // Entry or Exit price
    private String reason; // Reason for the signal, e.g., "RSI overbought"
    private long timestamp;

    public TradingDecision(TradeSignal signal, String symbol, Num price, String reason) {
        this.signal = signal;
        this.symbol = symbol;
        this.price = price;
        this.reason = reason;
        this.timestamp = System.currentTimeMillis(); // Custom timestamp logic
    }

    public TradingDecision(TradeSignal signal, String symbol) {
        this(signal, symbol, null, null);
        // Timestamp will be set by the above constructor call
    }

    public TradingDecision(TradeSignal signal) {
        this(signal, null, null, null);
        // Timestamp will be set by the above constructor call
    }

    // Note: Custom constructors are kept to auto-initialize 'timestamp'.
    // Lombok's @Data will provide getters, setters, equals, hashCode, toString.
    // It will not generate an @AllArgsConstructor because other constructors exist.
}

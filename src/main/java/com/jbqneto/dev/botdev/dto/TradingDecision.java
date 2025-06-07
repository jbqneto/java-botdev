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

    // Lombok's @Data will generate:
    // - getters for all fields
    // - setters for all fields
    // - equals()
    // - hashCode()
    // - toString()
    // - a constructor for all final fields (if any)
    // Since we have custom constructor logic for the timestamp,
    // we might not want Lombok's @AllArgsConstructor if it conflicts or is unused.
    // If we want an all-args constructor that *includes* timestamp for manual setting,
    // then we can add @AllArgsConstructor and remove the custom ones, or ensure signatures differ.
    // For now, keeping the custom constructors that auto-set timestamp.
    // If an @AllArgsConstructor is added by Lombok due to @Data and no other constructor,
    // it might not set the timestamp automatically.
    // The current custom constructors are fine. @Data will not generate an AllArgsConstructor if other constructors are present.
}

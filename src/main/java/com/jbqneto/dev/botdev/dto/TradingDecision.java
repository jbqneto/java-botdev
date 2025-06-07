package com.jbqneto.dev.botdev.dto;

import com.jbqneto.dev.botdev.strategy.TradeSignal;
import org.ta4j.core.num.Num;

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
        this.timestamp = System.currentTimeMillis();
    }

    public TradingDecision(TradeSignal signal, String symbol) {
        this(signal, symbol, null, null);
    }

    public TradingDecision(TradeSignal signal) {
        this(signal, null, null, null);
    }


    // Getters and Setters
    public TradeSignal getSignal() {
        return signal;
    }

    public void setSignal(TradeSignal signal) {
        this.signal = signal;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public Num getPrice() {
        return price;
    }

    public void setPrice(Num price) {
        this.price = price;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "TradingDecision{" +
                "signal=" + signal +
                ", symbol='" + symbol + '\'' +
                ", price=" + (price != null ? price.doubleValue() : "N/A") +
                ", reason='" + reason + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}

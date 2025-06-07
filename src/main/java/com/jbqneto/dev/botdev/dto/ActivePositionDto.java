package com.jbqneto.dev.botdev.dto;

import org.ta4j.core.num.Num;

public class ActivePositionDto {
    private String symbol;
    private Num entryPrice;
    private Num quantity;
    private PositionSide side; // LONG or SHORT
    private Num entryCandleLow; // Low of the candle on which entry occurred
    private Num entryCandleHigh; // High of the candle on which entry occurred
    private long entryTimestamp;

    public enum PositionSide {
        LONG, SHORT
    }

    public ActivePositionDto(String symbol, Num entryPrice, Num quantity, PositionSide side, Num entryCandleLow, Num entryCandleHigh) {
        this.symbol = symbol;
        this.entryPrice = entryPrice;
        this.quantity = quantity;
        this.side = side;
        this.entryCandleLow = entryCandleLow;
        this.entryCandleHigh = entryCandleHigh;
        this.entryTimestamp = System.currentTimeMillis();
    }

    // Getters
    public String getSymbol() {
        return symbol;
    }

    public Num getEntryPrice() {
        return entryPrice;
    }

    public Num getQuantity() {
        return quantity;
    }

    public PositionSide getSide() {
        return side;
    }

    public Num getEntryCandleLow() {
        return entryCandleLow;
    }

    public Num getEntryCandleHigh() {
        return entryCandleHigh;
    }

    public long getEntryTimestamp() {
        return entryTimestamp;
    }

    // Setters could be added if needed for updates, but typically positions are replaced or removed.
    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public void setEntryPrice(Num entryPrice) {
        this.entryPrice = entryPrice;
    }

    public void setQuantity(Num quantity) {
        this.quantity = quantity;
    }

    public void setSide(PositionSide side) {
        this.side = side;
    }

    public void setEntryCandleLow(Num entryCandleLow) {
        this.entryCandleLow = entryCandleLow;
    }

    public void setEntryCandleHigh(Num entryCandleHigh) {
        this.entryCandleHigh = entryCandleHigh;
    }

    public void setEntryTimestamp(long entryTimestamp) {
        this.entryTimestamp = entryTimestamp;
    }

    @Override
    public String toString() {
        return "ActivePositionDto{" +
                "symbol='" + symbol + '\'' +
                ", entryPrice=" + entryPrice +
                ", quantity=" + quantity +
                ", side=" + side +
                ", entryCandleLow=" + entryCandleLow +
                ", entryCandleHigh=" + entryCandleHigh +
                ", entryTimestamp=" + entryTimestamp +
                '}';
    }
}

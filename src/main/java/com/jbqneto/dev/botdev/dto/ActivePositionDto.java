package com.jbqneto.dev.botdev.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.ta4j.core.num.Num;

@Data
@NoArgsConstructor
@AllArgsConstructor
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

    // Note: The custom constructor that set entryTimestamp = System.currentTimeMillis() was removed
    // in favor of Lombok's @AllArgsConstructor. The timestamp should be explicitly passed or set by the caller.
}

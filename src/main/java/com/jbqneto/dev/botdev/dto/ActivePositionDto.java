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

    // Manual constructor to set timestamp, if needed, or rely on @AllArgsConstructor and set it post-creation
    // For simplicity with @AllArgsConstructor, we might need to adjust how timestamp is set if it's always system-generated.
    // Let's assume @AllArgsConstructor will cover all fields, and timestamp can be set if needed,
    // or the DTO is created and then timestamp is set if it's purely for "moment of creation in memory".
    // The previous constructor set it to System.currentTimeMillis().
    // If that behavior is desired with an all-args constructor that doesn't include it:
    public ActivePositionDto(String symbol, Num entryPrice, Num quantity, PositionSide side, Num entryCandleLow, Num entryCandleHigh) {
        this.symbol = symbol;
        this.entryPrice = entryPrice;
        this.quantity = quantity;
        this.side = side;
        this.entryCandleLow = entryCandleLow;
        this.entryCandleHigh = entryCandleHigh;
        this.entryTimestamp = System.currentTimeMillis(); // Keep this specific logic
    }
    // Lombok's @AllArgsConstructor will generate one for all fields including entryTimestamp.
    // If we want the System.currentTimeMillis() logic, this manual constructor is better.
    // To use Lombok and this logic, we'd need a @NoArgsConstructor and then manual setting, or a builder with a default value for timestamp.
    // For now, I'll keep this manual constructor for the specific timestamp logic and remove other boilerplate.
    // Lombok will add other constructors if needed (@NoArgsConstructor, and potentially an @AllArgsConstructor for all fields if this one is removed or has different signature).
    // To keep things simple and let Lombok do most work, I'll remove this manual constructor and assume timestamp is passed or set.
    // If System.currentTimeMillis() is always desired on creation, it's often better to set it in the service layer creating the DTO.
    // For now, let's assume entryTimestamp is a field to be filled by the creator, so @AllArgsConstructor is fine.
}

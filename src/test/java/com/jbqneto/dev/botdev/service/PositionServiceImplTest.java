package com.jbqneto.dev.botdev.service;

import com.jbqneto.dev.botdev.dto.ActivePositionDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ta4j.core.num.DecimalNum;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class PositionServiceImplTest {

    private PositionServiceImpl positionService;

    @BeforeEach
    void setUp() {
        positionService = new PositionServiceImpl();
    }

    @Test
    void updateAndGetPosition_shouldStoreAndRetrievePosition() {
        ActivePositionDto position = new ActivePositionDto(
                "BTCUSDT", DecimalNum.valueOf("30000"), DecimalNum.valueOf("0.01"),
                ActivePositionDto.PositionSide.LONG, DecimalNum.valueOf("29900"), DecimalNum.valueOf("30100")
        );
        positionService.updatePosition(position);

        Optional<ActivePositionDto> retrievedPositionOpt = positionService.getPosition("BTCUSDT");
        assertTrue(retrievedPositionOpt.isPresent());
        ActivePositionDto retrievedPosition = retrievedPositionOpt.get();
        assertEquals("BTCUSDT", retrievedPosition.getSymbol());
        assertEquals(DecimalNum.valueOf("30000"), retrievedPosition.getEntryPrice());
    }

    @Test
    void updatePosition_withNullSymbol_shouldNotStore() {
        ActivePositionDto position = new ActivePositionDto(
                null, DecimalNum.valueOf("30000"), DecimalNum.valueOf("0.01"),
                ActivePositionDto.PositionSide.LONG, DecimalNum.valueOf("29900"), DecimalNum.valueOf("30100")
        );
        positionService.updatePosition(position);
        assertTrue(positionService.getAllActiveSymbols().isEmpty());
    }

    @Test
    void updatePosition_withNullPosition_shouldNotStore() {
        positionService.updatePosition(null);
        assertTrue(positionService.getAllActiveSymbols().isEmpty());
    }


    @Test
    void removePosition_shouldRemoveExistingPosition() {
        ActivePositionDto position = new ActivePositionDto(
                "BTCUSDT", DecimalNum.valueOf("30000"), DecimalNum.valueOf("0.01"),
                ActivePositionDto.PositionSide.LONG, DecimalNum.valueOf("29900"), DecimalNum.valueOf("30100")
        );
        positionService.updatePosition(position);
        assertTrue(positionService.getPosition("BTCUSDT").isPresent());

        positionService.removePosition("BTCUSDT");
        assertFalse(positionService.getPosition("BTCUSDT").isPresent());
    }

    @Test
    void removePosition_nonExistingSymbol_shouldDoNothing() {
        positionService.removePosition("ETHUSDT"); // No error should occur
        assertFalse(positionService.getPosition("ETHUSDT").isPresent());
    }

    @Test
    void removePosition_nullOrEmptySymbol_shouldDoNothing() {
        ActivePositionDto position = new ActivePositionDto(
                "BTCUSDT", DecimalNum.valueOf("30000"), DecimalNum.valueOf("0.01"),
                ActivePositionDto.PositionSide.LONG, DecimalNum.valueOf("29900"), DecimalNum.valueOf("30100")
        );
        positionService.updatePosition(position);

        positionService.removePosition(null);
        positionService.removePosition("");

        assertTrue(positionService.hasActivePosition("BTCUSDT")); // Original position should still be there
    }

    @Test
    void getAllActiveSymbols_shouldReturnCorrectSymbols() {
        ActivePositionDto btcPos = new ActivePositionDto(
                "BTCUSDT", DecimalNum.valueOf("30000"), DecimalNum.valueOf("0.01"),
                ActivePositionDto.PositionSide.LONG, DecimalNum.valueOf("29900"), DecimalNum.valueOf("30100")
        );
        ActivePositionDto ethPos = new ActivePositionDto(
                "ETHUSDT", DecimalNum.valueOf("2000"), DecimalNum.valueOf("0.1"),
                ActivePositionDto.PositionSide.SHORT, DecimalNum.valueOf("1990"), DecimalNum.valueOf("2010")
        );
        positionService.updatePosition(btcPos);
        positionService.updatePosition(ethPos);

        Set<String> activeSymbols = positionService.getAllActiveSymbols();
        assertEquals(2, activeSymbols.size());
        assertTrue(activeSymbols.contains("BTCUSDT"));
        assertTrue(activeSymbols.contains("ETHUSDT"));
    }

    @Test
    void hasActivePosition_shouldReturnCorrectly() {
        assertFalse(positionService.hasActivePosition("BTCUSDT"));
        ActivePositionDto position = new ActivePositionDto(
                "BTCUSDT", DecimalNum.valueOf("30000"), DecimalNum.valueOf("0.01"),
                ActivePositionDto.PositionSide.LONG, DecimalNum.valueOf("29900"), DecimalNum.valueOf("30100")
        );
        positionService.updatePosition(position);
        assertTrue(positionService.hasActivePosition("BTCUSDT"));
        assertFalse(positionService.hasActivePosition("ETHUSDT"));
    }
}

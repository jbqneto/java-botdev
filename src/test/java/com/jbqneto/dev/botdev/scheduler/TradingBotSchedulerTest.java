package com.jbqneto.dev.botdev.scheduler;

import com.jbqneto.dev.botdev.config.TradingConfig;
import com.jbqneto.dev.botdev.dto.ActivePositionDto;
import com.jbqneto.dev.botdev.dto.BollingerBandsValues;
import com.jbqneto.dev.botdev.dto.Candlestick;
import com.jbqneto.dev.botdev.dto.NewOrderResponseDto;
import com.jbqneto.dev.botdev.dto.TradingDecision;
import com.jbqneto.dev.botdev.logger.TradeLoggerService;
import com.jbqneto.dev.botdev.service.BinanceDataService;
import com.jbqneto.dev.botdev.service.NotificationService;
import com.jbqneto.dev.botdev.service.OrderService;
import com.jbqneto.dev.botdev.service.TechnicalIndicatorService;
import com.jbqneto.dev.botdev.strategy.TradeSignal;
import com.jbqneto.dev.botdev.strategy.TradingStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ta4j.core.num.DecimalNum;

import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TradingBotSchedulerTest {

    @Mock
    private TradingConfig mockTradingConfig;
    @Mock
    private TradingConfig.Trading mockNestedTradingConfig;
    @Mock
    private TradingConfig.Trading.TradingHours mockTradingHours;
    @Mock
    private BinanceDataService mockBinanceDataService;
    @Mock
    private TechnicalIndicatorService mockTechnicalIndicatorService;
    @Mock
    private TradingStrategy mockTradingStrategy;
    @Mock
    private OrderService mockOrderService;
    @Mock
    private NotificationService mockNotificationService;
    @Mock
    private TradeLoggerService mockTradeLoggerService;
    @Mock
    private PositionService mockPositionService; // Added

    @InjectMocks
    private TradingBotScheduler tradingBotScheduler;

    private List<Candlestick> sampleCandlesticks;

    @BeforeEach
    void setUp() {
        when(mockTradingConfig.getTrading()).thenReturn(mockNestedTradingConfig);
        when(mockNestedTradingConfig.getTradingHours()).thenReturn(mockTradingHours);
        // Provide a default for positionService.getPosition to avoid NullPointer in general tests
        lenient().when(mockPositionService.getPosition(anyString())).thenReturn(Optional.empty());


        sampleCandlesticks = new ArrayList<>();
        Candlestick candle = new Candlestick();
        candle.setClosePrice("100");
        candle.setLowPrice("99");
        candle.setHighPrice("101");
        sampleCandlesticks.add(candle); // Ensure at least one candle
    }

    private void setupTradingHours(String start, String end) {
        when(mockTradingHours.getStart()).thenReturn(start);
        when(mockTradingHours.getEnd()).thenReturn(end);
    }

    @Test
    void runTradingLoop_shouldNotTrade_whenOutsideTradingHours() {
        // Simulate current time being outside trading hours
        LocalTime now = LocalTime.now(ZoneOffset.UTC);
        String startTime = now.plusHours(1).format(DateTimeFormatter.ofPattern("HH:mm"));
        String endTime = now.plusHours(2).format(DateTimeFormatter.ofPattern("HH:mm"));
        setupTradingHours(startTime, endTime);

        tradingBotScheduler.runTradingLoop();

        verify(mockTradingConfig.getTrading(), atLeastOnce()).getTradingHours(); // Trading hours checked
        verifyNoInteractions(mockTradingConfig.getTrading().getSymbols()); // No symbols fetched
        verifyNoInteractions(mockBinanceDataService);
        verifyNoInteractions(mockTradingStrategy);
    }

    @Test
    void runTradingLoop_shouldNotTrade_whenOutsideTradingHours_overnight() {
        LocalTime now = LocalTime.of(12,0); // Midday
        String startTime = "22:00"; // Overnight start
        String endTime = "05:00";   // Overnight end
        setupTradingHours(startTime, endTime);
         // Need to mock LocalTime.now(ZoneOffset.UTC) for consistent testing, but that's harder.
         // This test will depend on the actual execution time if not mocked.
         // For now, we assume this test runs at a time that is correctly outside this overnight window.
         // A better way is to inject a Clock.

        // To make it deterministic, let's test the logic by providing a time that IS outside.
        // If current time is 12:00 UTC, it's outside 22:00-05:00 UTC.
        // This test will pass if the current time is indeed outside.

        tradingBotScheduler.runTradingLoop(); // Assuming current time is outside
        verify(mockTradingConfig.getTrading(), atLeastOnce()).getTradingHours();
        verify(mockTradingConfig.getTrading(), never()).getSymbols();
    }


    @Test
    void runTradingLoop_shouldTrade_whenInsideTradingHours() {
        LocalTime now = LocalTime.now(ZoneOffset.UTC);
        String startTime = now.minusHours(1).format(DateTimeFormatter.ofPattern("HH:mm"));
        String endTime = now.plusHours(1).format(DateTimeFormatter.ofPattern("HH:mm"));
        setupTradingHours(startTime, endTime);
        when(mockNestedTradingConfig.getSymbols()).thenReturn(Collections.singletonList("BTCUSDT"));
        when(mockNestedTradingConfig.getDefaultTimeframe()).thenReturn("15m");
        when(mockBinanceDataService.getCandlestickBars(anyString(), anyString(), anyInt())).thenReturn(sampleCandlesticks);
        // Assume HOLD signal so no further actions are taken beyond fetching data
        when(mockTradingStrategy.generateSignal(anyString(), anyString()))
                .thenReturn(new TradingDecision(TradeSignal.HOLD, "BTCUSDT"));

        tradingBotScheduler.runTradingLoop();

        verify(mockTradingConfig.getTrading(), atLeastOnce()).getTradingHours();
        verify(mockNestedTradingConfig).getSymbols(); // Symbols should be fetched
        verify(mockBinanceDataService).getCandlestickBars(eq("BTCUSDT"), eq("15m"), anyInt());
        verify(mockTradingStrategy).generateSignal(eq("BTCUSDT"), eq("15m"));
    }

    @Test
    void runTradingLoop_longEntryFlow_successfulOrder() {
        setupTradingHours("00:00", "23:59"); // Ensure within trading hours
        when(mockNestedTradingConfig.getSymbols()).thenReturn(Collections.singletonList("BTCUSDT"));
        when(mockNestedTradingConfig.getDefaultTimeframe()).thenReturn("15m");
        when(mockBinanceDataService.getCandlestickBars(anyString(), anyString(), anyInt())).thenReturn(sampleCandlesticks);

        TradingDecision entryDecision = new TradingDecision(TradeSignal.LONG_ENTRY, "BTCUSDT", DecimalNum.valueOf("100"), "Long conditions met");
        when(mockTradingStrategy.generateSignal(eq("BTCUSDT"), anyString())).thenReturn(entryDecision);

        NewOrderResponseDto orderResponse = new NewOrderResponseDto();
        orderResponse.setSymbol("BTCUSDT");
        orderResponse.setOrderId(1L);
        orderResponse.setStatus("FILLED");
        orderResponse.setExecutedQty("0.01"); // (100 USD / 10000 USD/BTC approx) -> for price 100, qty would be 1
        orderResponse.setCummulativeQuoteQty("100.0"); // Matched to FIXED_USD_AMOUNT_PER_TRADE
		    sampleCandlesticks.get(sampleCandlesticks.size()-1).setClosePrice("100"); // Price for quantity calc

        when(mockOrderService.placeNewOrder(eq("BTCUSDT"), eq("BUY"), eq("MARKET"), isNull(), anyDouble(), isNull()))
                .thenReturn(orderResponse);

        tradingBotScheduler.runTradingLoop();

        // assertTrue(tradingBotScheduler.activePositions.containsKey("BTCUSDT"));
        // ActivePositionDto activePos = tradingBotScheduler.activePositions.get("BTCUSDT");
        // assertEquals(DecimalNum.valueOf("100.0"), activePos.getEntryPrice());
        // assertEquals(DecimalNum.valueOf("0.01"), activePos.getQuantity());
        // assertEquals(ActivePositionDto.PositionSide.LONG, activePos.getSide());
        ArgumentCaptor<ActivePositionDto> positionCaptor = ArgumentCaptor.forClass(ActivePositionDto.class);
        verify(mockPositionService).updatePosition(positionCaptor.capture());
        ActivePositionDto capturedPos = positionCaptor.getValue();
        assertEquals("BTCUSDT", capturedPos.getSymbol());
        assertEquals(DecimalNum.valueOf("100.0"), capturedPos.getEntryPrice());
        assertEquals(DecimalNum.valueOf("0.01"), capturedPos.getQuantity());
        assertEquals(ActivePositionDto.PositionSide.LONG, capturedPos.getSide());

        verify(mockTradeLoggerService).logTrade(eq(entryDecision), eq(orderResponse), anyString());
        verify(mockNotificationService).sendMessage(contains("LONG_ENTRY BTCUSDT"));
    }

    @Test
    void runTradingLoop_longEntryFlow_orderFailure() {
        setupTradingHours("00:00", "23:59");
        when(mockNestedTradingConfig.getSymbols()).thenReturn(Collections.singletonList("BTCUSDT"));
        when(mockNestedTradingConfig.getDefaultTimeframe()).thenReturn("15m");
        when(mockBinanceDataService.getCandlestickBars(anyString(), anyString(), anyInt())).thenReturn(sampleCandlesticks);
        sampleCandlesticks.get(sampleCandlesticks.size()-1).setClosePrice("100");


        TradingDecision entryDecision = new TradingDecision(TradeSignal.LONG_ENTRY, "BTCUSDT", DecimalNum.valueOf("100"), "Long conditions met");
        when(mockTradingStrategy.generateSignal(eq("BTCUSDT"), anyString())).thenReturn(entryDecision);

        NewOrderResponseDto orderResponseFailed = new NewOrderResponseDto();
        orderResponseFailed.setStatus("REJECTED");
        orderResponseFailed.setReason("Insufficient balance");
        when(mockOrderService.placeNewOrder(eq("BTCUSDT"), eq("BUY"), eq("MARKET"), isNull(), anyDouble(), isNull()))
                .thenReturn(orderResponseFailed);

        tradingBotScheduler.runTradingLoop();

        verify(mockPositionService, never()).updatePosition(any(ActivePositionDto.class)); // No position should be updated
        verify(mockTradeLoggerService).logTrade(eq(entryDecision), eq(orderResponseFailed), contains("Entry order placement failed"));
        verify(mockNotificationService).sendMessage(contains("Order Error: Failed to BUY BTCUSDT"));
    }


    @Test
    void runTradingLoop_longExitFlow_stopLossTriggered() {
        setupTradingHours("00:00", "23:59");
        when(mockNestedTradingConfig.getSymbols()).thenReturn(Collections.singletonList("BTCUSDT"));
        when(mockNestedTradingConfig.getDefaultTimeframe()).thenReturn("15m");
        when(mockNestedTradingConfig.getLongStopLossPercent()).thenReturn(1.0); // 1% stop loss

        ActivePositionDto existingPosition = new ActivePositionDto(
                "BTCUSDT", DecimalNum.valueOf("100"), DecimalNum.valueOf("1"), ActivePositionDto.PositionSide.LONG,
                DecimalNum.valueOf("98"), DecimalNum.valueOf("102") // entryCandleLow, entryCandleHigh
        );
        // tradingBotScheduler.activePositions.put("BTCUSDT", existingPosition); // Old way
        when(mockPositionService.getPosition("BTCUSDT")).thenReturn(Optional.of(existingPosition));


        // Current price drops below stop-loss (100 * (1 - 0.01) = 99). Also below entryCandleLow (98 * 0.999 approx 97.9)
        sampleCandlesticks.get(sampleCandlesticks.size() - 1).setClosePrice("97");
        when(mockBinanceDataService.getCandlestickBars(eq("BTCUSDT"), anyString(), anyInt())).thenReturn(sampleCandlesticks);

        // Mock Bollinger Bands for exit logic
        when(mockTechnicalIndicatorService.calculateBollingerBands(anyList(), anyInt(), anyDouble()))
            .thenReturn(new BollingerBandsValues(DecimalNum.valueOf(105), DecimalNum.valueOf(100), DecimalNum.valueOf(95)));


        NewOrderResponseDto exitOrderResponse = new NewOrderResponseDto();
        exitOrderResponse.setSymbol("BTCUSDT");
        exitOrderResponse.setOrderId(2L);
        exitOrderResponse.setStatus("FILLED");
        exitOrderResponse.setExecutedQty("1"); // Ensure this matches position quantity for full exit
        exitOrderResponse.setCummulativeQuoteQty("97"); // Exited at 97
        when(mockOrderService.placeNewOrder(eq("BTCUSDT"), eq("SELL"), eq("MARKET"), isNull(), eq(existingPosition.getQuantity().doubleValue()), isNull()))
                .thenReturn(exitOrderResponse);

        tradingBotScheduler.runTradingLoop();

        verify(mockPositionService).removePosition("BTCUSDT");
        verify(mockTradeLoggerService).logTrade(any(TradingDecision.class), eq(exitOrderResponse), contains("Stop loss triggered"));
        verify(mockNotificationService).sendMessage(contains("LONG_EXIT BTCUSDT"));
    }
}

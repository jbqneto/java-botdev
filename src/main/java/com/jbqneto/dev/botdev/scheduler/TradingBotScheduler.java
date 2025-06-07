package com.jbqneto.dev.botdev.scheduler;

import com.jbqneto.dev.botdev.config.StrategyConfig; // Changed
import com.jbqneto.dev.botdev.dto.ActivePositionDto;
import com.jbqneto.dev.botdev.dto.Candlestick;
import com.jbqneto.dev.botdev.dto.NewOrderResponseDto;
import com.jbqneto.dev.botdev.dto.TradingDecision;
import com.jbqneto.dev.botdev.logger.TradeLoggerService;
import com.jbqneto.dev.botdev.service.ExchangeDataService; // Changed
import com.jbqneto.dev.botdev.service.NotificationService;
import com.jbqneto.dev.botdev.service.OrderService;
import com.jbqneto.dev.botdev.service.TechnicalIndicatorService;
import com.jbqneto.dev.botdev.strategy.TradeSignal;
import com.jbqneto.dev.botdev.strategy.TradingStrategy;
import lombok.extern.slf4j.Slf4j; // Added
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.ta4j.core.num.DecimalNum;
import org.ta4j.core.num.Num;

import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@EnableScheduling
@Slf4j // Added
public class TradingBotScheduler {

    // private static final Logger logger = LoggerFactory.getLogger(TradingBotScheduler.class); // Removed

    private final StrategyConfig strategyConfig;
    private final ExchangeDataService exchangeDataService; // Changed
    private final TechnicalIndicatorService technicalIndicatorService;
    private final TradingStrategy tradingStrategy;
    private final OrderService orderService;
    private final NotificationService notificationService;
    private final TradeLoggerService tradeLoggerService;
    private final PositionService positionService; // Added

    // private final Map<String, ActivePositionDto> activePositions = new ConcurrentHashMap<>(); // Removed
    private static final DateTimeFormatter HH_MM_FORMATTER = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneOffset.UTC);

    @Autowired
    public TradingBotScheduler(StrategyConfig strategyConfig,
                               ExchangeDataService exchangeDataService,
                               TechnicalIndicatorService technicalIndicatorService,
                               TradingStrategy tradingStrategy,
                               OrderService orderService,
                               NotificationService notificationService,
                               TradeLoggerService tradeLoggerService,
                               PositionService positionService) { // Added
        this.strategyConfig = strategyConfig;
        this.exchangeDataService = exchangeDataService;
        this.technicalIndicatorService = technicalIndicatorService;
        this.tradingStrategy = tradingStrategy;
        this.orderService = orderService;
        this.notificationService = notificationService;
        this.tradeLoggerService = tradeLoggerService;
        this.positionService = positionService; // Added
    }

    // Example: Run every 15 minutes at the start of the minute.
    // Adjust cron as needed, e.g., "0 0/15 * * * ?" for 0, 15, 30, 45 minutes past the hour.
    // Or "1 0/15 * * * ?" to run at 1 minute past 0, 15, 30, 45 to ensure candle data is typically complete.
    @Scheduled(cron = "${strategy.scheduler.cron:0 0/15 * * * ?}")
    public void runTradingLoop() {
        log.info("Starting trading loop..."); // logger to log

        // Trading Hours Check
        try {
            String startTimeStr = strategyConfig.getTradingHours().getStart();
            String endTimeStr = strategyConfig.getTradingHours().getEnd();
            LocalTime startTime = LocalTime.parse(startTimeStr, HH_MM_FORMATTER);
            LocalTime endTime = LocalTime.parse(endTimeStr, HH_MM_FORMATTER);
            LocalTime currentTime = LocalTime.now(ZoneOffset.UTC);

            // Handle overnight trading period (e.g. start 22:00, end 05:00)
            if (startTime.isAfter(endTime)) { // Overnight case
                if (!(currentTime.isAfter(startTime) || currentTime.equals(startTime) ||
                      currentTime.isBefore(endTime))) {
                    log.info("Outside trading hours (overnight period). Current UTC time: {}. Trading hours: {}-{}", currentTime, startTimeStr, endTimeStr); // logger to log
                    return;
                }
            } else { // Same day case
                 if (currentTime.isBefore(startTime) || currentTime.isAfter(endTime)) {
                    log.info("Outside trading hours. Current UTC time: {}. Trading hours: {}-{}", currentTime, startTimeStr, endTimeStr); // logger to log
                    return;
                }
            }
            log.info("Within trading hours. Proceeding with strategy."); // logger to log
        } catch (Exception e) {
            log.error("Error parsing trading hours from config. Start: '{}', End: '{}'. Proceeding as if within hours.", // logger to log
                strategyConfig.getTradingHours().getStart(), strategyConfig.getTradingHours().getEnd(), e);
            // Fallback: proceed if parsing fails, or handle more strictly.
        }


        List<String> symbols = strategyConfig.getSymbols();
        if (symbols == null || symbols.isEmpty()) {
            log.warn("No trading symbols configured. Skipping trading loop."); // logger to log
            return;
        }

        for (String symbol : symbols) {
            try {
                log.info("Processing symbol: {}", symbol); // logger to log
                ActivePositionDto currentPosition = positionService.getPosition(symbol).orElse(null);
                List<Candlestick> candles = exchangeDataService.getCandlestickBars(symbol, strategyConfig.getDefaultTimeframe(), 200);

                if (candles == null || candles.isEmpty()) {
                    log.warn("No candlestick data received for {}. Skipping.", symbol); // logger to log
                    continue;
                }
                Candlestick latestCandle = candles.get(candles.size() - 1);
                Num currentPrice = DecimalNum.valueOf(latestCandle.getClosePrice());

                if (currentPosition != null) {
                    // --- Check for Exit Conditions ---
                    log.info("Active position found for {}: {}", symbol, currentPosition); // logger to log
                    boolean exited = handleExitConditions(currentPosition, candles, latestCandle);
                    if (exited) {
                        continue; // Move to next symbol if position was closed
                    }
                } else {
                    // --- If No Active Position, Check for Entries ---
                    log.info("No active position for {}. Checking for entry signals.", symbol); // logger to log
                    TradingDecision decision = tradingStrategy.generateSignal(symbol, strategyConfig.getDefaultTimeframe());

                    if (decision.getSignal() == TradeSignal.LONG_ENTRY || decision.getSignal() == TradeSignal.SHORT_ENTRY) {
                        log.info("Entry signal {} for {} at approx price {}. Reason: {}", decision.getSignal(), symbol, currentPrice, decision.getReason()); // logger to log

                        Num quantityToTrade = calculateQuantity(symbol, currentPrice, strategyConfig.getFixedUsdAmountPerTrade());
                        if (quantityToTrade == null || quantityToTrade.isLessThanOrEqual(DecimalNum.ZERO)) {
                            log.warn("Could not calculate valid quantity for {}. Skipping trade.", symbol); // logger to log
                            continue;
                        }

                        String orderSide = (decision.getSignal() == TradeSignal.LONG_ENTRY) ? "BUY" : "SELL";
                        NewOrderResponseDto orderResponse = orderService.placeNewOrder(symbol, orderSide, "MARKET", null, quantityToTrade.doubleValue(), null);

                        if (orderResponse != null && ("FILLED".equalsIgnoreCase(orderResponse.getStatus()) || "NEW".equalsIgnoreCase(orderResponse.getStatus()) || orderResponse.getStatus().contains("SIMULATED"))) { // "NEW" for limit orders if used later
                            Num executedPrice = (orderResponse.getCummulativeQuoteQty() != null && DecimalNum.valueOf(orderResponse.getExecutedQty()).isPositive()) ?
                                DecimalNum.valueOf(orderResponse.getCummulativeQuoteQty()).dividedBy(DecimalNum.valueOf(orderResponse.getExecutedQty())) :
                                currentPrice; // Fallback to current price if not available from response

                            ActivePositionDto.PositionSide side = (orderSide.equals("BUY")) ? ActivePositionDto.PositionSide.LONG : ActivePositionDto.PositionSide.SHORT;
                            ActivePositionDto newPosition = new ActivePositionDto(
                                    symbol,
                                    executedPrice,
                                    DecimalNum.valueOf(orderResponse.getExecutedQty()),
                                    side,
                                    DecimalNum.valueOf(latestCandle.getLowPrice()),  // Low of entry candle
                                    DecimalNum.valueOf(latestCandle.getHighPrice()) // High of entry candle
                            );
                            positionService.updatePosition(newPosition); // Changed
                            String notes = "Entry order placed. " + decision.getReason();
                            tradeLoggerService.logTrade(decision, orderResponse, notes);
                            notificationService.sendMessage(String.format("Trade Alert: %s %s @ %s. %s. Order ID: %s",
                                    decision.getSignal(), symbol, executedPrice.toString(), notes, orderResponse.getOrderId()));
                            log.info("Successfully opened position: {}", newPosition); // logger to log
                        } else {
                            String failureReason = orderResponse != null ? orderResponse.getStatus() + " - " + orderResponse.getReason() : "Order placement failed, null response.";
                            log.error("Failed to place entry order for {}. Reason: {}", symbol, failureReason); // logger to log
                            notificationService.sendMessage(String.format("Order Error: Failed to %s %s. Reason: %s", orderSide, symbol, failureReason));
                            tradeLoggerService.logTrade(decision, orderResponse, "Entry order placement failed: " + failureReason);
                        }
                    } else {
                        log.info("Signal for {} is {}. No action taken.", symbol, decision.getSignal()); // logger to log
                    }
                }
            } catch (Exception e) {
                log.error("Error processing symbol {}: {}", symbol, e.getMessage(), e); // logger to log
                notificationService.sendMessage(String.format("Bot Error: Exception processing symbol %s: %s", symbol, e.getMessage()));
            }
        }
        log.info("Trading loop finished."); // logger to log
    }

    private boolean handleExitConditions(ActivePositionDto position, List<Candlestick> candles, Candlestick latestCandle) {
        Num currentPrice = DecimalNum.valueOf(latestCandle.getClosePrice());
        String exitReason = null;
        TradeSignal exitSignal = null;

        Num entryPrice = position.getEntryPrice();
        Num stopLossPrice = null;
        Num targetPrice1 = null;

        BollingerBandsValues bb = technicalIndicatorService.calculateBollingerBands(
                candles,
                strategyConfig.getBollingerPeriod(),
                strategyConfig.getBollingerStdDev()
        );

        if (position.getSide() == ActivePositionDto.PositionSide.LONG) {
            stopLossPrice = entryPrice.multipliedBy(DecimalNum.ONE.minus(DecimalNum.valueOf(strategyConfig.getLongStopLossPercent() / 100.0)));
            // Also consider entryCandleLow as a stop loss
            if (position.getEntryCandleLow() != null && stopLossPrice.isLessThan(position.getEntryCandleLow())) {
                 // If calc SL is tighter than candle low, use calc SL. Otherwise, candle low might be preferred.
                 // For this logic, let's use the tighter of calculated SL or entry candle low.
                 stopLossPrice = stopLossPrice.max(position.getEntryCandleLow().multipliedBy(DecimalNum.valueOf(0.999))); // slightly below low
            } else if (position.getEntryCandleLow() != null) {
                stopLossPrice = position.getEntryCandleLow().multipliedBy(DecimalNum.valueOf(0.999));
            }


            targetPrice1 = entryPrice.multipliedBy(DecimalNum.ONE.plus(DecimalNum.valueOf(strategyConfig.getLongExitTarget1Percent() / 100.0)));

            if (currentPrice.isLessThanOrEqual(stopLossPrice)) {
                exitReason = "Stop loss triggered at " + currentPrice.toString();
                exitSignal = TradeSignal.LONG_EXIT;
            } else if (currentPrice.isGreaterThanOrEqual(targetPrice1)) {
                exitReason = "Target 1 profit hit at " + currentPrice.toString();
                exitSignal = TradeSignal.LONG_EXIT;
            } else if (bb != null && bb.getUpper() != null && currentPrice.isGreaterThanOrEqual(bb.getUpper())) {
                exitReason = "Price touched/crossed upper Bollinger Band at " + currentPrice.toString();
                exitSignal = TradeSignal.LONG_EXIT;
            }
        } else { // SHORT position
            stopLossPrice = entryPrice.multipliedBy(DecimalNum.ONE.plus(DecimalNum.valueOf(strategyConfig.getShortStopLossPercent() / 100.0)));
             if (position.getEntryCandleHigh() != null && stopLossPrice.isGreaterThan(position.getEntryCandleHigh())) {
                 stopLossPrice = stopLossPrice.min(position.getEntryCandleHigh().multipliedBy(DecimalNum.valueOf(1.001))); // slightly above high
            } else if (position.getEntryCandleHigh() != null) {
                stopLossPrice = position.getEntryCandleHigh().multipliedBy(DecimalNum.valueOf(1.001));
            }

            targetPrice1 = entryPrice.multipliedBy(DecimalNum.ONE.minus(DecimalNum.valueOf(strategyConfig.getShortExitTarget1Percent() / 100.0)));

            if (currentPrice.isGreaterThanOrEqual(stopLossPrice)) {
                exitReason = "Stop loss triggered at " + currentPrice.toString();
                exitSignal = TradeSignal.SHORT_EXIT;
            } else if (currentPrice.isLessThanOrEqual(targetPrice1)) {
                exitReason = "Target 1 profit hit at " + currentPrice.toString();
                exitSignal = TradeSignal.SHORT_EXIT;
            } else if (bb != null && bb.getLower() != null && currentPrice.isLessThanOrEqual(bb.getLower())) {
                exitReason = "Price touched/crossed lower Bollinger Band at " + currentPrice.toString();
                exitSignal = TradeSignal.SHORT_EXIT;
            }
        }

        if (exitSignal != null) {
            log.info("Exit signal {} for {}. Reason: {}", exitSignal, position.getSymbol(), exitReason); // logger to log
            String orderSide = (position.getSide() == ActivePositionDto.PositionSide.LONG) ? "SELL" : "BUY";

            NewOrderResponseDto orderResponse = orderService.placeNewOrder(
                    position.getSymbol(),
                    orderSide,
                    "MARKET",
                    null,
                    position.getQuantity().doubleValue(),
                    null);

            TradingDecision decision = new TradingDecision(exitSignal, position.getSymbol(), currentPrice, exitReason);
            if (orderResponse != null && ("FILLED".equalsIgnoreCase(orderResponse.getStatus()) || orderResponse.getStatus().contains("SIMULATED"))) {
                positionService.removePosition(position.getSymbol()); // Changed
                tradeLoggerService.logTrade(decision, orderResponse, exitReason);
                notificationService.sendMessage(String.format("Trade Alert: %s %s @ %s. %s. Order ID: %s",
                        exitSignal, position.getSymbol(), currentPrice.toString(), exitReason, orderResponse.getOrderId()));
                log.info("Successfully closed position for {}: {}", position.getSymbol(), exitReason); // logger to log
                return true;
            } else {
                String failureReason = orderResponse != null ? orderResponse.getStatus() + " - " + orderResponse.getReason() : "Order placement failed, null response.";
                log.error("Failed to place exit order for {}. Reason: {}", position.getSymbol(), failureReason); // logger to log
                notificationService.sendMessage(String.format("Order Error: Failed to %s %s to close position. Reason: %s", orderSide, position.getSymbol(), failureReason));
                tradeLoggerService.logTrade(decision, orderResponse, "Exit order placement failed: " + failureReason);
                return false; // Exit order failed, keep position active for now
            }
        }
        return false; // No exit condition met
    }

    private Num calculateQuantity(String symbol, Num currentPrice, double fixedUsdAmount) {
        if (currentPrice == null || currentPrice.isZero() || currentPrice.isNegative()) {
            log.warn("Cannot calculate quantity for symbol {} due to invalid current price: {}", symbol, currentPrice); // logger to log
            return DecimalNum.ZERO;
        }
        // Example: Invest a fixed USD amount per trade
        return DecimalNum.valueOf(fixedUsdAmount).dividedBy(currentPrice);
    }
}

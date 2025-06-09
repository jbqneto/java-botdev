# Project Explanation: Trading Bot

## 1. Project Overview

**Purpose:**
This project implements a demonstration cryptocurrency trading bot that operates based on technical indicators and a predefined day trading strategy. It is designed to interact with the Binance exchange (though parts are abstracted for potential future extension to other exchanges), make trading decisions, place orders, log trades, and send notifications via Telegram.

**Functionality:**
-   Fetches market data (candlestick/kline) from Binance.
-   Calculates various technical indicators (VWAP, RSI, EMA, Bollinger Bands, Average Volume).
-   Applies a configurable trading strategy to generate LONG/SHORT entry or exit signals.
-   Manages active positions.
-   Places market orders on Binance.
-   Logs all trading decisions and order outcomes to a CSV file.
-   Sends notifications about trades and errors via Telegram.
-   Operates on a configurable schedule (e.g., every 15 minutes).
-   Allows configuration of API keys, Telegram bot details, and strategy parameters via `application.yml`.

**Technologies Used:**
-   Java 21
-   Spring Boot 3.5.0 (for application framework, dependency injection, scheduling, configuration management)
-   Maven (for project build and dependency management)
-   Binance Connector/J (official Java library for Binance API)
-   TelegramBots (Java library for Telegram Bot API)
-   TA4J-Core (Java library for technical analysis)
-   Jackson Databind (for JSON parsing)
-   Lombok (to reduce boilerplate code)
-   SLF4J (for logging, with Logback as the default Spring Boot binding)
-   JUnit 5 & Mockito (for unit testing)

---

## 2. `BotdevApplication.java`

**Path:** `com.jbqneto.dev.botdev.BotdevApplication`

This is the main entry point for the Spring Boot application.

```java
package com.jbqneto.dev.botdev;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BotdevApplication {
    public static void main(String[] args) {
        SpringApplication.run(BotdevApplication.class, args);
    }
}
```

**Key Features:**
-   `@SpringBootApplication`: This is a convenience annotation that adds all of the following:
    -   `@Configuration`: Tags the class as a source of bean definitions for the application context.
    -   `@EnableAutoConfiguration`: Tells Spring Boot to start adding beans based on classpath settings, other beans, and various property settings. For example, it automatically configures Jackson for JSON if it's on the classpath.
    -   `@ComponentScan`: Tells Spring to look for other components, configurations, and services in the `com.jbqneto.dev.botdev` package and its sub-packages. This is how all the custom services, configuration classes, and the scheduler are found and managed by Spring.
-   `SpringApplication.run(...)`: Launches the Spring application context.

**Scheduling:**
While `@EnableScheduling` can be added here, it's currently on the `TradingBotScheduler` component itself, which is sufficient for Spring Boot to detect and process scheduled tasks within that component.

---

## 3. Configuration Classes (`com.jbqneto.dev.botdev.config`)

These classes are used to map external configuration properties (primarily from `application.yml`) into type-safe Java objects. They all use `@Configuration` to be recognized as Spring configuration beans and `@ConfigurationProperties` to specify the prefix under which their properties are found in the YAML file. Lombok's `@Data` is used to generate getters, setters, `toString`, `equals`, and `hashCode` methods.

### 3.1. `BinanceConfig.java`

**Purpose:** Holds configuration specific to Binance API access.

**Fields:**
-   `apiKey` (String): The API key for Binance.
-   `apiSecret` (String): The API secret for Binance.

**YAML Mapping (`application.yml`):**
```yaml
binance:
  apiKey: YOUR_BINANCE_API_KEY
  apiSecret: YOUR_BINANCE_API_SECRET
```

### 3.2. `TelegramConfig.java`

**Purpose:** Holds configuration for Telegram bot notifications.

**Fields:**
-   `botToken` (String): The token for the Telegram bot.
-   `chatId` (String): The target chat ID for sending notifications.

**YAML Mapping (`application.yml`):**
```yaml
telegram:
  botToken: YOUR_TELEGRAM_BOT_TOKEN
  chatId: YOUR_TELEGRAM_CHAT_ID
```

### 3.3. `StrategyConfig.java`

**Purpose:** Holds all parameters related to the trading strategy and bot operation.

**Key Fields:**
-   `symbols` (List<String>): List of trading symbols to operate on (e.g., "BTCUSDT", "ETHUSDT").
-   `defaultTimeframe` (String): The primary candlestick interval for trading decisions (e.g., "15m").
-   `confirmationTimeframes` (List<String>): Additional timeframes for potential confirmation signals (currently not used in the core logic but available).
-   `tradingHours` (TradingHours): Nested object defining the allowed trading window.
    -   `start` (String): Start time in HH:mm format (UTC).
    -   `end` (String): End time in HH:mm format (UTC).
-   `rsiPeriod` (int): Period for RSI calculation (e.g., 14).
-   `emaShortPeriod` (int): Period for the shorter EMA (e.g., 9).
-   `emaLongPeriod` (int): Period for the longer EMA (e.g., 21).
-   `bollingerPeriod` (int): Period for Bollinger Bands calculation (e.g., 20).
-   `bollingerStdDev` (double): Standard deviation multiplier for Bollinger Bands (e.g., 2.0).
-   `volumeAvgPeriod` (int): Period for calculating average volume (e.g., 10).
-   `longExitTarget1Percent` (double): Profit target percentage for exiting long positions.
-   `longStopLossPercent` (double): Stop loss percentage for exiting long positions.
-   `shortExitTarget1Percent` (double): Profit target percentage for exiting short positions.
-   `shortStopLossPercent` (double): Stop loss percentage for exiting short positions.
-   `fixedUsdAmountPerTrade` (double): The amount in USD to be used for sizing each trade.
-   `scheduler` (Scheduler): Nested object for scheduler configuration.
    -   `cron` (String): Cron expression for the trading loop scheduler.

**YAML Mapping (`application.yml`):**
```yaml
strategy:
  symbols: BTCUSDT,ETHUSDT,SOLUSDT
  defaultTimeframe: 15m
  confirmationTimeframes: 5m,1h
  tradingHours:
    start: "02:00" # UTC
    end: "23:00"   # UTC
  rsiPeriod: 14
  # ... other indicator and exit target fields ...
  fixedUsdAmountPerTrade: 100.0
  scheduler:
    cron: "0 0/15 * * * ?"
```

---

## 4. DTOs (`com.jbqneto.dev.botdev.dto`)

Data Transfer Objects (DTOs) are simple classes used to carry data between layers and components. They are refactored using Lombok's `@Data`, `@NoArgsConstructor`, and `@AllArgsConstructor` for conciseness.

### 4.1. `ActivePositionDto.java`

**Purpose:** Represents an currently open trade/position.
**Fields:**
-   `symbol` (String): The trading symbol.
-   `entryPrice` (Num): The price at which the position was entered.
-   `quantity` (Num): The amount of the asset held.
-   `side` (PositionSide): Enum (`LONG` or `SHORT`) indicating the direction of the trade.
-   `entryCandleLow` (Num): The low price of the candle on which the entry signal occurred (used for some stop-loss calculations).
-   `entryCandleHigh` (Num): The high price of the candle on which the entry signal occurred.
-   `entryTimestamp` (long): Timestamp of when the position DTO was created in memory.

### 4.2. `BollingerBandsValues.java`

**Purpose:** A simple container to hold the three values of Bollinger Bands.
**Fields:**
-   `upper` (Num): The upper Bollinger Band value.
-   `middle` (Num): The middle Bollinger Band value (SMA).
-   `lower` (Num): The lower Bollinger Band value.

### 4.3. `Candlestick.java`

**Purpose:** Represents a single candlestick/kline bar from the exchange.
**Annotation:** `@JsonIgnoreProperties(ignoreUnknown = true)` makes Jackson parsing robust to extra fields from the API.
**Fields:**
-   `openTime` (long): Timestamp for the opening of the candle.
-   `openPrice` (String): Opening price.
-   `highPrice` (String): Highest price during the candle period.
-   `lowPrice` (String): Lowest price during the candle period.
-   `closePrice` (String): Closing price.
-   `volume` (String): Trading volume in the base asset.
-   `closeTime` (long): Timestamp for the closing of the candle.
-   `quoteAssetVolume` (String): Trading volume in the quote asset.
-   `numberOfTrades` (int): Number of trades during the candle period.
-   `takerBuyBaseAssetVolume` (String): Volume of the base asset bought by takers.
-   `takerBuyQuoteAssetVolume` (String): Volume of the quote asset bought by takers.

### 4.4. `NewOrderResponseDto.java`

**Purpose:** Represents the response from the exchange after placing a new order. Also used to convey error information if order placement fails.
**Annotation:** `@JsonIgnoreProperties(ignoreUnknown = true)`.
**Key Fields:**
-   `symbol` (String): Trading symbol.
-   `orderId` (Long): Unique ID assigned by the exchange.
-   `clientOrderId` (String): User-defined order ID.
-   `transactTime` (Long): Timestamp of the transaction.
-   `price` (String): Price of the order (especially for LIMIT orders).
-   `origQty` (String): Original quantity of the order.
-   `executedQty` (String): Quantity executed in the trade.
-   `cummulativeQuoteQty` (String): Total quote asset amount filled.
-   `status` (String): Order status (e.g., "NEW", "FILLED", "CANCELED", "REJECTED").
-   `timeInForce` (String): Time in force for the order (e.g., "GTC").
-   `type` (String): Order type (e.g., "LIMIT", "MARKET").
-   `side` (String): Order side ("BUY" or "SELL").
-   `reason` (String): Custom field used internally to pass error messages or notes if order placement fails before or during API call.

### 4.5. `TradingDecision.java`

**Purpose:** Encapsulates the output of the trading strategy, indicating the signal and relevant details.
**Fields:**
-   `signal` (TradeSignal): The generated trading signal (e.g., `LONG_ENTRY`, `HOLD`).
-   `symbol` (String): The trading symbol for which the decision was made.
-   `price` (Num): The intended entry/exit price associated with the signal (usually the current close price at the time of decision).
-   `reason` (String): A textual explanation for why the signal was generated.
-   `timestamp` (long): Timestamp of when the decision was made, automatically set to `System.currentTimeMillis()` by its custom constructors.
**Custom Constructors:** It retains custom constructors to automatically set the `timestamp` upon creation, ensuring each decision is time-stamped.

---

## 5. Core Service Interfaces & Implementations (`com.jbqneto.dev.botdev.service`)

Each service follows a common pattern: an interface defining the contract and an implementation class providing the specific logic. They are annotated with `@Service` for Spring component scanning and use `@Slf4j` for logging.

### 5.1. `ExchangeDataService` / `BinanceExchangeDataServiceImpl`

-   **`ExchangeDataService` (Interface):**
    -   **Purpose:** Defines a generic contract for fetching market data from an exchange. This abstraction allows for potential future implementations for other exchanges.
    -   **`getCandlestickBars(String symbol, String interval, Integer limit)`:** Method to fetch candlestick data.
-   **`BinanceExchangeDataServiceImpl` (Implementation):**
    -   **Purpose:** Implements `ExchangeDataService` specifically for the Binance exchange.
    -   **Constructor:** Injects `BinanceConfig` to get API keys. Initializes `SpotClientImpl` (from `binance-connector-java`). If API keys are placeholders or missing, it logs a warning and initializes the client without credentials (limiting it to public endpoints).
    -   **`getCandlestickBars(...)`:**
        -   Takes symbol, interval, and limit as parameters.
        -   Uses `spotClient.createMarket().klines(parameters)` to make the API call.
        -   **JSON Parsing:** The raw JSON string response from Binance is parsed using Jackson's `ObjectMapper`. The response is a `List<List<Object>>`.
        -   **`mapKlineEntryToCandlestick(List<Object> klineEntry, String symbolForLogging)` (private helper):** This method is called for each inner list (representing a single candlestick) to map its indexed elements to the fields of the `Candlestick` DTO. It handles type conversions (e.g., `Number` to `long` or `String`).
        -   **Error Handling:** Catches exceptions during API calls or JSON parsing, logs them, and returns an empty list.

### 5.2. `TechnicalIndicatorService` / `TechnicalIndicatorServiceImpl`

-   **`TechnicalIndicatorService` (Interface):**
    -   **Purpose:** Defines a contract for calculating various technical indicators.
    -   Methods: `calculateVWAP`, `calculateRSI`, `calculateEMA`, `calculateBollingerBands`, `calculateAverageVolume`. Each takes a `List<Candlestick>` and relevant period(s) as input.
-   **`TechnicalIndicatorServiceImpl` (Implementation):**
    -   **Purpose:** Implements `TechnicalIndicatorService` using the TA4J-Core library.
    -   **`convertToBarSeries(List<Candlestick> candlesticks, String seriesName)` (private helper):**
        *   Converts a list of `Candlestick` DTOs into a TA4J `BarSeries` object. This is a prerequisite for using TA4J indicators.
        *   Calls `mapCandlestickToTa4jBar` for each DTO.
    -   **`mapCandlestickToTa4jBar(Candlestick candle)` (private helper):**
        *   Converts a single `Candlestick` DTO to a TA4J `org.ta4j.core.Bar`.
        *   Maps DTO fields (open, high, low, close prices, volume) to `Num` objects (using `DecimalNum::valueOf`).
        *   Converts the `closeTime` (long) to `ZonedDateTime` as required by TA4J.
        *   Handles potential errors during conversion of individual candles.
    -   **Indicator Calculation Methods:**
        *   Each method (`calculateVWAP`, `calculateRSI`, etc.) first converts the input `List<Candlestick>` to a `BarSeries`.
        *   Performs a check to ensure there's enough data for the given indicator period; returns `null` and logs a warning if not.
        *   Instantiates the appropriate TA4J indicator (e.g., `VWAPIndicator`, `RSIIndicator`, `EMAIndicator`, `BollingerBandsLowerIndicator`, etc.). For price-based indicators like RSI and EMA, it uses a `ClosePriceIndicator` as input. For average volume, it uses a `VolumeIndicator`.
        *   Returns the calculated value of the indicator for the most recent bar in the series (`series.getEndIndex()`).
        *   `calculateBollingerBands` returns a `BollingerBandsValues` DTO containing the upper, middle, and lower band values.

### 5.3. `OrderService` / `OrderServiceImpl`

-   **`OrderService` (Interface):**
    -   **Purpose:** Defines a contract for placing and managing orders on an exchange.
    -   **`placeNewOrder(...)`:** Method to place a new order.
    -   Placeholder methods for `cancelOrder` and `getOrderStatus` are included for future expansion.
-   **`OrderServiceImpl` (Implementation):**
    -   **Purpose:** Implements `OrderService` for the Binance exchange.
    -   **Constructor:** Injects `BinanceConfig`. Initializes `SpotClientImpl`. Similar to `BinanceExchangeDataServiceImpl`, it handles placeholder/missing API keys by logging a warning and initializing a client with limited capabilities (order placement will fail if keys are placeholders, which is checked in `placeNewOrder`).
    -   **`placeNewOrder(...)`:**
        *   **API Key Check:** Explicitly checks if API keys are placeholders; if so, logs an error and returns an error DTO without attempting an API call.
        *   **Input Validation:** Checks for valid quantity and, for LIMIT orders, a valid price.
        *   **Parameter Preparation:** Constructs a `LinkedHashMap` of parameters required by the Binance API's `newOrder` endpoint (e.g., `symbol`, `side`, `type`, `quantity`, `price`, `timeInForce`, `newOrderRespType="RESULT"`).
        *   **API Call:** Uses `spotClient.createTrade().newOrder(parameters)` to place the order.
        *   **JSON Parsing:** The raw JSON string response from Binance is parsed into `NewOrderResponseDto` using Jackson's `ObjectMapper`.
        *   **DTO Enrichment:** If certain fields (like symbol, side, type, original quantity) are not in the API response (depending on `newOrderRespType`), they are populated from the request parameters.
        *   **Error Handling:** Catches exceptions (including potential `BinanceClientException` or `JsonProcessingException`), logs them, and populates `NewOrderResponseDto` with error status and reason.

### 5.4. `PositionService` / `PositionServiceImpl`

-   **`PositionService` (Interface):**
    *   **Purpose:** Defines a contract for managing the state of active trading positions.
    *   Methods: `getPosition(String symbol)`, `updatePosition(ActivePositionDto position)`, `removePosition(String symbol)`, `getAllActiveSymbols()`, `hasActivePosition(String symbol)`.
-   **`PositionServiceImpl` (Implementation):**
    *   **Purpose:** Implements `PositionService` using an in-memory `ConcurrentHashMap` to store active positions.
    *   **`activePositions` (Map):** Stores `ActivePositionDto` objects keyed by symbol.
    *   **Method Implementations:** Provides straightforward CRUD-like operations on the `activePositions` map, with logging for significant actions (add, update, remove). Includes null checks for inputs.

### 5.5. `NotificationService` / `TelegramNotificationServiceImpl`

-   **`NotificationService` (Interface):**
    *   **Purpose:** Defines a generic contract for sending notifications.
    *   **`sendMessage(String messageText)`:** Method to send a message. Returns `true` if successful, `false` otherwise.
-   **`TelegramNotificationServiceImpl` (Implementation):**
    *   **Purpose:** Implements `NotificationService` to send messages via Telegram.
    *   **Constructor:** Injects `TelegramConfig`. Initializes a `DefaultAbsSender` (from `telegrambots` library). If the bot token is a placeholder or missing, the sender is not initialized, and a warning is logged.
    *   **`sendMessage(String messageText)`:**
        *   **Pre-condition Checks:** Returns `false` if the `telegramSender` was not initialized or if the `chatId` from config is missing/placeholder.
        *   **Message Creation:** Creates a `org.telegram.telegrambots.meta.api.methods.send.SendMessage` object, setting the target `chatId` and the message text.
        *   **Execution:** Calls `telegramSender.execute(message)` to send the message.
        *   **Error Handling:** Catches `TelegramApiException`, logs errors, and returns `false`.

### 5.6. `TradeLoggerService` / `FileTradeLoggerService`

-   **`TradeLoggerService` (Interface):**
    *   **Purpose:** Defines a contract for logging trade-related activities.
    *   **`logTrade(TradingDecision decision, NewOrderResponseDto orderResponse, String notes)`:** Method to log a trade.
-   **`FileTradeLoggerService` (Implementation):**
    *   **Purpose:** Implements `TradeLoggerService` by writing trade information to a local CSV file.
    *   **Log File:** Defines a default log file name (`tradelog.csv`). Provides a constructor to allow a custom file path (useful for testing).
    *   **Constructor & Header:** Ensures a CSV header (`Timestamp,Symbol,Signal,IntendedPrice,OrderID,Status,ExecutedQty,CumulativeQuoteQty,Notes`) is written to the file if it's new or empty.
    *   **`logTrade(...)` (synchronized):**
        *   The method is `synchronized` to provide basic thread safety for file writes.
        *   **Formatting:** Formats the log entry into a CSV string using `StringJoiner`. Handles null values in DTOs by substituting "N/A" or empty strings.
        *   **`escapeCsv(String data)` (private helper):** Escapes fields containing commas, double quotes, or newlines by enclosing them in double quotes and doubling up any existing double quotes within the field.
        *   **File Writing:** Appends the formatted log line to the file using `java.nio.file.Files.write` with `StandardOpenOption.APPEND` and `StandardOpenOption.CREATE`.
        *   **Error Handling:** Catches `IOException` during file writing and logs an error message.

---

## 6. Trading Strategy Components (`com.jbqneto.dev.botdev.strategy`)

### 6.1. `TradeSignal` (Enum)

**Purpose:** Represents the possible trading signals generated by the strategy.
**Values:**
-   `LONG_ENTRY`: Signal to enter a long position.
-   `LONG_EXIT`: Signal to exit a long position.
-   `SHORT_ENTRY`: Signal to enter a short position.
-   `SHORT_EXIT`: Signal to exit a short position.
-   `HOLD`: No specific trading action advised.

### 6.2. `TradingStrategy` (Interface)

**Purpose:** Defines the contract for any trading strategy implementation.
**`generateSignal(String symbol, String timeframe)`:**
-   This is the core method a strategy must implement.
-   It's responsible for fetching necessary market data (via injected services), calculating technical indicators, applying its internal rules, and returning a `TradingDecision`.
-   The `timeframe` parameter suggests the primary timeframe for analysis, though a strategy might internally use other timeframes for confirmation.

### 6.3. `DayTradingStrategyImpl` (Implementation)

**Purpose:** Implements a specific day trading strategy based on a combination of technical indicators.
**Constructor:** Injects `ExchangeDataService`, `TechnicalIndicatorService`, and `StrategyConfig`.
**`generateSignal(String symbol, String timeframe)`:**
-   **Data Fetching:** Uses `ExchangeDataService` to get candlestick data for the specified `symbol` and `primaryTimeframe` (from `StrategyConfig`). Fetches a fixed limit of candles (e.g., 200).
-   **Indicator Calculation:**
    -   Calculates VWAP (20-period), RSI (from `strategyConfig.rsiPeriod`), EMA (short and long periods from `strategyConfig`), Bollinger Bands (period and stddev from `strategyConfig`), and Average Volume (from `strategyConfig`).
    -   **Previous Indicator Values:** To detect crossovers or changes (e.g., RSI rising), it currently recalculates indicators for the dataset excluding the latest candle. A `TODO` comment notes this inefficiency and suggests refactoring `TechnicalIndicatorService` for better performance in the future.
-   **Rule Logic (extracted into private helper methods):**
    -   **`checkForLongEntry(...)`**:
        1.  **VWAP Cross:** Price (close of the latest candle) crosses above VWAP.
        2.  **RSI Confirmation:** RSI was recently below 30 (oversold) and is now rising (current RSI > previous RSI).
        3.  **EMA Crossover:** Shorter EMA (e.g., EMA9) crosses above the longer EMA (e.g., EMA21).
        4.  **Volume Confirmation:** Current candle's volume is greater than the average volume over a specified period (e.g., last 10 candles).
        -   If all conditions are met, a `LONG_ENTRY` signal is generated with an aggregated reason string.
    -   **`checkForShortEntry(...)`**:
        1.  **VWAP Cross:** Price crosses below VWAP.
        2.  **RSI Confirmation:** RSI was recently above 70 (overbought) and is now falling.
        3.  **EMA Crossover:** Shorter EMA crosses below the longer EMA.
        4.  **Volume Confirmation:** Increased volume occurs on a bearish candle (candle closes lower than its open).
        -   If all conditions are met, a `SHORT_ENTRY` signal is generated.
-   **Default Signal:** If no entry conditions are met, a `HOLD` signal is returned.
-   **Exit Logic:** This class is stateless and focuses on entry signals. Exit logic is handled by `TradingBotScheduler` based on active position data.

---

## 7. Scheduler (`com.jbqneto.dev.botdev.scheduler`)

### 7.1. `TradingBotScheduler`

**Purpose:** Orchestrates the entire trading bot's execution cycle on a schedule. It is the central component that ties all other services together.
**Annotations:**
-   `@Component`: Marks it as a Spring-managed component.
-   `@EnableScheduling`: Enables Spring's scheduled task execution.
-   `@Slf4j`: For Lombok-provided logger.
**Constructor:** Injects all necessary services: `StrategyConfig`, `ExchangeDataService`, `TechnicalIndicatorService`, `TradingStrategy`, `OrderService`, `NotificationService`, `TradeLoggerService`, and the new `PositionService`.
**`activePositions` (Map):** Removed; now uses `PositionService`.
**`runTradingLoop()` (Scheduled Method):**
-   Annotated with `@Scheduled(cron = "${strategy.scheduler.cron:0 0/15 * * * ?}")`, making the schedule configurable via `application.yml` (under `strategy.scheduler.cron`), with a default of every 15 minutes.
-   **Trading Hours Check:**
    -   Parses `start` and `end` trading hours (UTC) from `StrategyConfig`.
    -   Compares with the current UTC time. If outside trading hours, it logs a message and skips the current cycle. Handles overnight trading periods correctly.
-   **Symbol Iteration:** Retrieves the list of symbols from `StrategyConfig` and processes each one.
-   **For each symbol:**
    1.  **Get Active Position:** Checks if an active position exists for the symbol using `positionService.getPosition(symbol)`.
    2.  **Fetch Data:** Retrieves the latest candlestick data using `ExchangeDataService`.
    3.  **Process Exits (if position exists):** Calls `processExitStrategyAndExecuteOrder(...)`.
    4.  **Process Entries (if no position exists):** Calls `handlePotentialEntry(...)`.
    5.  Handles exceptions during symbol processing by logging and sending a notification.
-   **`processExitStrategyAndExecuteOrder(ActivePositionDto position, List<Candlestick> candles, Candlestick latestCandle)` (private helper):**
    -   **Calculates Exit Conditions:**
        *   Determines current price from the latest candle.
        *   Calculates stop-loss and profit target prices based on the position's entry price and percentages from `StrategyConfig`.
        *   Considers `entryCandleLow` (for longs) or `entryCandleHigh` (for shorts) in stop-loss calculations.
        *   Calculates Bollinger Bands using `TechnicalIndicatorService`.
        *   Checks for:
            *   Stop-loss hit.
            *   Profit target 1 hit.
            *   Price touching/crossing Bollinger Bands (upper for LONG, lower for SHORT).
    -   **Executes Exit:** If any exit condition is met:
        *   Determines the order side (SELL for LONG, BUY for SHORT) and quantity (full position quantity).
        *   Places a MARKET order using `OrderService`.
        *   If the order is successful (FILLED or simulated):
            *   Removes the position using `positionService.removePosition()`.
            *   Logs the exit trade using `TradeLoggerService`.
            *   Sends a notification using `NotificationService`.
        *   If the order fails, logs an error and sends an error notification.
    -   Returns `true` if an exit order was successfully placed, `false` otherwise.
-   **`handlePotentialEntry(String symbol, Candlestick latestCandle, Num currentPrice)` (private helper):**
    -   Calls `tradingStrategy.generateSignal(...)`.
    -   If an `LONG_ENTRY` or `SHORT_ENTRY` signal is received:
        *   **Quantity Calculation:** Calls `calculateQuantity(...)` to determine the trade size.
        *   **`calculateQuantity(String symbol, Num currentPrice, double fixedUsdAmount)` (private helper):** Calculates the quantity of the asset to trade based on a `fixedUsdAmount` (from `StrategyConfig`) and the `currentPrice`. Returns `DecimalNum.ZERO` if the price is invalid.
        *   Places a MARKET order using `OrderService`.
        *   If the order is successful:
            *   Creates an `ActivePositionDto` with details from the order response (executed price, quantity) and latest candle (low/high for potential future stop-loss reference).
            *   Stores the new position using `positionService.updatePosition()`.
            *   Logs the entry trade and sends a notification.
        *   If the order fails, logs an error and sends an error notification.
    -   If a `HOLD` signal is received, logs this information.

---
This document provides a high-level overview and detailed explanations of each major component of the trading bot project.

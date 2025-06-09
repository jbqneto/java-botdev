package com.jbqneto.dev.botdev.logger;

import com.jbqneto.dev.botdev.dto.NewOrderResponseDto;
import com.jbqneto.dev.botdev.dto.TradingDecision;
import lombok.extern.slf4j.Slf4j; // Added
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.ta4j.core.num.Num;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.StringJoiner;

@Service
@Slf4j
public class FileTradeLoggerService implements TradeLoggerService {

    private static final String LOG_FILE_NAME = "tradelog.csv";
    private final Path logFilePath;
    private static final DateTimeFormatter CSV_TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS z").withZone(ZoneId.systemDefault());

    public FileTradeLoggerService() {
        // Consider making the log file path configurable
        this.logFilePath = Paths.get(LOG_FILE_NAME);
        ensureHeader();
    }

    // Test constructor to allow specifying a different file path
    public FileTradeLoggerService(Path logFilePath) {
        this.logFilePath = logFilePath;
        ensureHeader();
    }

    private void ensureHeader() {
        try {
            if (Files.notExists(logFilePath)) {
                String header = "Timestamp,Symbol,Signal,IntendedPrice,OrderID,Status,ExecutedQty,CumulativeQuoteQty,Notes\n";
                Files.write(logFilePath, header.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE_NEW);
            } else if (Files.size(logFilePath) == 0) { // File exists but is empty
                 String header = "Timestamp,Symbol,Signal,IntendedPrice,OrderID,Status,ExecutedQty,CumulativeQuoteQty,Notes\n";
                Files.write(logFilePath, header.getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);
            }
        } catch (IOException e) {
            log.error("Failed to write CSV header to trade log file: {}", logFilePath, e); // logger to log
        }
    }

    @Override
    public synchronized void logTrade(TradingDecision decision, NewOrderResponseDto orderResponse, String notes) {
        String timestamp = CSV_TIMESTAMP_FORMATTER.format(Instant.now());

        StringJoiner csvEntry = new StringJoiner(",");
        csvEntry.add(timestamp);
        csvEntry.add(decision != null && decision.getSymbol() != null ? decision.getSymbol() : "N/A");
        csvEntry.add(decision != null && decision.getSignal() != null ? decision.getSignal().toString() : "N/A");

        Num intendedPrice = (decision != null ? decision.getPrice() : null);
        csvEntry.add(intendedPrice != null ? intendedPrice.toString() : "N/A");

        csvEntry.add(orderResponse != null && orderResponse.getOrderId() != null ? orderResponse.getOrderId().toString() : "N/A");
        csvEntry.add(orderResponse != null && orderResponse.getStatus() != null ? escapeCsv(orderResponse.getStatus()) : "N/A");
        csvEntry.add(orderResponse != null && orderResponse.getExecutedQty() != null ? escapeCsv(orderResponse.getExecutedQty()) : "N/A");
        csvEntry.add(orderResponse != null && orderResponse.getCummulativeQuoteQty() != null ? escapeCsv(orderResponse.getCummulativeQuoteQty()) : "N/A");
        csvEntry.add(notes != null ? escapeCsv(notes) : "");

        String logLine = csvEntry.toString() + "\n";

        try {
            Files.write(logFilePath, logLine.getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND, StandardOpenOption.CREATE);
        } catch (IOException e) {
            log.error("Failed to write to trade log file: {}", logFilePath, e); // logger to log
            // Optionally, re-throw or handle more gracefully
        }
    }

    private String escapeCsv(String data) {
        if (data == null) {
            return "";
        }
        // If data contains comma, quote, or newline, then enclose in double quotes
        if (data.contains(",") || data.contains("\"") || data.contains("\n")) {
            // Replace any existing double quotes with two double quotes
            data = data.replace("\"", "\"\"");
            return "\"" + data + "\"";
        }
        return data;
    }
}

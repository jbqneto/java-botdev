package com.jbqneto.dev.botdev.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "strategy")
@Data
public class StrategyConfig {

    private List<String> symbols;
    private String defaultTimeframe;
    private List<String> confirmationTimeframes;
    private TradingHours tradingHours = new TradingHours(); // Keep new TradingHours() for default initialization
    private int rsiPeriod;
    private int emaShortPeriod;
    private int emaLongPeriod;
    private int bollingerPeriod;
    private double bollingerStdDev;
    private int volumeAvgPeriod;
    private double longExitTarget1Percent;
    private double longStopLossPercent;
    private double shortExitTarget1Percent;
    private double shortStopLossPercent;
    private double fixedUsdAmountPerTrade;
    private Scheduler scheduler = new Scheduler(); // For nested scheduler.cron property

    @Data // Lombok for getters/setters
    public static class TradingHours {
        private String start;
        private String end;
    }

    @Data // Lombok for getters/setters
    public static class Scheduler {
        private String cron;
    }
}

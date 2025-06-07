package com.jbqneto.dev.botdev.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "strategy")
public class StrategyConfig {

    private List<String> symbols;
    private String defaultTimeframe;
    private List<String> confirmationTimeframes;
    private TradingHours tradingHours = new TradingHours();
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

    public static class TradingHours {
        private String start;
        private String end;

        public String getStart() {
            return start;
        }

        public void setStart(String start) {
            this.start = start;
        }

        public String getEnd() {
            return end;
        }

        public void setEnd(String end) {
            this.end = end;
        }
    }

    // Getters and Setters
    public List<String> getSymbols() {
        return symbols;
    }

    public void setSymbols(List<String> symbols) {
        this.symbols = symbols;
    }

    public String getDefaultTimeframe() {
        return defaultTimeframe;
    }

    public void setDefaultTimeframe(String defaultTimeframe) {
        this.defaultTimeframe = defaultTimeframe;
    }

    public List<String> getConfirmationTimeframes() {
        return confirmationTimeframes;
    }

    public void setConfirmationTimeframes(List<String> confirmationTimeframes) {
        this.confirmationTimeframes = confirmationTimeframes;
    }

    public TradingHours getTradingHours() {
        return tradingHours;
    }

    public void setTradingHours(TradingHours tradingHours) {
        this.tradingHours = tradingHours;
    }

    public int getRsiPeriod() {
        return rsiPeriod;
    }

    public void setRsiPeriod(int rsiPeriod) {
        this.rsiPeriod = rsiPeriod;
    }

    public int getEmaShortPeriod() {
        return emaShortPeriod;
    }

    public void setEmaShortPeriod(int emaShortPeriod) {
        this.emaShortPeriod = emaShortPeriod;
    }

    public int getEmaLongPeriod() {
        return emaLongPeriod;
    }

    public void setEmaLongPeriod(int emaLongPeriod) {
        this.emaLongPeriod = emaLongPeriod;
    }

    public int getBollingerPeriod() {
        return bollingerPeriod;
    }

    public void setBollingerPeriod(int bollingerPeriod) {
        this.bollingerPeriod = bollingerPeriod;
    }

    public double getBollingerStdDev() {
        return bollingerStdDev;
    }

    public void setBollingerStdDev(double bollingerStdDev) {
        this.bollingerStdDev = bollingerStdDev;
    }

    public int getVolumeAvgPeriod() {
        return volumeAvgPeriod;
    }

    public void setVolumeAvgPeriod(int volumeAvgPeriod) {
        this.volumeAvgPeriod = volumeAvgPeriod;
    }

    public double getLongExitTarget1Percent() {
        return longExitTarget1Percent;
    }

    public void setLongExitTarget1Percent(double longExitTarget1Percent) {
        this.longExitTarget1Percent = longExitTarget1Percent;
    }

    public double getLongStopLossPercent() {
        return longStopLossPercent;
    }

    public void setLongStopLossPercent(double longStopLossPercent) {
        this.longStopLossPercent = longStopLossPercent;
    }

    public double getShortExitTarget1Percent() {
        return shortExitTarget1Percent;
    }

    public void setShortExitTarget1Percent(double shortExitTarget1Percent) {
        this.shortExitTarget1Percent = shortExitTarget1Percent;
    }

    public double getShortStopLossPercent() {
        return shortStopLossPercent;
    }

    public void setShortStopLossPercent(double shortStopLossPercent) {
        this.shortStopLossPercent = shortStopLossPercent;
    }

    public double getFixedUsdAmountPerTrade() {
        return fixedUsdAmountPerTrade;
    }

    public void setFixedUsdAmountPerTrade(double fixedUsdAmountPerTrade) {
        this.fixedUsdAmountPerTrade = fixedUsdAmountPerTrade;
    }
}

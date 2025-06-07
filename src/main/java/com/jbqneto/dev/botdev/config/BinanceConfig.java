package com.jbqneto.dev.botdev.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "binance")
@Data
public class BinanceConfig {

    private String apiKey;
    private String apiSecret;

}

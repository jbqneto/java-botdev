package com.jbqneto.dev.botdev.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class NewOrderResponseDto {
    private String symbol;
    private Long orderId;
    private String clientOrderId;
    private Long transactTime;
    private String price;
    private String origQty; // Original quantity
    private String executedQty;
    private String cummulativeQuoteQty;
    private String status;
    private String timeInForce;
    private String type;
    private String side;
    private String reason; // Used for custom error reasons, not typically from Binance success response
}

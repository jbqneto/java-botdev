package com.jbqneto.dev.botdev.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.ta4j.core.num.Num;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BollingerBandsValues {
    private Num upper;
    private Num middle;
    private Num lower;
}

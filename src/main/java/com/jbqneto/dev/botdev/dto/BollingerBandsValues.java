package com.jbqneto.dev.botdev.dto;

import org.ta4j.core.num.Num;

public class BollingerBandsValues {
    private Num upper;
    private Num middle;
    private Num lower;

    public BollingerBandsValues(Num upper, Num middle, Num lower) {
        this.upper = upper;
        this.middle = middle;
        this.lower = lower;
    }

    public Num getUpper() {
        return upper;
    }

    public void setUpper(Num upper) {
        this.upper = upper;
    }

    public Num getMiddle() {
        return middle;
    }

    public void setMiddle(Num middle) {
        this.middle = middle;
    }

    public Num getLower() {
        return lower;
    }

    public void setLower(Num lower) {
        this.lower = lower;
    }

    @Override
    public String toString() {
        return "BollingerBandsValues{" +
                "upper=" + upper +
                ", middle=" + middle +
                ", lower=" + lower +
                '}';
    }
}

package com.github.cao.awa.catheter;

import java.math.BigDecimal;

public class BigDecimalCatheter extends Catheter<BigDecimal> {

    public BigDecimalCatheter(BigDecimal[] targets) {
        super(targets);
    }

    public static BigDecimalCatheter make(BigDecimal... targets) {
        return new BigDecimalCatheter(targets);
    }

    public static BigDecimalCatheter makeCapacity(int size) {
        return new BigDecimalCatheter(array(size));
    }

    public static <X> BigDecimalCatheter of(BigDecimal[] targets) {
        return new BigDecimalCatheter(targets);
    }

    private static BigDecimal[] array(int size) {
        return new BigDecimal[size];
    }
}

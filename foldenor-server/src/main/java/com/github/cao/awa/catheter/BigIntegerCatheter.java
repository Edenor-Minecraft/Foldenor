package com.github.cao.awa.catheter;

import java.math.BigInteger;

public class BigIntegerCatheter extends Catheter<BigInteger> {

    public BigIntegerCatheter(BigInteger[] targets) {
        super(targets);
    }

    public static BigIntegerCatheter make(BigInteger... targets) {
        return new BigIntegerCatheter(targets);
    }

    public static BigIntegerCatheter makeCapacity(int size) {
        return new BigIntegerCatheter(array(size));
    }

    public static <X> BigIntegerCatheter of(BigInteger[] targets) {
        return new BigIntegerCatheter(targets);
    }

    private static BigInteger[] array(int size) {
        return new BigInteger[size];
    }
}

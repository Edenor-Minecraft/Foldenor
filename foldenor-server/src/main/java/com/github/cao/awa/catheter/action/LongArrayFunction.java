package com.github.cao.awa.catheter.action;

@FunctionalInterface
public interface LongArrayFunction {
    long[] apply(long generator);
}

package com.github.cao.awa.catheter.action;

@FunctionalInterface
public interface ByteToIntegerFunction {

    /**
     * Applies this function to the given argument.
     *
     * @param value the function argument
     * @return the function result
     */
    int applyAsInteger(byte value);
}


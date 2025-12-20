package com.github.cao.awa.catheter.action;

@FunctionalInterface
public interface LongToByteFunction {

    /**
     * Applies this function to the given argument.
     *
     * @param value the function argument
     * @return the function result
     */
    byte applyAsByte(long value);
}


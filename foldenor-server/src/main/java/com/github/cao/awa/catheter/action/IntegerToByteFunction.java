package com.github.cao.awa.catheter.action;

@FunctionalInterface
public interface IntegerToByteFunction {

    /**
     * Applies this function to the given argument.
     *
     * @param value the function argument
     * @return the function result
     */
    byte applyAsByte(int value);
}


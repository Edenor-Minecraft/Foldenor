package com.github.cao.awa.catheter.action;

@FunctionalInterface
public interface BooleanToLongFunction {

    /**
     * Applies this function to the given argument.
     *
     * @param value the function argument
     * @return the function result
     */
    long applyAsLong(boolean value);
}


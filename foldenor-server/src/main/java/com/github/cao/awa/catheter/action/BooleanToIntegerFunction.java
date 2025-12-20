package com.github.cao.awa.catheter.action;

@FunctionalInterface
public interface BooleanToIntegerFunction {

    /**
     * Applies this function to the given argument.
     *
     * @param value the function argument
     * @return the function result
     */
    int applyAsInteger(boolean value);
}


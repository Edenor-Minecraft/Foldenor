package com.github.cao.awa.catheter.action;

@FunctionalInterface
public interface BooleanToDoubleFunction {

    /**
     * Applies this function to the given argument.
     *
     * @param value the function argument
     * @return the function result
     */
    double applyAsDouble(boolean value);
}


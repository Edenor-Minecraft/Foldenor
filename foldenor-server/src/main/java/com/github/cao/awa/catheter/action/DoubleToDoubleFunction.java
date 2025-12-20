package com.github.cao.awa.catheter.action;

@FunctionalInterface
public interface DoubleToDoubleFunction {

    /**
     * Applies this function to the given argument.
     *
     * @param value the function argument
     * @return the function result
     */
    double applyAsDouble(double value);
}


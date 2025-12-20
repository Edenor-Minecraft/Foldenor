package com.github.cao.awa.catheter.action;

@FunctionalInterface
public interface BiDoubleToDoubleFunction {

    /**
     * Applies this function to the given argument.
     *
     * @param d1 the first function argument
     * @param d2 the second function argument
     * @return the function result
     */
    double applyAsDouble(double d1, double d2);
}


package com.github.cao.awa.catheter.action;

@FunctionalInterface
public interface BiIntegerToIntegerFunction {

    /**
     * Applies this function to the given argument.
     *
     * @param i1 the first function argument
     * @param i2 the second function argument
     * @return the function result
     */
    int applyAsInt(int i1, int i2);
}


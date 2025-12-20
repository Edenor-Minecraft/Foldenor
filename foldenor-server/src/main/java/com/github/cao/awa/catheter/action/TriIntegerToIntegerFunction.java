package com.github.cao.awa.catheter.action;

@FunctionalInterface
public interface TriIntegerToIntegerFunction {

    /**
     * Applies this function to the given argument.
     *
     * @param i1 the first function argument
     * @param i2 the second function argument
     * @param i3 the tri function argument
     * @return the function result
     */
    int apply(int i1, int i2, int i3);
}


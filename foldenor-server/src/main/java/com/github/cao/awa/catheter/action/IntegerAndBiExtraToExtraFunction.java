package com.github.cao.awa.catheter.action;

@FunctionalInterface
public interface IntegerAndBiExtraToExtraFunction<X> {

    /**
     * Applies this function to the given argument.
     *
     * @param i the first function argument
     * @param x1 the second function argument
     * @param x2 the tri function argument
     * @return the function result
     */
    X apply(int i, X x1, X x2);
}


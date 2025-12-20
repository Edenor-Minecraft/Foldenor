package com.github.cao.awa.catheter.action;

@FunctionalInterface
public interface IntegerAndBiToLongFunction {

    /**
     * Applies this function to the given argument.
     *
     * @param i the first function argument
     * @param l1 the second function argument
     * @param l2 the tri function argument
     * @return the function result
     */
    long apply(int i, long l1, long l2);
}


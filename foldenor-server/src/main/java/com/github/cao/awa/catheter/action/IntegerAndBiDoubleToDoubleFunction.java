package com.github.cao.awa.catheter.action;

@FunctionalInterface
public interface IntegerAndBiDoubleToDoubleFunction {

    /**
     * Applies this function to the given argument.
     *
     * @param i the first function argument
     * @param d1 the second function argument
     * @param d2 the tri function argument
     * @return the function result
     */
    double apply(int i, double d1, double d2);
}


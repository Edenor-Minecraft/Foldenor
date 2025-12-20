package com.github.cao.awa.catheter.action;

@FunctionalInterface
public interface IntegerAndBiByteToByteFunction {

    /**
     * Applies this function to the given argument.
     *
     * @param i the first function argument
     * @param b1 the second function argument
     * @param b2 the tri function argument
     * @return the function result
     */
    byte apply(int i, byte b1, byte b2);
}


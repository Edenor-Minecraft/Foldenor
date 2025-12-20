package com.github.cao.awa.catheter.action;

@FunctionalInterface
public interface BiByteToByteFunction {

    /**
     * Applies this function to the given argument.
     *
     * @param b1 the first function argument
     * @param b2 the second function argument
     * @return the function result
     */
    byte applyAsByte(byte b1, byte b2);
}


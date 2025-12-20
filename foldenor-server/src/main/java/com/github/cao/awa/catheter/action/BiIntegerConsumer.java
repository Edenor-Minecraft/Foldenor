package com.github.cao.awa.catheter.action;

@FunctionalInterface
public interface BiIntegerConsumer {

    /**
     * Performs this operation on the given argument.
     *
     * @param i1 the first input argument
     * @param i2 the second input argument
     */
    void accept(int i1, int i2);
}

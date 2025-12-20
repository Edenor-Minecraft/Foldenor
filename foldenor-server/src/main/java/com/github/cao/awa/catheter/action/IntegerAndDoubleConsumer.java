package com.github.cao.awa.catheter.action;

@FunctionalInterface
public interface IntegerAndDoubleConsumer {

    /**
     * Performs this operation on the given argument.
     *
     * @param i the first input argument
     * @param d the second input argument
     */
    void accept(int i, double d);
}

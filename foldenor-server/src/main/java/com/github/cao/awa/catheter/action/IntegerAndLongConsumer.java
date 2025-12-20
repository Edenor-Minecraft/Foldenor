package com.github.cao.awa.catheter.action;

@FunctionalInterface
public interface IntegerAndLongConsumer {

    /**
     * Performs this operation on the given argument.
     *
     * @param i the first input argument
     * @param l the second input argument
     */
    void accept(int i, long l);
}

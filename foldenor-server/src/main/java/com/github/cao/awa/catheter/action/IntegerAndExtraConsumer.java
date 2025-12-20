package com.github.cao.awa.catheter.action;

@FunctionalInterface
public interface IntegerAndExtraConsumer<X> {

    /**
     * Performs this operation on the given argument.
     *
     * @param i the first input argument
     * @param x the second input argument
     */
    void accept(int i, X x);
}

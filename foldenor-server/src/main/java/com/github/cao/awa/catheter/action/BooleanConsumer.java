package com.github.cao.awa.catheter.action;

@FunctionalInterface
public interface BooleanConsumer {

    /**
     * Performs this operation on the given argument.
     *
     * @param b the input argument
     */
    void accept(boolean b);
}

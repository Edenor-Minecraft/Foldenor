package com.github.cao.awa.catheter.action;

@FunctionalInterface
public interface ByteConsumer {

    /**
     * Performs this operation on the given argument.
     *
     * @param b the input argument
     */
    void accept(byte b);
}

package com.github.cao.awa.catheter.action;

@FunctionalInterface
public interface ByteFunction<R> {

    /**
     * Applies this function to the given argument.
     *
     * @param t the function argument
     * @return the function result
     */
    R apply(byte t);
}


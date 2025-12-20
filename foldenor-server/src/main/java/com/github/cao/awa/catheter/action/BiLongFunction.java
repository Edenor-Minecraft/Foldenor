package com.github.cao.awa.catheter.action;

@FunctionalInterface
public interface BiLongFunction {

    /**
     * Applies this function to the given argument.
     *
     * @param l1 the first function argument
     * @param l2 the second function argument
     * @return the function result
     */
    long apply(long l1, long l2);
}


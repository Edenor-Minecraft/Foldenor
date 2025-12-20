package com.github.cao.awa.catheter.action;

@FunctionalInterface
public interface BytePredicate {

    /**
     * Evaluates this predicate on the given argument.
     *
     * @param b the input argument
     * @return {@code true} if the input argument matches the predicate,
     * otherwise {@code false}
     */
    boolean test(byte b);
}

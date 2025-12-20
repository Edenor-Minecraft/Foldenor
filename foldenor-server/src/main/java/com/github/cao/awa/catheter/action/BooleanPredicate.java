package com.github.cao.awa.catheter.action;

@FunctionalInterface
public interface BooleanPredicate {

    /**
     * Evaluates this predicate on the given argument.
     *
     * @param b the input argument
     * @return {@code true} if the input argument matches the predicate,
     * otherwise {@code false}
     */
    boolean test(boolean b);
}

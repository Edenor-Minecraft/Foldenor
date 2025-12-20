package com.github.cao.awa.catheter.action;

@FunctionalInterface
public interface BiDoublePredicate {

    /**
     * Evaluates this predicate on the given argument.
     *
     * @param d1 the first input argument
     * @param d2 the second input argument
     * @return {@code true} if the input argument matches the predicate,
     * otherwise {@code false}
     */
    boolean test(double d1, double d2);
}

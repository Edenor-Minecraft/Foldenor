package com.github.cao.awa.catheter.action;

@FunctionalInterface
public interface BiLongPredicate {

    /**
     * Evaluates this predicate on the given argument.
     *
     * @param l1 the first input argument
     * @param l2 the second input argument
     * @return {@code true} if the input argument matches the predicate,
     * otherwise {@code false}
     */
    boolean test(long l1, long l2);
}

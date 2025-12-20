package com.github.cao.awa.catheter.action;

@FunctionalInterface
public interface IntegerAndBiLongPredicate {

    /**
     * Evaluates this predicate on the given argument.
     *
     * @param i the first input argument
     * @param l1 the second input argument
     * @param l2 the tri input argument
     * @return {@code true} if the input argument matches the predicate,
     * otherwise {@code false}
     */
    boolean test(int i, long l1, long l2);
}

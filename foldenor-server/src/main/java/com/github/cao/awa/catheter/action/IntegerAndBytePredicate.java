package com.github.cao.awa.catheter.action;

@FunctionalInterface
public interface IntegerAndBytePredicate {

    /**
     * Evaluates this predicate on the given argument.
     *
     * @param i the first input argument
     * @param b1 the second input argument
     * @return {@code true} if the input argument matches the predicate,
     * otherwise {@code false}
     */
    boolean test(int i, byte b1);
}

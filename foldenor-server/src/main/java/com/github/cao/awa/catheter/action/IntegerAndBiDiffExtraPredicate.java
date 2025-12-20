package com.github.cao.awa.catheter.action;

@FunctionalInterface
public interface IntegerAndBiDiffExtraPredicate<X, Y> {

    /**
     * Evaluates this predicate on the given argument.
     *
     * @param i the first input argument
     * @param x the second input argument
     * @param y the tri input argument
     * @return {@code true} if the input argument matches the predicate,
     * otherwise {@code false}
     */
    boolean test(int i, X x, Y y);
}

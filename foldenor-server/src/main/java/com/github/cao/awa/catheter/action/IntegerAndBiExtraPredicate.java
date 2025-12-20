package com.github.cao.awa.catheter.action;

@FunctionalInterface
public interface IntegerAndBiExtraPredicate<X> {

    /**
     * Evaluates this predicate on the given argument.
     *
     * @param i the first input argument
     * @param x1 the second input argument
     * @param x2 the tri input argument
     * @return {@code true} if the input argument matches the predicate,
     * otherwise {@code false}
     */
    boolean test(int i, X x1, X x2);
}

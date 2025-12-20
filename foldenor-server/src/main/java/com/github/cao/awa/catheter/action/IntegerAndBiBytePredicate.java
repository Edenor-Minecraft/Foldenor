package com.github.cao.awa.catheter.action;

@FunctionalInterface
public interface IntegerAndBiBytePredicate {

    /**
     * Evaluates this predicate on the given argument.
     *
     * @param i the first input argument
     * @param b1 the second input argument
     * @param b2 the tri input argument
     * @return {@code true} if the input argument matches the predicate,
     * otherwise {@code false}
     */
    boolean test(int i, byte b1, byte b2);
}

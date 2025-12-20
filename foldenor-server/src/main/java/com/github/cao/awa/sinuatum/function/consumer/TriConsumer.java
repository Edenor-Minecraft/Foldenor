package com.github.cao.awa.sinuatum.function.consumer;

import java.util.Objects;

@FunctionalInterface
public interface TriConsumer<A, B, C> {
    /**
     * Performs this operation on the given arguments.
     *
     * @param a the first input argument
     * @param b the second input argument
     * @param c the third input argument
     */
    void accept(A a, B b, C c);

    /**
     * Returns a composed {@code Consumer} that performs, in sequence, this
     * operation followed by the {@code after} operation. If performing either
     * operation throws an exception, it is relayed to the caller of the
     * composed operation.  If performing this operation throws an exception,
     * the {@code after} operation will not be performed.
     *
     * @param after
     *         the operation to perform after this operation
     * @return a composed {@code Consumer} that performs in sequence this
     * operation followed by the {@code after} operation
     *
     */
    default TriConsumer<A, B, C> andThen(TriConsumer<A, B, C> after) {
        Objects.requireNonNull(after);
        return (a, b, c) -> {
            accept(a, b, c);
            after.accept(a, b, c);
        };
    }
}
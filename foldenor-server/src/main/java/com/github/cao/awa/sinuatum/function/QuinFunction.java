package com.github.cao.awa.sinuatum.function;

import java.util.Objects;
import java.util.function.Function;

@FunctionalInterface
public interface QuinFunction<A, B, C, D, E, R> {
    /**
     * Applies this function to the given arguments.
     *
     * @param a the first function argument
     * @param b the second function argument
     * @param c the third function argument
     * @param d the quad function argument
     * @param e the quin function argument
     * @return the function result
     */
    R apply(A a, B b, C c, D d, E e);

    /**
     * Returns a composed function that first applies this function to
     * its input, and then applies the {@code after} function to the result.
     * If evaluation of either function throws an exception, it is relayed to
     * the caller of the composed function.
     *
     * @param <V> the type of output of the {@code after} function, and of the
     *           composed function
     * @param after the function to apply after this function is applied
     * @return a composed function that first applies this function and then
     * applies the {@code after} function
     * @throws NullPointerException if after is null
     */
    default <V> QuinFunction<A, B, C, D, E, V> andThen(Function<? super R, ? extends V> after) {
        Objects.requireNonNull(after);
        return (a, b, c, d, e) -> after.apply(apply(a, b, c, d, e));
    }
}
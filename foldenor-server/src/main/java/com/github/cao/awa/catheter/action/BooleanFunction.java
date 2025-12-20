package com.github.cao.awa.catheter.action;

import java.util.Objects;
import java.util.function.Function;

@FunctionalInterface
public interface BooleanFunction<R> {

    /**
     * Applies this function to the given argument.
     *
     * @param t the function argument
     * @return the function result
     */
    R apply(boolean t);
}


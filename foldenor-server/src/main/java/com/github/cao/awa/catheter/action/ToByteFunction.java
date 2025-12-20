package com.github.cao.awa.catheter.action;

@FunctionalInterface
public interface ToByteFunction<X> {

    /**
     * Applies this function to the given argument.
     *
     * @param value the function argument
     * @return the function result
     */
    byte applyAsByte(X value);
}


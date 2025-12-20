package com.github.cao.awa.catheter.action;

@FunctionalInterface
public interface BooleanToByteFunction {

    /**
     * Applies this function to the given argument.
     *
     * @param value the function argument
     * @return the function result
     */
    byte applyAsByte(boolean value);
}


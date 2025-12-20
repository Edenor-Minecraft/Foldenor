package com.github.cao.awa.catheter.action;

@FunctionalInterface
public interface ByteUnaryOperator {
    /**
     * Applies this operator to the given operand.
     *
     * @param operand the operand
     * @return the operator result
     */
    byte applyAsByte(byte operand);
}

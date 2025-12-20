package com.github.cao.awa.catheter.receptacle;

public final class IntegerReceptacle {
    private int target;

    public IntegerReceptacle(int target) {
        this.target = target;
    }

    public static IntegerReceptacle of() {
        return of(0);
    }

    public static IntegerReceptacle of(int target) {
        return new IntegerReceptacle(target);
    }

    public int get() {
        return this.target;
    }

    public IntegerReceptacle set(int target) {
        this.target = target;
        return this;
    }

    public IntegerReceptacle set(IntegerReceptacle target) {
        this.target = target.get();
        return this;
    }
}

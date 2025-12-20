package com.github.cao.awa.catheter.receptacle;

public final class DoubleReceptacle {
    private double target;

    public DoubleReceptacle(double target) {
        this.target = target;
    }

    public static DoubleReceptacle of() {
        return of(0);
    }

    public static DoubleReceptacle of(double target) {
        return new DoubleReceptacle(target);
    }

    public double get() {
        return this.target;
    }

    public DoubleReceptacle set(double target) {
        this.target = target;
        return this;
    }

    public DoubleReceptacle set(DoubleReceptacle target) {
        this.target = target.get();
        return this;
    }
}

package com.github.cao.awa.catheter.receptacle;

public final class BooleanReceptacle {
    private boolean target;

    public BooleanReceptacle(boolean target) {
        this.target = target;
    }

    public static BooleanReceptacle of() {
        return of(false);
    }

    public static BooleanReceptacle of(boolean target) {
        return new BooleanReceptacle(target);
    }

    public BooleanReceptacle and(boolean target) {
        this.target = this.target && target;
        return this;
    }

    public BooleanReceptacle or(boolean target) {
        this.target = this.target || target;
        return this;
    }

    public boolean get() {
        return this.target;
    }

    public BooleanReceptacle set(boolean target) {
        this.target = target;
        return this;
    }

    public BooleanReceptacle set(BooleanReceptacle target) {
        this.target = target.get();
        return this;
    }
}

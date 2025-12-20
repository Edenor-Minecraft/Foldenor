package com.github.cao.awa.catheter.receptacle;

public final class LongReceptacle {
    private long target;

    public LongReceptacle(long target) {
        this.target = target;
    }

    public static LongReceptacle of() {
        return of(0);
    }

    public static LongReceptacle of(long target) {
        return new LongReceptacle(target);
    }

    public long get() {
        return this.target;
    }

    public LongReceptacle set(long target) {
        this.target = target;
        return this;
    }

    public LongReceptacle set(LongReceptacle target) {
        this.target = target.get();
        return this;
    }
}

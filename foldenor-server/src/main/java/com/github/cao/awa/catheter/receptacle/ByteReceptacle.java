package com.github.cao.awa.catheter.receptacle;

public final class ByteReceptacle {
    private byte target;

    public ByteReceptacle(byte target) {
        this.target = target;
    }

    public static ByteReceptacle of() {
        return of((byte) 0);
    }

    public static ByteReceptacle of(byte target) {
        return new ByteReceptacle(target);
    }

    public byte get() {
        return this.target;
    }

    public ByteReceptacle set(byte target) {
        this.target = target;
        return this;
    }

    public ByteReceptacle set(ByteReceptacle target) {
        this.target = target.get();
        return this;
    }
}

package com.github.cao.awa.catheter.matrix;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class MatrixPos implements Comparable<MatrixPos> {
    private final int x;
    private final int y;

    public MatrixPos(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (MatrixPos) obj;
        return this.x == that.x &&
                this.y == that.y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    @Override
    public String toString() {
        return "MatrixPos[" +
                "x=" + x + ", " +
                "y=" + y + ']';
    }

    @Override
    public int compareTo(@NotNull MatrixPos o) {
        return hashCode() - o.hashCode();
    }
}

package com.github.cao.awa.sepals.entity.ai.brain;

public interface DetailedDebuggableTask {
    String information();
    default boolean alwaysRunning() {
        return false;
    }
}
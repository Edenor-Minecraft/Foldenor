package com.github.cao.awa.sepals.entity.ai.task.composite;

import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;

public class SepalsTaskStatus {
    public static boolean isRunning(Behavior.Status status) {
        return status == Behavior.Status.RUNNING;
    }

    public static boolean isStopped(Behavior.Status status) {
        return status == Behavior.Status.STOPPED;
    }

    public static boolean isRunning(BehaviorControl<?> task) {
        return isRunning(task.getStatus());
    }

    public static boolean isStopped(BehaviorControl<?> task) {
        return isStopped(task.getStatus());
    }
}
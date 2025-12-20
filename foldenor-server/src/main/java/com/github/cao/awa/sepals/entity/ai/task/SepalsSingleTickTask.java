package com.github.cao.awa.sepals.entity.ai.task;

import com.github.cao.awa.sepals.entity.ai.brain.DetailedDebuggableTask;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.behavior.OneShot;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.VisibleForDebug;

import java.util.function.Predicate;

public abstract class SepalsSingleTickTask<E extends LivingEntity> extends OneShot<E> implements DetailedDebuggableTask {
    private ServerLevel currentWorld;
    private E currentEntity;
    private Brain<?> currentBrain;
    @VisibleForDebug
    private long currentTime;

    @Override
    public boolean trigger(ServerLevel world, E entity, long time) {
        this.currentWorld = world;
        this.currentEntity = entity;
        this.currentBrain = entity.getBrain();
        this.currentTime = time;
        return complete(world, entity, time);
    }

    public abstract boolean complete(ServerLevel world, E entity, long time);

    public <U> boolean require(MemoryModuleType<U> memoryType, Predicate<U> action) {
        return currentBrain().getMemory(memoryType).filter(action).isPresent();
    }

    public <U> void remember(MemoryModuleType<U> memoryType, U value) {
        currentBrain().setMemory(memoryType, value);
    }

    public ServerLevel currentWorld() {
        return this.currentWorld;
    }

    public E currentEntity() {
        return this.currentEntity;
    }

    public Brain<?> currentBrain() {
        return this.currentBrain;
    }

    public long currentTime() {
        return this.currentTime;
    }

    @Override
    public boolean alwaysRunning() {
        return true;
    }

    @Override
    public String toString() {
        return information();
    }
}
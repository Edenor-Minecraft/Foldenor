package com.github.cao.awa.sepals.entity.ai.task.wait;

import com.github.cao.awa.sepals.entity.ai.brain.DetailedDebuggableTask;
import com.github.cao.awa.sepals.entity.ai.task.composite.SepalsTaskStatus;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

import java.util.Set;

public class SepalsWaitTask implements BehaviorControl<LivingEntity>, DetailedDebuggableTask {
    private final int minRunTime;
    private final int maxRunTime;
    private Behavior.Status status = Behavior.Status.STOPPED;
    @VisibleForDebug
    private long currentTime;
    private long waitUntil;

    public SepalsWaitTask(int minRunTime, int maxRunTime) {
        this.minRunTime = minRunTime;
        this.maxRunTime = maxRunTime;
    }

    @Override
    public Behavior.Status getStatus() {
        return this.status;
    }

    @Override
    public Set<MemoryModuleType<?>> getRequiredMemories() {
        return Set.of();
    }

    @Override
    public final boolean tryStart(ServerLevel world, LivingEntity entity, long time) {
        this.status = Behavior.Status.RUNNING;
        int i = this.minRunTime + world.getRandom().nextInt(this.maxRunTime + 1 - this.minRunTime);
        this.waitUntil = time + i;
        return true;
    }

    @Override
    public final void tickOrStop(ServerLevel world, LivingEntity entity, long time) {
        this.currentTime = time;

        if (time > this.waitUntil) {
            doStop(world, entity, time);
        }
    }

    @Override
    public final void doStop(ServerLevel world, LivingEntity entity, long time) {
        this.status = Behavior.Status.STOPPED;
    }

    @Override
    public String debugString() {
        return this.getClass().getSimpleName();
    }

    @Override
    public String information() {
        if (SepalsTaskStatus.isStopped(this)) {
            return "WaitTask(STOPPED, runTime(max=" + this.maxRunTime + ", min=" + this.minRunTime + "))";
        } else {
            return "WaitTask(RUNNING, runTime(max=" + this.maxRunTime + ", min=" + this.minRunTime + ", remaining=" + (this.waitUntil - this.currentTime) + "))";
        }
    }
}
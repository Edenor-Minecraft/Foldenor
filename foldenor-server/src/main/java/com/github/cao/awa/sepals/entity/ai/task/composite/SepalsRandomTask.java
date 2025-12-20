package com.github.cao.awa.sepals.entity.ai.task.composite;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.server.level.ServerLevel;

import java.util.List;
import java.util.Map;

public class SepalsRandomTask<E extends LivingEntity> extends SepalsCompositeTask<E> {
    public SepalsRandomTask(List<Pair<? extends BehaviorControl<? super E>, Integer>> tasks) {
        this(ImmutableMap.of(), tasks);
    }

    public SepalsRandomTask(
            Map<MemoryModuleType<?>, MemoryStatus> requiredMemoryState,
            List<Pair<? extends BehaviorControl<? super E>, Integer>> tasks
    ) {
        super(requiredMemoryState, ImmutableSet.of(), Order.SHUFFLED, RunMode.RUN_ONE, tasks);
    }

    @Override
    public final boolean tryStart(ServerLevel world, E entity, long time) {
        if (shouldStart(entity)) {
            this.status = Behavior.Status.RUNNING;
            this.tasks.shuffle();
            this.tasks.elements()
                    .filter(SepalsTaskStatus::isStopped)
                    .till(task -> task.tryStart(world, entity, time));
            return true;
        } else {
            return false;
        }
    }
}
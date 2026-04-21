package com.github.cao.awa.sepals.entity.ai.task.composite;

import com.github.cao.awa.catheter.Catheter;
import com.github.cao.awa.sepals.entity.ai.brain.DetailedDebuggableTask;
import com.github.cao.awa.sepals.entity.ai.brain.TaskDelegate;
import com.github.cao.awa.sepals.weight.WeightedList;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class SepalsCompositeTask<E extends LivingEntity> implements BehaviorControl<E>, TaskDelegate<E>, DetailedDebuggableTask {
    private final Map<MemoryModuleType<?>, MemoryStatus> requiredMemoryState;
    private final Set<MemoryModuleType<?>> memoriesToForgetWhenStopped;
    protected final Order order;
    protected final RunMode runMode;
    protected final WeightedList<BehaviorControl<? super E>> tasks = new WeightedList<>();
    protected Behavior.Status status = Behavior.Status.STOPPED;

    public SepalsCompositeTask(
            Map<MemoryModuleType<?>, MemoryStatus> requiredMemoryState,
            Set<MemoryModuleType<?>> memoriesToForgetWhenStopped,
            Order order,
            RunMode runMode,
            List<Pair<? extends BehaviorControl<? super E>, Integer>> tasks
    ) {
        this.requiredMemoryState = requiredMemoryState;
        this.memoriesToForgetWhenStopped = memoriesToForgetWhenStopped;
        this.order = order;
        this.runMode = runMode;
        tasks.forEach(task -> this.tasks.add(task.getFirst(), task.getSecond()));
    }

    boolean shouldStart(E entity) {
        for(Map.Entry<MemoryModuleType<?>, MemoryStatus> entry : this.requiredMemoryState.entrySet()) {
            MemoryModuleType<?> memoryModuleType = entry.getKey();
            MemoryStatus memoryModuleState = entry.getValue();
            if (!entity.getBrain().checkMemory(memoryModuleType, memoryModuleState)) {
                return false;
            }
        }

        return true;
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
    public boolean tryStart(@NotNull ServerLevel world, E entity, long time) {
        if (this.shouldStart(entity)) {
            this.status = Behavior.Status.RUNNING;
            this.order.apply(this.tasks);
            this.runMode.run(this.tasks.elements(), world, entity, time);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public final void tickOrStop(ServerLevel world, E entity, long time) {
        this.tasks.elements()
                .filter(SepalsTaskStatus::isRunning)
                .ifEmpty(x -> doStop(world, entity, time))
                .each(task -> task.tickOrStop(world, entity, time));
    }

    @Override
    public final void doStop(ServerLevel world, E entity, long time) {
        this.status = Behavior.Status.STOPPED;
        this.tasks.elements().filter(SepalsTaskStatus::isRunning).each(task -> task.doStop(world, entity, time));
        this.memoriesToForgetWhenStopped.forEach(entity.getBrain()::eraseMemory);
    }

    @Override
    public String debugString() {
        return getClass().getSimpleName();
    }

    public String toString() {
        Set<? extends BehaviorControl<? super E>> set = this.tasks.elements().filter(SepalsTaskStatus::isRunning).set();
        return "(" + debugString() + "): " + set;
    }

    @Override
    public Catheter<BehaviorControl<? super E>> sepals$tasks() {
        return this.tasks.elements();
    }

    @Override
    public String information() {
        String orderMode = this.order == Order.SHUFFLED ? "SHUFFLED" : "ORDERED";
        String runMode = this.runMode == RunMode.RUN_ONE ? "PICK ONCE" : "RUN ALL";

        return "CompositeTask(" + this.status + ", " + orderMode + ", " + runMode + ", tasks(count=" + this.tasks.size() + "))";
    }

    public enum Order {
        ORDERED(list -> {
        }),
        SHUFFLED(WeightedList::shuffle);

        private final Consumer<WeightedList<?>> listModifier;

        Order(final Consumer<WeightedList<?>> listModifier) {
            this.listModifier = listModifier;
        }

        public void apply(WeightedList<?> list) {
            this.listModifier.accept(list);
        }
    }

    public enum RunMode {
        RUN_ONE {
            @Override
            public <E extends LivingEntity> void run(Catheter<BehaviorControl<? super E>> tasks, ServerLevel world, E entity, long time) {
                tasks.filter(SepalsTaskStatus::isStopped)
                        .till(task -> task.tryStart(world, entity, time));
            }
        },
        TRY_ALL {
            @Override
            public <E extends LivingEntity> void run(Catheter<BehaviorControl<? super E>> tasks, ServerLevel world, E entity, long time) {
                tasks.filter(SepalsTaskStatus::isStopped)
                        .each(task -> task.tryStart(world, entity, time));
            }
        };

        public abstract <E extends LivingEntity> void run(Catheter<BehaviorControl<? super E>> tasks, ServerLevel world, E entity, long time);
    }
}
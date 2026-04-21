package com.github.cao.awa.sepals.entity.ai.task.look;

import com.github.cao.awa.sepals.entity.ai.cache.SepalsLivingTargetCache;
import com.github.cao.awa.sepals.entity.ai.task.SepalsSingleTickTask;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.server.level.ServerLevel;

import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

public class SepalsLookAtPlayerTask extends SepalsSingleTickTask<LivingEntity> {
    private final float maxDistance;

    public SepalsLookAtPlayerTask(float maxDistance) {
        this.maxDistance = maxDistance;
    }

    public static SepalsLookAtPlayerTask create(float maxDistance) {
        return new SepalsLookAtPlayerTask(maxDistance);
    }

    @Override
    public boolean complete(ServerLevel world, LivingEntity entity, long time) {
        return require(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES, cache -> {
            Predicate<LivingEntity> distance = target -> target.distanceToSqr(entity) <= this.maxDistance;

            Optional<? extends LivingEntity> optional;
            if (cache instanceof SepalsLivingTargetCache sepalsCache) {
                optional = entity.isVehicle() ?
                        sepalsCache.findFirstPlayer(distance::test, target -> !entity.hasPassenger(target)) :
                        sepalsCache.findFirstPlayer(distance::test);
            } else {
                optional = entity.isVehicle() ?
                        cache.findClosest(target -> distance.test(target) && !entity.hasPassenger(target)) :
                        cache.findClosest(distance);
            }

            if (optional.isEmpty()) {
                return false;
            } else {
                remember(MemoryModuleType.LOOK_TARGET, new EntityTracker(optional.get(), true));
                return true;
            }
        });
    }

    @Override
    public String information() {
        return "LookAtPlayerTask(distance=" + this.maxDistance + ")";
    }

    @Override
    public Set<MemoryModuleType<?>> getRequiredMemories() {
        return Set.of();
    }
}
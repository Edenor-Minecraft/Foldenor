package com.github.cao.awa.sepals.entity.ai.task.rest;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;

public class SepalsWakeUpTask {
    public static BehaviorControl<LivingEntity> create() {
        return BehaviorBuilder.create(context -> context.point((world, entity, time) -> {
            if (!entity.isSleeping() || entity.getBrain().isActive(Activity.REST)) {
                return false;
            } else {
                entity.stopSleeping();
                return true;
            }
        }));
    }
}
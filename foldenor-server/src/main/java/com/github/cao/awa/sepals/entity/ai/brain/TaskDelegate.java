package com.github.cao.awa.sepals.entity.ai.brain;

import com.github.cao.awa.catheter.Catheter;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;

public interface TaskDelegate<E extends LivingEntity> {
    Catheter<BehaviorControl<? super E>> sepals$tasks();
}
package net.edenor.foldenor.util;

import io.netty.util.internal.ThreadLocalRandom;
import net.edenor.foldenor.config.FoldenorConfig;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import java.util.Objects;
import java.util.regex.Pattern;

public class FoldenorUtils {
    public static Boolean pickedItems = false; //Let Me Despawn

    public static boolean rollPercentChance(float percent) {
        return percent != 0.0 && ThreadLocalRandom.current().nextFloat() <= (percent / 100.0);
    }

    //Let Me Despawn start
    public static void setPersistence(Mob entity, EquipmentSlot slot) {
        if (!FoldenorConfig.lmdEnabled) return;
        ItemStack itemStack = entity.getItemBySlot(slot);
        CustomData component = itemStack.get(DataComponents.CUSTOM_DATA);
        CompoundTag nbt;
        if (component != null) {
            nbt = component.copyTag();
        } else {
            nbt = new CompoundTag();
        }
        nbt.putBoolean("picked", true);
        itemStack.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));
        pickedItems = true;
        entity.persistenceRequired = FoldenorConfig.mobNames.contains(Objects.requireNonNull(entity.level().registryAccess().lookupOrThrow(Registries.ENTITY_TYPE).getKey(entity.getType())).toString()) || !hasDespawnableName(entity);
    }

    public static boolean hasDespawnableName(Mob entity) {
        if(entity.hasCustomName()) {
            return matchesStackedName(entity.getCustomName().getString(), entity);
        }
        return true;
    }

    public static boolean matchesStackedName(String customName, Mob entity) {
        return Pattern.compile(Pattern.quote(getLocalizedEntityName(entity.getType()).getString()) + " x\\d+").matcher(customName).find();
    }

    public static Component getLocalizedEntityName(EntityType<?> entityType) {
        String translationKey = entityType.getDescriptionId();
        return Component.translatable(translationKey);
    }

    public static void dropEquipmentOnDiscard(LivingEntity entity) {
        if (!FoldenorConfig.lmdEnabled) return;
        for (EquipmentSlot equipmentSlot : EquipmentSlot.values()) {
            ItemStack itemStack = entity.getItemBySlot(equipmentSlot);
            if (!itemStack.isEmpty() && itemStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).contains("picked") && !EnchantmentHelper.has(itemStack, EnchantmentEffectComponents.PREVENT_EQUIPMENT_DROP)) {
                entity.spawnAtLocation((ServerLevel) entity.level(), itemStack);
                entity.setItemSlot(equipmentSlot, ItemStack.EMPTY);
            }
        }
    }
    //Let Me Despawn end
}
package com.github.cao.awa.sepals.entity.ai.cache;

import com.github.cao.awa.catheter.Catheter;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class SepalsLivingTargetCache extends NearestVisibleLivingEntities {
    private final LivingEntity[] entities;
    private final Player[] players;
    private final Object2BooleanOpenHashMap<LivingEntity> directSuccess;
    private final Predicate<LivingEntity> compute;

    @SuppressWarnings("unchecked")
    public SepalsLivingTargetCache(ServerLevel world, LivingEntity owner, LivingEntity[] entities, Player[] players) {
        super(world, owner, Collections.EMPTY_LIST);
        this.entities = entities;
        this.players = players;
        this.directSuccess = new Object2BooleanOpenHashMap<>(entities.length);
        Predicate<LivingEntity> predicate = entity -> Sensor.isEntityTargetable(world, owner, entity);
        this.compute = entity -> this.directSuccess.computeIfAbsent(entity, predicate);
    }

    public Optional<LivingEntity> findFirst(Predicate<LivingEntity> predicate) {
        LivingEntity[] entities = this.entities;

        for (LivingEntity entity : entities) {
            if (predicate.test(entity) && this.compute.test(entity)) {
                return Optional.of(entity);
            }
        }

        return Optional.empty();
    }

    public Optional<Player> findFirstPlayer(Predicate<Player> predicate) {
        return findFirstPlayer(predicate, () -> true);
    }

    public Optional<Player> findFirstPlayer(Predicate<Player> predicate, BooleanSupplier canTargetPredicateWhen) {
        Player[] players = this.players;

        for (Player player : players) {
            if (predicate.test(player) && canTargetPredicateWhen.getAsBoolean() && this.compute.test(player)) {
                return Optional.of(player);
            }
        }

        return Optional.empty();
    }

    public Optional<Player> findFirstPlayer(Predicate<Player> predicate, Predicate<Player> canTargetPredicateWhen) {
        Player[] players = this.players;

        for (Player player : players) {
            if (predicate.test(player) && canTargetPredicateWhen.test(player) && this.compute.test(player)) {
                return Optional.of(player);
            }
        }

        return Optional.empty();
    }

    public Iterable<LivingEntity> iterate(Predicate<LivingEntity> predicate) {
        return () -> stream(predicate).iterator();
    }

    public Stream<LivingEntity> stream(Predicate<LivingEntity> predicate) {
        return Arrays.stream(this.entities).filter(entity -> predicate.test(entity) && this.compute.test(entity));
    }

    public List<LivingEntity> collect(Predicate<LivingEntity> predicate) {
        return Catheter.of(this.entities).filterTo(entity -> predicate.test(entity) && this.compute.test(entity)).list();
    }

    public boolean contains(@NotNull LivingEntity target) {
        LivingEntity[] entities = this.entities;
        for (LivingEntity entity : entities) {
            if (entity == target) {
                return this.compute.test(entity);
            }
        }

        return false;
    }

    public boolean anyMatch(Predicate<LivingEntity> predicate) {
        LivingEntity[] entities = this.entities;

        for (LivingEntity entity : entities) {
            if (predicate.test(entity) && this.compute.test(entity)) {
                return true;
            }
        }

        return false;
    }
}
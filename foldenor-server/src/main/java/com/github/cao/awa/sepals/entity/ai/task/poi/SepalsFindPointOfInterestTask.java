package com.github.cao.awa.sepals.entity.ai.task.poi;

import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.longs.Long2ObjectFunction;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.behavior.*;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import com.github.cao.awa.sepals.world.poi.SepalsPointOfInterestStorage;
import org.apache.commons.lang3.mutable.MutableLong;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

public class SepalsFindPointOfInterestTask {
    public static BehaviorControl<PathfinderMob> create(Predicate<Holder<PoiType>> poiPredicate, MemoryModuleType<GlobalPos> poiPosModule, boolean onlyRunIfChild, Optional<Byte> entityStatus, BiPredicate<ServerLevel, BlockPos> worldPosBiPredicate) {
        return create(poiPredicate, poiPosModule, poiPosModule, onlyRunIfChild, entityStatus, worldPosBiPredicate);
    }

    public static BehaviorControl<PathfinderMob> create(Predicate<Holder<PoiType>> poiPredicate, MemoryModuleType<GlobalPos> poiPosModule, boolean onlyRunIfChild, Optional<Byte> entityStatus) {
        return create(poiPredicate, poiPosModule, poiPosModule, onlyRunIfChild, entityStatus, (world, pos) -> true);
    }

    public static BehaviorControl<PathfinderMob> create(
            Predicate<Holder<PoiType>> poiPredicate,
            MemoryModuleType<GlobalPos> poiPosModule,
            MemoryModuleType<GlobalPos> potentialPoiPosModule,
            boolean onlyRunIfChild,
            Optional<Byte> entityStatus, BiPredicate<ServerLevel, BlockPos> worldPosBiPredicate
    ) {
        MutableLong mutableLong = new MutableLong(0L);
        Long2ObjectMap<RetryMarker> retryMarkers = new Long2ObjectOpenHashMap<>();
        OneShot<PathfinderMob> singleTickTask = BehaviorBuilder.create(
                taskContext -> taskContext.group(taskContext.absent(potentialPoiPosModule))
                        .apply(
                                taskContext,
                                queryResult -> (world, entity, time) -> {
                                    if (onlyRunIfChild && entity.isBaby()) {
                                        return false;
                                    } else if (mutableLong.getValue() == 0L) {
                                        mutableLong.setValue(world.getRedstoneGameTime() + (long) world.random.nextInt(20));
                                        return false;
                                    } else if (world.getRedstoneGameTime() < mutableLong.getValue()) {
                                        return false;
                                    } else {
                                        mutableLong.setValue(time + 20L + (long) world.getRandom().nextInt(20));
                                        PoiManager pointOfInterestStorage = world.getPoiManager();
                                        retryMarkers.long2ObjectEntrySet().removeIf(entry -> !entry.getValue().isAttempting(time));
                                        Predicate<BlockPos> predicate2 = pos -> {
                                            RetryMarker retryMarker = retryMarkers.get(pos.asLong());
                                            if (retryMarker == null) {
                                                return true;
                                            } else if (!retryMarker.shouldRetry(time)) {
                                                return false;
                                            } else {
                                                retryMarker.setAttemptTime(time);
                                                return true;
                                            }
                                        };
                                        Pair<Holder<PoiType>, BlockPos>[] poiPoses = SepalsPointOfInterestStorage.getSortedTypesAndPositions(
                                                        pointOfInterestStorage,
                                                        poiPredicate, predicate2, entity.blockPosition(), 48, PoiManager.Occupancy.HAS_SPACE
                                                )
                                                .holdTill(5)
                                                .distinct()
                                                .safeArray();

                                        Path path = findPathToPoi(entity, poiPoses);
                                        if (path == null || !path.canReach()) {
                                            for (Pair<Holder<PoiType>, BlockPos> pair : poiPoses) {
                                                retryMarkers.computeIfAbsent(
                                                        pair.getSecond().asLong(),
                                                        x -> new RetryMarker(world.random, time)
                                                );
                                            }
                                        } else {
                                            BlockPos blockPos = path.getTarget();
                                            pointOfInterestStorage.getType(blockPos).ifPresent(poiType -> {
                                                SepalsPointOfInterestStorage.getPosition(
                                                        pointOfInterestStorage,
                                                        poiPredicate,
                                                        (Holder, blockPos2) -> blockPos2.equals(blockPos),
                                                        blockPos,
                                                        1
                                                );
                                                queryResult.set(GlobalPos.of(world.dimension(), blockPos));
                                                entityStatus.ifPresent(status -> world.broadcastEntityEvent(entity, status));
                                                retryMarkers.clear();
                                                world.debugSynchronizers().updatePoi(blockPos);
                                            });
                                        }

                                        return true;
                                    }
                                }
                        )
        );
        return potentialPoiPosModule == poiPosModule
                ? singleTickTask
                : BehaviorBuilder.create(context -> context.group(context.absent(poiPosModule)).apply(context, poiPos -> singleTickTask));
    }

    @Nullable
    public static Path findPathToPoi(Mob entity, Pair<Holder<PoiType>, BlockPos>[] pois) {
        if (pois.length == 0) {
            return null;
        } else {
            Set<BlockPos> set = new HashSet<>();
            int i = 1;

            for (Pair<Holder<PoiType>, BlockPos> pair : pois) {
                i = Math.max(i, pair.getFirst().value().validRange());
                set.add(pair.getSecond());
            }

            return entity.getNavigation().createPath(set, i);
        }
    }

    static class RetryMarker {
        private final RandomSource random;
        private long previousAttemptAt;
        private long nextScheduledAttemptAt;
        private int currentDelay;

        RetryMarker(RandomSource random, long time) {
            this.random = random;
            this.setAttemptTime(time);
        }

        public void setAttemptTime(long time) {
            this.previousAttemptAt = time;
            int i = this.currentDelay + this.random.nextInt(40) + 40;
            this.currentDelay = Math.min(i, 400);
            this.nextScheduledAttemptAt = time + (long) this.currentDelay;
        }

        public boolean isAttempting(long time) {
            return time - this.previousAttemptAt < 400L;
        }

        public boolean shouldRetry(long time) {
            return time >= this.nextScheduledAttemptAt;
        }

        public String toString() {
            return "RetryMarker{, previousAttemptAt="
                    + this.previousAttemptAt
                    + ", nextScheduledAttemptAt="
                    + this.nextScheduledAttemptAt
                    + ", currentDelay="
                    + this.currentDelay
                    + "}";
        }
    }
}
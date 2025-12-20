package com.github.cao.awa.sepals.world.poi;

import com.github.cao.awa.catheter.Catheter;
import com.mojang.datafixers.util.Function4;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Holder;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.ai.village.poi.PoiSection;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiType;

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("unchecked")
public class SepalsPointOfInterestStorage {
    public static Function4<
            PoiManager,
            Predicate<Holder<PoiType>>,
            ChunkPos,
            PoiManager.Occupancy,
            Stream<PoiRecord>
            > getInChunkFunction = SepalsPointOfInterestStorage::sepalsGetInChunk;

    static  {
        getInChunkFunction = PoiManager::getInChunk;
    }

    public static Stream<PoiRecord> sepalsGetInChunk(PoiManager storage, Predicate<Holder<PoiType>> typePredicate, ChunkPos chunkPos, PoiManager.Occupancy occupationStatus) {
        return ((RegionBasedStorageSectionExtended<PoiSection>) storage).sepals$getInChunk(typePredicate, chunkPos, occupationStatus);
    }

    public static Catheter<PoiRecord> getInSquare(
            PoiManager storage,
            Predicate<Holder<PoiType>> typePredicate,
            BlockPos pos,
            int radius,
            PoiManager.Occupancy occupationStatus
    ) {
        int i = Math.floorDiv(radius, 16) + 1;
        return Catheter.of(ChunkPos.rangeClosed(new ChunkPos(pos), i).flatMap((chunkPos) -> getInChunk(storage, typePredicate, chunkPos, occupationStatus)).filter((poi) -> {
            BlockPos blockPos2 = poi.getPos();
            return Math.abs(blockPos2.getX() - pos.getX()) <= radius && Math.abs(blockPos2.getZ() - pos.getZ()) <= radius;
        }).collect(Collectors.toSet()));
    }

    @VisibleForDebug
    public static Stream<PoiRecord> getInChunk(
            PoiManager storage,
            Predicate<Holder<PoiType>> typePredicate,
            ChunkPos chunkPos,
            PoiManager.Occupancy occupationStatus
    ) {
        return getInChunkFunction.apply(storage, typePredicate, chunkPos, occupationStatus);
    }

    public static Catheter<PoiRecord> get(
            PoiSection set,
            Predicate<Holder<PoiType>> predicate,
            PoiManager.Occupancy occupationStatus
    ) {
        return Catheter.of(set.byType.entrySet())
                .filter(predicate, Map.Entry::getKey)
                .collectionFlatTo(Map.Entry::getValue)
                .filter(occupationStatus.getTest());
    }

    public static Catheter<PoiRecord> getInCircle(
            PoiManager storage,
            Predicate<Holder<PoiType>> typePredicate,
            BlockPos pos,
            int radius,
            PoiManager.Occupancy occupationStatus
    ) {
        double i = radius * radius;
        return getInSquare(storage, typePredicate, pos, radius, occupationStatus)
                .discard(poiPos -> poiPos.distSqr(pos) > i, PoiRecord::getPos);
    }

    public static Catheter<BlockPos> getPositions(
            PoiManager storage,
            Predicate<Holder<PoiType>> typePredicate,
            Predicate<BlockPos> posPredicate,
            BlockPos pos,
            int radius,
            PoiManager.Occupancy occupationStatus
    ) {
        return getInCircle(storage, typePredicate, pos, radius, occupationStatus)
                .varyTo(PoiRecord::getPos)
                .filter(posPredicate);
    }

    public static Catheter<Pair<Holder<PoiType>, BlockPos>> getTypesAndPositions(
            PoiManager storage,
            Predicate<Holder<PoiType>> typePredicate,
            Predicate<BlockPos> posPredicate,
            BlockPos pos,
            int radius,
            PoiManager.Occupancy occupationStatus
    ) {
        return getInCircle(storage, typePredicate, pos, radius, occupationStatus)
                .filter(posPredicate, PoiRecord::getPos)
                .varyTo((PoiRecord poi) -> Pair.of(poi.getPoiType(), poi.getPos()))
                .arrayGenerator(Pair[]::new);
    }

    public static Catheter<Pair<Holder<PoiType>, BlockPos>> getSortedTypesAndPositions(
            PoiManager storage,
            Predicate<Holder<PoiType>> typePredicate,
            Predicate<BlockPos> posPredicate,
            BlockPos pos,
            int radius,
            PoiManager.Occupancy occupationStatus
    ) {
        return getTypesAndPositions(storage, typePredicate, posPredicate, pos, radius, occupationStatus)
                .sort(Comparator.comparingDouble(pair -> pair.getSecond().distSqr(pos)));
    }

    public static Optional<BlockPos> getPosition(
            PoiManager storage,
            Predicate<Holder<PoiType>> typePredicate,
            Predicate<BlockPos> posPredicate,
            BlockPos pos,
            int radius,
            PoiManager.Occupancy occupationStatus
    ) {
        return Optional.ofNullable(
                getPositions(storage, typePredicate, posPredicate, pos, radius, occupationStatus)
                        .findFirst(x -> true)
        );
    }

    public static Optional<BlockPos> getNearestPosition(
            PoiManager storage,
            Predicate<Holder<PoiType>> typePredicate,
            BlockPos pos,
            int radius, PoiManager.Occupancy occupationStatus
    ) {
        return Optional.of(
                getInCircle(storage, typePredicate, pos, radius, occupationStatus)
                        .varyTo(PoiRecord::getPos)
                        .min(Comparator.comparingDouble(blockPos2 -> blockPos2.distSqr(pos)))
        );
    }

    public static Optional<Pair<Holder<PoiType>, BlockPos>> getNearestTypeAndPosition(
            PoiManager storage,
            Predicate<Holder<PoiType>> typePredicate,
            BlockPos pos, int radius, PoiManager.Occupancy occupationStatus
    ) {
        return Optional.ofNullable(
                        getInCircle(storage, typePredicate, pos, radius, occupationStatus)
                                .min(Comparator.comparingDouble(poi -> poi.getPos().distSqr(pos)))
                )
                .map(poi -> Pair.of(poi.getPoiType(), poi.getPos()));
    }

    public static Optional<BlockPos> getNearestPosition(
            PoiManager storage,
            Predicate<Holder<PoiType>> typePredicate,
            Predicate<BlockPos> posPredicate,
            BlockPos pos,
            int radius,
            PoiManager.Occupancy occupationStatus
    ) {
        return Optional.of(
                getInCircle(storage, typePredicate, pos, radius, occupationStatus)
                        .varyTo(PoiRecord::getPos)
                        .filter(posPredicate)
                        .min(Comparator.comparingDouble(blockPos2 -> blockPos2.distSqr(pos)))
        );
    }

    public static Optional<BlockPos> getPosition(
            PoiManager storage,
            Predicate<Holder<PoiType>> typePredicate,
            BiPredicate<Holder<PoiType>, BlockPos> biPredicate,
            BlockPos pos,
            int radius
    ) {
        return Optional.ofNullable(
                getInCircle(storage, typePredicate, pos, radius, PoiManager.Occupancy.HAS_SPACE)
                        .filter(poi -> biPredicate.test(poi.getPoiType(), poi.getPos()))
                        .findFirst(x -> true)
        ).map(poi -> {
            poi.acquireTicket();
            return poi.getPos();
        });
    }

    public static Optional<BlockPos> getPosition(
            PoiManager storage,
            Predicate<Holder<PoiType>> typePredicate,
            Predicate<BlockPos> positionPredicate,
            PoiManager.Occupancy occupationStatus,
            BlockPos pos,
            int radius,
            RandomSource random
    ) {
        Catheter<PoiRecord> catheter = getInCircle(storage, typePredicate, pos, radius, occupationStatus);
        catheter.shuffle(random::nextLong);
        return Optional.ofNullable(
                catheter.filter(poi -> positionPredicate.test(poi.getPos()))
                        .findFirst(x -> true)
        ).map(PoiRecord::getPos);
    }

    public static <T> void shuffle(T[] elements, RandomSource random) {
        int i = elements.length;

        for (int j = i; j > 1; --j) {
            int swapTo = random.nextInt(j);
            int swapFrom = j - 1;
            T fromElement = elements[swapFrom];
            T toElement = elements[swapTo];
            elements[swapTo] = fromElement;
            elements[swapFrom] = toElement;
        }
    }

    /**
     * Preloads chunks in a square area with the given radius. Loads the chunks with {@code ChunkStatus.EMPTY}.
     *
     * @param radius the radius in blocks
     */
    public static void preloadChunks(PoiManager storage, LevelReader world, BlockPos pos, int radius) {
        Catheter.of(SectionPos.aroundChunk(
                                new ChunkPos(pos),
                                Math.floorDiv(radius, 16),
                                storage.levelHeightAccessor.getMinSectionY(),
                                storage.levelHeightAccessor.getMaxSectionY()
                        ).toArray(SectionPos[]::new)
                )
                .varyTo(sectionPos -> Pair.of(sectionPos, storage.get(sectionPos.asLong())))
                .discard(pair -> pair.getSecond().map(PoiSection::isValid).orElse(false))
                .varyTo(pair -> pair.getFirst().chunk())
                .filter(chunkPos -> storage.loadedChunks.add(chunkPos.toLong()))
                .each(chunkPos -> world.getChunk(chunkPos.x, chunkPos.z, ChunkStatus.EMPTY));
    }
}
package com.github.cao.awa.sepals.world.poi;

import com.github.cao.awa.catheter.Catheter;
import net.minecraft.core.Holder;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiType;

import java.util.function.Predicate;
import java.util.stream.Stream;

public interface RegionBasedStorageSectionExtended<R> {
    Catheter<R> sepals$getWithinChunkColumn(int x, int z);
    Stream<PoiRecord> sepals$getInChunk(Predicate<Holder<PoiType>> typePredicate, ChunkPos chunkPos, PoiManager.Occupancy occupationStatus);
}
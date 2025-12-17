package net.caffeinemc.mods.lithium.mixin.ai.poi;

import com.google.common.collect.AbstractIterator;
import net.caffeinemc.mods.lithium.common.util.Distances;
import net.caffeinemc.mods.lithium.common.world.interests.PointOfInterestSetExtended;
import net.caffeinemc.mods.lithium.common.world.interests.iterator.SinglePointOfInterestTypeFilter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.ai.village.poi.PoiSection;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import org.spongepowered.asm.mixin.*;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

@Mixin(PoiSection.class)
public abstract class PoiSectionMixin implements PointOfInterestSetExtended {
    @Mutable
    @Shadow
    @Final
    private Map<Holder<PoiType>, Set<PoiRecord>> byType;

    // Must not replace the HashMap and HashSet with optimized collections because iteration order is detectable ingame.

    @Shadow
    protected abstract Optional<PoiRecord> getPoiRecord(BlockPos blockPos);

    @Override
    public void lithium$collectMatchingPoints(Predicate<Holder<PoiType>> type, PoiManager.Occupancy status, Consumer<PoiRecord> consumer) {
        if (type instanceof SinglePointOfInterestTypeFilter) {
            this.collectWithSingleTypeFilter(((SinglePointOfInterestTypeFilter) type).getType(), status, consumer);
        } else {
            this.collectWithDynamicTypeFilter(type, status, consumer);
        }
    }

    @Override
    public PoiRecord lithium$getL2ClosestMatchingPoint(BlockPos center, Predicate<Holder<PoiType>> typeFilter, Predicate<? super PoiRecord> poiPredicate) {
        if (typeFilter instanceof SinglePointOfInterestTypeFilter singleTypeFilter) {
            return this.getL2ClosestMatchingPoint(center, singleTypeFilter.getType(), poiPredicate);
        } else {
            return this.getL2ClosestMatchingPoint(center, typeFilter, poiPredicate);
        }
    }


    @Unique
    private PoiRecord getL2ClosestMatchingPoint(BlockPos center, Holder<PoiType> type, Predicate<? super PoiRecord> poiPredicate) {
        Set<PoiRecord> poiRecords = this.byType.get(type);
        if (poiRecords == null || poiRecords.isEmpty()) {
            return null;
        }
        PoiRecord closest = null;
        long closestDistanceSq = Long.MAX_VALUE;
        for (PoiRecord poiRecord : poiRecords) {
            long distanceSq = Distances.distanceSq(center, poiRecord.getPos());
            if (distanceSq < closestDistanceSq && poiPredicate.test(poiRecord)) {
                closestDistanceSq = distanceSq;
                closest = poiRecord;
            }
        }
        return closest;
    }

    @Unique
    private PoiRecord getL2ClosestMatchingPoint(BlockPos center, Predicate<Holder<PoiType>> typeFilter, Predicate<? super PoiRecord> poiPredicate) {
        PoiRecord closest = null;
        long closestDistanceSq = Long.MAX_VALUE;

        for (Map.Entry<Holder<PoiType>, Set<PoiRecord>> entry : this.byType.entrySet()) {
            if (!typeFilter.test(entry.getKey()) || entry.getValue().isEmpty()) {
                continue;
            }

            for (PoiRecord poiRecord : entry.getValue()) {
                long distanceSq = Distances.distanceSq(center, poiRecord.getPos());
                if (distanceSq < closestDistanceSq && poiPredicate.test(poiRecord)) {
                    closestDistanceSq = distanceSq;
                    closest = poiRecord;
                }
            }
        }

        return closest;
    }


    @Override
    public void lithium$collectMatchingPointsL2Limited(BlockPos center, long maxDistanceSq, Predicate<Holder<PoiType>> typeFilter, Predicate<? super PoiRecord> poiPredicate, Consumer<PoiRecord> consumer, int limit) {
        if (typeFilter instanceof SinglePointOfInterestTypeFilter singleTypeFilter) {
            this.collectMatchingPointsL2Limited(center, maxDistanceSq, singleTypeFilter.getType(), poiPredicate, consumer, limit);
        } else {
            this.collectMatchingPointsL2Limited(center, maxDistanceSq, typeFilter, poiPredicate, consumer, limit);
        }
    }

    @Unique
    private void collectMatchingPointsL2Limited(BlockPos center, long maxDistanceSq, Holder<PoiType> type, Predicate<? super PoiRecord> poiPredicate, Consumer<PoiRecord> consumer, int limit) {
        Set<PoiRecord> poiRecords = this.byType.get(type);
        if (poiRecords == null || poiRecords.isEmpty()) {
            return;
        }

        for (PoiRecord poiRecord : poiRecords) {
            long distanceSq = Distances.distanceSq(center, poiRecord.getPos());
            if (distanceSq <= maxDistanceSq && poiPredicate.test(poiRecord)) {
                consumer.accept(poiRecord);
                if (--limit == 0) {
                    return;
                }
            }
        }
    }

    @Unique
    private void collectMatchingPointsL2Limited(BlockPos center, long maxDistanceSq, Predicate<Holder<PoiType>> typeFilter, Predicate<? super PoiRecord> poiPredicate, Consumer<PoiRecord> consumer, int limit) {
        for (Map.Entry<Holder<PoiType>, Set<PoiRecord>> entry : this.byType.entrySet()) {
            if (!typeFilter.test(entry.getKey()) || entry.getValue().isEmpty()) {
                continue;
            }

            for (PoiRecord poiRecord : entry.getValue()) {
                long distanceSq = Distances.distanceSq(center, poiRecord.getPos());
                if (distanceSq <= maxDistanceSq && poiPredicate.test(poiRecord)) {
                    consumer.accept(poiRecord);
                    if (--limit == 0) {
                        return;
                    }
                }
            }
        }
    }

    @Override
    public PoiRecord lithium$getFirstMatchingPoint(BlockPos pos, long maxDistSq, Predicate<Holder<PoiType>> typeFilter, Predicate<BlockPos> posPredicate, PoiManager.Occupancy status) {
        if (typeFilter instanceof SinglePointOfInterestTypeFilter singleTypeFilter) {
            return this.getFirstMatchingPoint(pos, maxDistSq, singleTypeFilter.getType(), posPredicate, status);
        } else {
            return this.getFirstMatchingPoint(pos, maxDistSq, typeFilter, posPredicate, status);
        }
    }

    @Unique
    private PoiRecord getFirstMatchingPoint(BlockPos pos, long maxDistSq, Holder<PoiType> type, Predicate<BlockPos> posPredicate, PoiManager.Occupancy status) {
        Set<PoiRecord> poiRecords = this.byType.get(type);
        if (poiRecords == null || poiRecords.isEmpty()) {
            return null;
        }

        Predicate<? super PoiRecord> statusPredicate = status.getTest();
        for (PoiRecord poiRecord : poiRecords) {
            long distanceSq = Distances.distanceSq(pos, poiRecord.getPos());
            if (distanceSq <= maxDistSq && posPredicate.test(poiRecord.getPos()) && statusPredicate.test(poiRecord)) {
                return poiRecord;
            }
        }
        return null;
    }

    @Unique
    private PoiRecord getFirstMatchingPoint(BlockPos pos, long maxDistSq, Predicate<Holder<PoiType>> typeFilter, Predicate<BlockPos> posPredicate, PoiManager.Occupancy status) {
        Predicate<? super PoiRecord> statusPredicate = status.getTest();
        for (Map.Entry<Holder<PoiType>, Set<PoiRecord>> entry : this.byType.entrySet()) {
            if (!typeFilter.test(entry.getKey()) || entry.getValue().isEmpty()) {
                continue;
            }

            for (PoiRecord poiRecord : entry.getValue()) {
                long distanceSq = Distances.distanceSq(pos, poiRecord.getPos());
                if (distanceSq <= maxDistSq && posPredicate.test(poiRecord.getPos()) && statusPredicate.test(poiRecord)) {
                    return poiRecord;
                }
            }
        }
        return null;
    }


    @Unique
    private void collectWithDynamicTypeFilter(Predicate<Holder<PoiType>> typeFilter, PoiManager.Occupancy status, Consumer<PoiRecord> consumer) {
        for (Map.Entry<Holder<PoiType>, Set<PoiRecord>> entry : this.byType.entrySet()) {
            if (!typeFilter.test(entry.getKey()) || entry.getValue().isEmpty()) {
                continue;
            }

            for (PoiRecord poi : entry.getValue()) {
                if (status.getTest().test(poi)) {
                    consumer.accept(poi);
                }
            }
        }
    }

    @Unique
    private void collectWithSingleTypeFilter(Holder<PoiType> type, PoiManager.Occupancy status, Consumer<PoiRecord> consumer) {
        Set<PoiRecord> entries = this.byType.get(type);

        if (entries == null || entries.isEmpty()) {
            return;
        }

        for (PoiRecord poi : entries) {
            if (status.getTest().test(poi)) {
                consumer.accept(poi);
            }
        }
    }

    @Override
    public PoiRecord lithium$getAt(BlockPos pos) {
        return this.getPoiRecord(pos).orElse(null);
    }

    @Override
    public Iterator<PoiRecord> lithium$iterate(Predicate<Holder<PoiType>> typeFilter) {
        if (typeFilter instanceof SinglePointOfInterestTypeFilter singleTypeFilter) {
            return this.iterateWithSingleTypeFilter(singleTypeFilter.getType());
        } else {
            return this.iterateWithDynamicTypeFilter(typeFilter);
        }
    }

    @Unique
    private Iterator<PoiRecord> iterateWithSingleTypeFilter(Holder<PoiType> type) {
        Set<PoiRecord> entries = this.byType.get(type);
        if (entries == null || entries.isEmpty()) {
            return Collections.emptyIterator();
        }
        return entries.iterator();
    }

    @Unique
    private Iterator<PoiRecord> iterateWithDynamicTypeFilter(Predicate<Holder<PoiType>> typeFilter) {
        Iterator<Map.Entry<Holder<PoiType>, Set<PoiRecord>>> entryIterator = this.byType.entrySet().iterator();

        return new AbstractIterator<>() {
            private Iterator<PoiRecord> currentSetIterator = Collections.emptyIterator();

            @Override
            protected PoiRecord computeNext() {
                while (true) {
                    if (this.currentSetIterator.hasNext()) {
                        return this.currentSetIterator.next();
                    } else if (entryIterator.hasNext()) {
                        Map.Entry<Holder<PoiType>, Set<PoiRecord>> entry = entryIterator.next();
                        if (typeFilter.test(entry.getKey())) {
                            this.currentSetIterator = entry.getValue().iterator();
                        }
                    } else {
                        return this.endOfData();
                    }
                }
            }
        };
    }
}

package net.caffeinemc.mods.lithium.mixin.ai.poi.tasks;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.caffeinemc.mods.lithium.common.util.Distances;
import net.caffeinemc.mods.lithium.common.util.tuples.Tuple5Obj1I;
import net.caffeinemc.mods.lithium.common.world.interests.PointOfInterestStorageExtended;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.behavior.AcquirePoi;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Collection;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.Stream;

@Mixin(value = AcquirePoi.class, priority = 500)
public class AcquirePoiMixin {

    @Redirect(
            method = { "method_46880", "lambda$create$6" }, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/village/poi/PoiManager;take(Ljava/util/function/Predicate;Ljava/util/function/BiPredicate;Lnet/minecraft/core/BlockPos;I)Ljava/util/Optional;")
    )
    private static Optional<BlockPos> takeOptimized(PoiManager instance, Predicate<Holder<PoiType>> predicate, BiPredicate<Holder<PoiType>, BlockPos> biPredicate, BlockPos blockPos, int radius) {
        return ((PointOfInterestStorageExtended) instance).lithium$takeAt(predicate, biPredicate, blockPos);
    }


    @Redirect(
            method = { "method_46885", "lambda$create$8" },
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/village/poi/PoiManager;findAllClosestFirstWithType(Ljava/util/function/Predicate;Ljava/util/function/Predicate;Lnet/minecraft/core/BlockPos;ILnet/minecraft/world/entity/ai/village/poi/PoiManager$Occupancy;)Ljava/util/stream/Stream;")
    )
    private static Stream<Pair<Holder<PoiType>, BlockPos>> getNull(
            PoiManager poiManager,
            Predicate<Holder<PoiType>> predicate,
            Predicate<BlockPos> filter,
            BlockPos center,
            int radius,
            PoiManager.Occupancy occupancy,
            @Share("findAllClosestFirstWithTypeArguments") LocalRef<Tuple5Obj1I<PoiManager, Predicate<Holder<PoiType>>, Predicate<BlockPos>, BlockPos, PoiManager.Occupancy>> invocationArgs
    ) {

        //Capture the method args and later capture the limit call's constant.
        invocationArgs.set(new Tuple5Obj1I<>(poiManager, predicate, filter, center, occupancy, radius));
        return null; //Mixin below handles the null value
    }

    @Redirect(
            method = { "method_46885", "lambda$create$8" },
            at = @At(value = "INVOKE", target = "Ljava/util/stream/Stream;limit(J)Ljava/util/stream/Stream;")
    )
    private static Stream<Pair<Holder<PoiType>, BlockPos>> getNClosestFirstWithType(
            Stream<Pair<Holder<PoiType>, BlockPos>> stream,
            long limit,
            @Local(argsOnly = true) Long2ObjectMap<AcquirePoi.JitteredLinearRetry> batchCache,
            @Local(argsOnly = true) long timestamp,
            @Share("findAllClosestFirstWithTypeArguments") LocalRef<Tuple5Obj1I<PoiManager, Predicate<Holder<PoiType>>, Predicate<BlockPos>, BlockPos, PoiManager.Occupancy>> invocationArgsRef
    ) {
        if (stream == null && invocationArgsRef != null) {
            var invocationArgs = invocationArgsRef.get();
            PoiManager poiManager = invocationArgs.a();
            Predicate<Holder<PoiType>> typeFilter = invocationArgs.b();

            Predicate<BlockPos> sideEffectfulPosPredicate = invocationArgs.c();
            Predicate<BlockPos> sideEffectlessPosPredicate = pos -> {
                AcquirePoi.JitteredLinearRetry retryMarker = batchCache.get(pos.asLong());
                return retryMarker == null || retryMarker.shouldRetry(timestamp);
            };

            BlockPos center = invocationArgs.d();
            PoiManager.Occupancy status = invocationArgs.e();
            int radius = invocationArgs.f();

            //Run optimized implementation using side effect-less predicate
            Collection<Pair<Holder<PoiType>, BlockPos>> pois = ((PointOfInterestStorageExtended) poiManager).lithium$getNClosestFirstWithType(typeFilter, sideEffectlessPosPredicate, center, radius, status, limit);
            //Apply side effects separately, since vanilla applies the side effects to entire search volume (due to sort in stream after filter):
            if (!batchCache.isEmpty()) {
                long radiusSq = (long) radius * (long) radius;
                batchCache.forEach((longPos, mutableRetryMarker) -> {
                    BlockPos poiPos = BlockPos.of(longPos);
                    if (Distances.distanceSq(poiPos, center) <= radiusSq) {
                        if (poiManager.exists(poiPos, typeFilter)) {
                            sideEffectfulPosPredicate.test(poiPos);
                        }
                    }
                });
            }
            return pois.stream();
        }
        return stream;
    }
}

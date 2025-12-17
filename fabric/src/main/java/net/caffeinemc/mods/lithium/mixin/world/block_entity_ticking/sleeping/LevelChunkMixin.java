package net.caffeinemc.mods.lithium.mixin.world.block_entity_ticking.sleeping;

import com.llamalad7.mixinextras.sugar.Local;
import net.caffeinemc.mods.lithium.common.block.entity.SleepingBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelChunk.class)
public class LevelChunkMixin {

    @SuppressWarnings("UnresolvedMixinReference")
    @Inject(
            method = { "method_31719", "lambda$updateBlockEntityTicker$9" }, // Fabric, Neoforge
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addBlockEntityTicker(Lnet/minecraft/world/level/block/entity/TickingBlockEntity;)V")
    )
    private void setBlockEntityTickingOrder(BlockEntity blockEntity, BlockEntityTicker<?> blockEntityTicker, BlockPos pos, LevelChunk.RebindableTickingBlockEntityWrapper wrappedBlockEntityTickInvoker, CallbackInfoReturnable<?> cir, @Local(ordinal = 1) LevelChunk.RebindableTickingBlockEntityWrapper wrappedBlockEntityTickInvoker2) {
        if (blockEntity instanceof SleepingBlockEntity sleepingBlockEntity) {
            sleepingBlockEntity.lithium$setTickWrapper((WrappedBlockEntityTickInvokerAccessor) wrappedBlockEntityTickInvoker2);
        }
    }

    @SuppressWarnings("UnresolvedMixinReference")
    @Inject(
            method = { "method_31719", "lambda$updateBlockEntityTicker$9" }, // Fabric, Neoforge
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/LevelChunk$RebindableTickingBlockEntityWrapper;rebind(Lnet/minecraft/world/level/block/entity/TickingBlockEntity;)V")
    )
    private void setBlockEntityTickingOrder(BlockEntity blockEntity, BlockEntityTicker<?> blockEntityTicker, BlockPos pos, LevelChunk.RebindableTickingBlockEntityWrapper wrappedBlockEntityTickInvoker, CallbackInfoReturnable<?> cir) {
        if (blockEntity instanceof SleepingBlockEntity sleepingBlockEntity) {
            sleepingBlockEntity.lithium$setTickWrapper((WrappedBlockEntityTickInvokerAccessor) wrappedBlockEntityTickInvoker);
        }
    }

}

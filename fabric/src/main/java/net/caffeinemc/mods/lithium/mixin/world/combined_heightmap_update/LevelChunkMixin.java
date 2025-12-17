package net.caffeinemc.mods.lithium.mixin.world.combined_heightmap_update;

import com.llamalad7.mixinextras.sugar.Local;
import net.caffeinemc.mods.lithium.common.world.chunk.heightmap.CombinedHeightmapUpdate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.blending.BlendingData;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(LevelChunk.class)
public abstract class LevelChunkMixin extends ChunkAccess {

    public LevelChunkMixin(ChunkPos chunkPos, UpgradeData upgradeData, LevelHeightAccessor levelHeightAccessor, PalettedContainerFactory palettedContainerFactory, long l, @Nullable LevelChunkSection[] levelChunkSections, @Nullable BlendingData blendingData) {
        super(chunkPos, upgradeData, levelHeightAccessor, palettedContainerFactory, l, levelChunkSections, blendingData);
    }

    @Redirect(
            method = "setBlockState(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Lnet/minecraft/world/level/block/state/BlockState;",
            at = @At(value = "INVOKE", target = "Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;")
    )
    private <K, V> V skipGetHeightmap(Map<K, V> heightmaps, K heightmapType) {
        if (heightmapType == Heightmap.Types.MOTION_BLOCKING || heightmapType == Heightmap.Types.MOTION_BLOCKING_NO_LEAVES || heightmapType == Heightmap.Types.OCEAN_FLOOR || heightmapType == Heightmap.Types.WORLD_SURFACE) {
            return null;
        }
        return heightmaps.get(heightmapType);
    }

    @Redirect(
            method = "setBlockState(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Lnet/minecraft/world/level/block/state/BlockState;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/Heightmap;update(IIILnet/minecraft/world/level/block/state/BlockState;)Z")
    )
    private boolean skipHeightmapUpdate(Heightmap instance, int x, int y, int z, BlockState state) {
        if (instance == null) {
            return false;
        }
        return instance.update(x, y, z, state);
    }

    @Inject(
            method = "setBlockState(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Lnet/minecraft/world/level/block/state/BlockState;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/levelgen/Heightmap;update(IIILnet/minecraft/world/level/block/state/BlockState;)Z",
                    ordinal = 0
            )
    )
    private void updateHeightmapsCombined(BlockPos blockPos, BlockState blockState, int i, CallbackInfoReturnable<BlockState> cir, @Local(ordinal = 1) int y, @Local(ordinal = 2) int x, @Local(ordinal = 4) int z) {
        Heightmap heightmap0 = this.heightmaps.get(Heightmap.Types.MOTION_BLOCKING); //TODO check if local caputure is correct
        Heightmap heightmap1 = this.heightmaps.get(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES);
        Heightmap heightmap2 = this.heightmaps.get(Heightmap.Types.OCEAN_FLOOR);
        Heightmap heightmap3 = this.heightmaps.get(Heightmap.Types.WORLD_SURFACE);
        CombinedHeightmapUpdate.updateHeightmaps(heightmap0, heightmap1, heightmap2, heightmap3, (LevelChunk) (ChunkAccess) this, x, y, z, blockState);
    }
}

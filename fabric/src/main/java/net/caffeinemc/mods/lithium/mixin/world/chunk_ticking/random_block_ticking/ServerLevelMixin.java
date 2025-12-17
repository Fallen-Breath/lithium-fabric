package net.caffeinemc.mods.lithium.mixin.world.chunk_ticking.random_block_ticking;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.caffeinemc.mods.lithium.common.block.BlockCountingSection;
import net.caffeinemc.mods.lithium.common.block.BlockStateFlags;
import net.caffeinemc.mods.lithium.common.world.section.LithiumSectionData;
import net.caffeinemc.mods.lithium.common.world.section.RandomTickingSectionDataHelper;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerEntityGetter;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.WritableLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Slice;

/**
 * Optimize random ticks without changing their random distribution:
 * <p>
 * Chunk sections keep a counter about the random tickable block positions (fluid or block random tickable).
 * When the number of random tickable positions is low, choose a random tickable block, instead of randomly
 * choosing positions and checking whether there is a random tickable block or fluid at the position.
 * To avoid increasing the random tick speed implicitly, the chance to pick a random tickable block at all is
 * N in 4096, where N is the number of random tickable blocks in that section, hence the counter described above.
 * <p>
 * Having to find the chosen random tickable block in the chunk section is expensive, as a linear search has to access
 * half of all blocks in the chunk sections on average.
 * To speed this up, additional data in form of a byte array is stored and kept up to date at all times:
 * The section is split into 17 mini-sections (< 256 blocks each), for each of which the number of random tickable
 * positions inside is stored in a byte. This allows quickly finding the corresponding mini-section of the chosen
 * random tickable position, followed by a shorter linear search within the mini-section.
 * See also {@link RandomTickingSectionDataHelper#randomTickNthBlock(LevelChunkSection, int, byte[], ServerLevel, int, int, int, RandomSource)}
 */
@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin extends Level implements ServerEntityGetter, WorldGenLevel {

    //Magic number tuned to switch between vanilla and lithium's implementation. Lithium's implementation is only faster
    // when the number of randomtickable blocks in a section is relatively low. Value was determined by trying around.
    // Some results: For grass covered flatworld, values higher than 256 (all chunks have 0 or 256) are better. Nether needs something below 500 for good performance, probably because there is a split between sections with random lava and sections with lava lakes.

    @Unique
    private static final int MAX_COUNT_FOR_BLOCK_SEARCH_AFTER_RANDOM_CHANCE = (int) (4096 * 0.09375);

    protected ServerLevelMixin(WritableLevelData writableLevelData, ResourceKey<Level> resourceKey, RegistryAccess registryAccess, Holder<DimensionType> holder, boolean bl, boolean bl2, long l, int i) {
        super(writableLevelData, resourceKey, registryAccess, holder, bl, bl2, l, i);
    }

    @ModifyExpressionValue(
            method = "tickChunk(Lnet/minecraft/world/level/chunk/LevelChunk;I)V",
            at = @At(value = "CONSTANT", args = "intValue=0"),
            slice = @Slice(
                    from = @At(value = "INVOKE", target = "Lnet/minecraft/core/SectionPos;sectionToBlockCoord(I)I"),
                    to = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;getBlockRandomPos(IIII)Lnet/minecraft/core/BlockPos;", ordinal = 1)
            )
    )
    private int lithiumRandomTick(int original, @Local LevelChunkSection levelChunkSection, @Local(ordinal = 0, argsOnly = true) int randomTickSpeed, @Local(ordinal = 1) int chunkX, @Local(ordinal = 5) int sectionY, @Local(ordinal = 2) int chunkZ) {
        short randomTickableStatesCount = ((BlockCountingSection) levelChunkSection).lithium$getCount(BlockStateFlags.RANDOM_TICKING);
        if (randomTickableStatesCount <= MAX_COUNT_FOR_BLOCK_SEARCH_AFTER_RANDOM_CHANCE) {
            for (int p = 0; p < randomTickSpeed; p++) {
                int randomBlockIndex = this.getRandomBlockIndexForRandomTick();
                if (randomBlockIndex < randomTickableStatesCount) { // N in 4096 chance to hit a random tickable block, where N is the number of random tickable blocks in the 16*16*16 chunk section
                    //Random tickable block was hit -> must find and random tick that block
                    RandomTickingSectionDataHelper.randomTickNthBlock(levelChunkSection, randomBlockIndex, ((LithiumSectionData) levelChunkSection).lithium$getSectionData().getRandomTickableBlocksByY(), (ServerLevel) (Object) this, chunkX, sectionY, chunkZ, this.random);
                    randomTickableStatesCount = ((BlockCountingSection) levelChunkSection).lithium$getCount(BlockStateFlags.RANDOM_TICKING);
                }
            }
            //High return value cancels the vanilla code
            return randomTickSpeed;
        }
        //Run the vanilla code instead, due to it being faster when many random tickable blocks are present.
        return original;
    }

    /**
     * [VanillaCopy] from getBlockRandomPos, but without BlockPos allocation and with possibly different usage of random bits
     *
     * @return random block index in range [0..4095]
     */
    @Unique
    private int getRandomBlockIndexForRandomTick() {
        this.randValue = this.randValue * 3 + 1013904223;
        int r = this.randValue >> 2;
        //Use the same bits as vanilla, but pack them into an integer instead of a BlockPos, which can be interpreted as number in [0..4095]
        return (r & 15) | (r >> 8 & 0xf00) | (r >> 4 & 0xf0);
    }
}

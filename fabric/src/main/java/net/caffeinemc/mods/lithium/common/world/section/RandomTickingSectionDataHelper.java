package net.caffeinemc.mods.lithium.common.world.section;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import net.caffeinemc.mods.lithium.common.block.BlockCountingSection;
import net.caffeinemc.mods.lithium.common.block.BlockStateFlagHolder;
import net.caffeinemc.mods.lithium.common.block.BlockStateFlags;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.Palette;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public class RandomTickingSectionDataHelper {

    //The range 0..256 does not fit into 8 bit, that is why this only goes to 248.
    public static final int MINISECTION_SIZE = 248;

    public static final int MINISECTION_BITS = Mth.ceillog2(MINISECTION_SIZE);
    public static final int MINISECTION_COUNT = Mth.ceil(4096 / (float) MINISECTION_SIZE);
    public static final int MINISECTIONS_PER_BYTE = 8 / MINISECTION_BITS;
    public static final int BYTE_COUNT = Mth.ceil(MINISECTION_COUNT / (float) MINISECTIONS_PER_BYTE);

    public static final int RANDOM_TICKING_FLAG_MASK = 1 << BlockStateFlags.RANDOM_TICKING.getIndex();

    public static void naiveInitializeData(LevelChunkSection section, byte[] data) {
        if (data.length != BYTE_COUNT) {
            throw new IllegalArgumentException("Invalid data length: " + data.length + ", expected " + BYTE_COUNT);
        }
        Arrays.fill(data, 0, BYTE_COUNT, (byte) 0);

        if (!section.maybeHas(blockState -> blockState.isRandomlyTicking() || blockState.getFluidState().isRandomlyTicking())) {
            return;
        }
        PalettedContainer<BlockState> states = section.getStates();
        for (int blockIndex = 0; blockIndex < 4096; blockIndex++) {
            BlockState blockState = states.get(unpackX(blockIndex), unpackY(blockIndex), unpackZ(blockIndex));
            if ((((BlockStateFlagHolder) blockState).lithium$getAllFlags() & RANDOM_TICKING_FLAG_MASK) != 0) {
                int byteIndex = blockIndex / MINISECTION_SIZE;
                data[byteIndex]++;
            }
        }
    }

    /**
     * Block counter that also initializes the minisection counts
     */
    public static class LithiumBlockCounter implements PalettedContainer.CountConsumer<BlockState> {

        private final byte[] randomTickData;
        private byte lastRandomTickableBlockCountTotal;
        private int minisectionIndex;

        private final PalettedContainer.CountConsumer<BlockState> delegate;

        public LithiumBlockCounter(byte[] randomTickData, PalettedContainer.CountConsumer<BlockState> original) {
            this.randomTickData = randomTickData;
            this.lastRandomTickableBlockCountTotal = 0;
            this.minisectionIndex = 0;
            this.delegate = original;
        }

        @Override
        public void accept(@NotNull BlockState blockState, int i) {
            this.delegate.accept(blockState, i);
        }

        public void finishedCountingMinisection(Int2IntOpenHashMap indexCounts, Palette<BlockState> palette) {
            //A bunch of bytes can over- and underflow here, but actually it is no issue
            //Subtract the previous total first, since the new total is added in the forEach below
            this.randomTickData[this.minisectionIndex] -= this.lastRandomTickableBlockCountTotal;
            indexCounts.int2IntEntrySet().forEach(entry -> {
                BlockState blockState = palette.valueFor(entry.getIntKey());
                if ((((BlockStateFlagHolder) blockState).lithium$getAllFlags() & RANDOM_TICKING_FLAG_MASK) != 0) {
                    this.randomTickData[this.minisectionIndex] += (byte) entry.getIntValue();
                }
            });
            this.lastRandomTickableBlockCountTotal += this.randomTickData[this.minisectionIndex];

            this.minisectionIndex++;
        }

        public void handleAfterCounting(LevelChunkSection section) {
            if (MINISECTION_COUNT != this.minisectionIndex) { //Mod compatibility issue fallback - Mixin in PalettedContainer could not detect our counter, as another mod wrapped it again.
                if (this.randomTickData != ((LithiumSectionData) section).lithium$getSectionData().getRandomTickableBlocksByY()) {
                    throw new IllegalArgumentException("Lithium random tick data was replaced unexpectedly!");
                }
                Arrays.fill(this.randomTickData, 0, this.randomTickData.length, (byte) 0);
                naiveInitializeData(section, this.randomTickData);
            }

            assert 0 == sanityCheckRandomTickableBlockCount((BlockCountingSection) section);
        }

        private int sanityCheckRandomTickableBlockCount(BlockCountingSection section) {
            // Sanity check: Total random block count equals sum of minisection counts:

            int randomTickableStatesCount = section.lithium$getCount(BlockStateFlags.RANDOM_TICKING);
            int sum = 0;
            for (byte randomTickDatum : this.randomTickData) {
                sum += Byte.toUnsignedInt(randomTickDatum);
            }
            if (randomTickableStatesCount != sum) {
                throw new IllegalStateException("Lithium random tick data initialization calculated inconsistent results: " + randomTickableStatesCount + " != " + sum);
            }
            return 0;
        }
    }

    public static void randomTickNthBlock(LevelChunkSection section, int randomBlockIndex, byte[] data, ServerLevel level, int sectionBlockX, int sectionBlockY, int sectionBlockZ, RandomSource random) {
        assert data.length == BYTE_COUNT;
        //Section block coordinates must be the lower corner of the chunk section
        assert (sectionBlockX & 0xF) == 0;
        assert (sectionBlockY & 0xF) == 0;
        assert (sectionBlockZ & 0xF) == 0;

        //Find the corresponding minisection that the block is in
        int minisectionIndex = 0;
        for (; minisectionIndex < data.length; minisectionIndex++) {
            int countInMinisection = Byte.toUnsignedInt(data[minisectionIndex]);
            if (randomBlockIndex >= countInMinisection) {
                randomBlockIndex -= countInMinisection;
            } else {
                break;
            }
        }
        //Search from the minisection start until hitting the block, which should be in that minisection
        for (int blockIndex = minisectionIndex * MINISECTION_SIZE; blockIndex < 4096; blockIndex++) {

            //Convert [0..4095] to x,y,z in [0..15], consistently with the minisections and in the order vanilla stores the blocks in the bit storage
            int x = unpackX(blockIndex);
            int y = unpackY(blockIndex);
            int z = unpackZ(blockIndex);

            BlockState blockState = section.getBlockState(x, y, z);
            if ((((BlockStateFlagHolder) blockState).lithium$getAllFlags() & RANDOM_TICKING_FLAG_MASK) != 0) {
                if (randomBlockIndex-- == 0) {
                    //Vanilla always ticks the block first followed by the fluid with an immutable block position. Do not use Mutable block pos here.
                    BlockPos immutableRandomTickPos = new BlockPos(sectionBlockX | x, sectionBlockY | y, sectionBlockZ | z);
                    if (blockState.isRandomlyTicking()) {
                        blockState.randomTick(level, immutableRandomTickPos, random);
                    }
                    FluidState fluidState = blockState.getFluidState();
                    if (fluidState.isRandomlyTicking()) {
                        fluidState.randomTick(level, immutableRandomTickPos, random);
                    }
                    return;
                }
            }
        }

        throw new IllegalStateException("Failed to find random tickable position! This means lithium's random tickable block optimization encountered inconsistent data, hinting at a mod compatibility issue.");
    }

    public static void addAt(int x, int y, int z, byte[] data) {
        data[getMinisectionIndex(x, y, z)]++;
    }

    public static void removeAt(int x, int y, int z, byte[] data) {
        data[getMinisectionIndex(x, y, z)]--;
    }

    public static int getMinisectionIndex(int x, int y, int z) {
        return pack(x, y, z) / MINISECTION_SIZE;
    }

    private static int pack(int x, int y, int z) {
        return (y << 4 | z) << 4 | x;
    }

    public static int unpackX(int index) {
        return index & 0xF;
    }

    public static int unpackY(int index) {
        return index >> 8 & 0xF;
    }

    public static int unpackZ(int index) {
        return index >> 4 & 0xF;
    }
}

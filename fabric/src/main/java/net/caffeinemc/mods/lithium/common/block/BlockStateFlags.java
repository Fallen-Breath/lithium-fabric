package net.caffeinemc.mods.lithium.common.block;

import net.caffeinemc.mods.lithium.common.ai.pathing.BlockStatePathingCache;
import net.caffeinemc.mods.lithium.common.ai.pathing.PathNodeCache;
import net.caffeinemc.mods.lithium.common.entity.FluidCachingEntity;
import net.caffeinemc.mods.lithium.common.reflection.ReflectionUtil;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.pathfinder.PathType;

import java.util.ArrayList;

public class BlockStateFlags {
    public static final boolean ENABLED = BlockCountingSection.class.isAssignableFrom(LevelChunkSection.class);
    public static final int NUM_TRACKED_FLAGS;
    public static final TrackedBlockStatePredicate[] TRACKED_FLAGS;

    //Counting flags
    public static final TrackedBlockStatePredicate OVERSIZED_SHAPE;
    public static final TrackedBlockStatePredicate PATH_NOT_OPEN;
    public static final TrackedBlockStatePredicate WATER;
    public static final TrackedBlockStatePredicate LAVA;

    public static final TrackedBlockStatePredicate[] FLAGS;

    //Non counting flags
    public static final TrackedBlockStatePredicate ENTITY_TOUCHABLE;
    public static final TrackedBlockStatePredicate RANDOM_TICKING;

    static {
        ArrayList<TrackedBlockStatePredicate> countingFlags = new ArrayList<>();

        //TODO add each flag if and only if it is going to be used (corresponding mixins enabled)
        //noinspection ConstantValue
        OVERSIZED_SHAPE = new TrackedBlockStatePredicate(countingFlags.size()) {
            @Override
            public boolean test(BlockState operand) {
                return operand.hasLargeCollisionShape();
            }
        };
        countingFlags.add(OVERSIZED_SHAPE);

        if (FluidCachingEntity.class.isAssignableFrom(Entity.class)) {
            WATER = new TrackedBlockStatePredicate(countingFlags.size()) {
                @Override
                public boolean test(BlockState operand) {
                    return operand.getFluidState().getType().is(FluidTags.WATER);
                }
            };
            countingFlags.add(WATER);

            LAVA = new TrackedBlockStatePredicate(countingFlags.size()) {
                @Override
                public boolean test(BlockState operand) {
                    return operand.getFluidState().getType().is(FluidTags.LAVA);
                }
            };
            countingFlags.add(LAVA);
        } else {
            WATER = null;
            LAVA = null;
        }

        if (BlockStatePathingCache.class.isAssignableFrom(BlockBehaviour.BlockStateBase.class)) {
            PATH_NOT_OPEN = new TrackedBlockStatePredicate(countingFlags.size()) {
                @Override
                public boolean test(BlockState operand) {
                    return PathNodeCache.getNeighborPathNodeType(operand) != PathType.OPEN;
                }
            };
            countingFlags.add(PATH_NOT_OPEN);
        } else {
            PATH_NOT_OPEN = null;
        }

        RANDOM_TICKING = new TrackedBlockStatePredicate(countingFlags.size()) {
            @Override
            public boolean test(BlockState operand) {
                return operand.isRandomlyTicking() || operand.getFluidState().isRandomlyTicking();
            }
        };
        countingFlags.add(RANDOM_TICKING);

        NUM_TRACKED_FLAGS = countingFlags.size();
        TRACKED_FLAGS = countingFlags.toArray(new TrackedBlockStatePredicate[NUM_TRACKED_FLAGS]);

        ArrayList<TrackedBlockStatePredicate> flags = new ArrayList<>(countingFlags);

        ENTITY_TOUCHABLE = new TrackedBlockStatePredicate(flags.size()) {
            @Override
            public boolean test(BlockState operand) {
                return ReflectionUtil.isBlockStateEntityTouchable(operand) || operand.is(Blocks.LAVA) || operand.is(BlockTags.FIRE); //Fire and Lava explicit as they need to be added to the set of touched blocks too
            }
        };
        flags.add(ENTITY_TOUCHABLE);


        FLAGS = flags.toArray(new TrackedBlockStatePredicate[0]);
    }
}

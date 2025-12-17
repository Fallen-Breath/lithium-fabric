package net.caffeinemc.mods.lithium.common.client;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.caffeinemc.mods.lithium.common.util.collections.DummyList;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import org.spongepowered.asm.mixin.Unique;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class SharedFields {
    public static final AtomicInteger MAXIMUM_BIOME_PARTICLE_CHANCE = new AtomicInteger(Float.floatToIntBits(0.0F)); //Using atomic integer as replacement for atomic float
    @Unique
    public static final Brain<?> DUMMY_BRAIN = new Brain<>(new DummyList<>(), List.of(), ImmutableList.of(), () -> new Codec<>() {
        @Override
        public <T> DataResult<Pair<Brain<LivingEntity>, T>> decode(DynamicOps<T> dynamicOps, T t) {
            throw new IllegalStateException("Trying to decode client side brain! If you really want this, disable lithium's client side brain optimization!");
        }

        @Override
        public <T> DataResult<T> encode(Brain<LivingEntity> livingEntityBrain, DynamicOps<T> dynamicOps, T t) {
            return DataResult.success(t); //Encode nothing, compatible with mods that just want the nbt of the entire mob for something else
        }
    });
}

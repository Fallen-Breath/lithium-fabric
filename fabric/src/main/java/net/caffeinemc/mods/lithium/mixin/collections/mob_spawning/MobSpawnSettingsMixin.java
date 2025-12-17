package net.caffeinemc.mods.lithium.mixin.collections.mob_spawning;

import java.util.Map;

import com.google.common.collect.Maps;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.MobSpawnSettings;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MobSpawnSettings.class)
public class MobSpawnSettingsMixin {
    @Mutable
    @Shadow
    @Final
    private Map<MobCategory, WeightedList<MobSpawnSettings.SpawnerData>> spawners;

    /**
     * Re-initialize the spawn category lists with a much faster backing collection type for enum keys. This provides
     * a modest speed-up for mob spawning as {@link MobSpawnSettings#getMobs(MobCategory)} is a rather hot method.
     */
    @Inject(method = "<init>(FLjava/util/Map;Ljava/util/Map;)V", at = @At("RETURN"))
    private void reinit(float creatureSpawnProbability, Map<MobCategory, WeightedList<MobSpawnSettings.SpawnerData>> spawners, Map<EntityType<?>, MobSpawnSettings.MobSpawnCost> spawnCosts, CallbackInfo ci) {
        Map<MobCategory, WeightedList<MobSpawnSettings.SpawnerData>> spawns = Maps.newEnumMap(MobCategory.class);

        for (Map.Entry<MobCategory, WeightedList<MobSpawnSettings.SpawnerData>> entry : this.spawners.entrySet()) {
            spawns.put(entry.getKey(), entry.getValue());
        }

        this.spawners = spawns;
    }
}

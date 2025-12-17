package net.caffeinemc.mods.lithium.mixin.gen.cached_generator_settings;

import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(NoiseBasedChunkGenerator.class)
public class NoiseBasedChunkGeneratorMixin {

    @Shadow
    @Final
    private Holder<NoiseGeneratorSettings> settings;
    private int cachedSeaLevel = Integer.MIN_VALUE;

    /**
     * Use cached sea level instead of retrieving from the registry every time.
     * This method is called for every block in the chunk so this will save a lot of registry lookups.
     * <p>
     * Lazy initialization added to avoid unbound registry value crash
     *
     * @author SuperCoder79, 2No2Name
     * @reason avoid registry lookup
     */
    @Overwrite
    public int getSeaLevel() {
        if (cachedSeaLevel == Integer.MIN_VALUE) {
            this.cachedSeaLevel = this.settings.value().seaLevel();
        }
        return this.cachedSeaLevel;
    }
}

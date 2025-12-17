package net.caffeinemc.mods.lithium.mixin.util.section_data_storage;

import net.caffeinemc.mods.lithium.common.world.section.LithiumSectionData;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(LevelChunkSection.class)
public class LevelChunkSectionMixin implements LithiumSectionData {
    @Unique
    private SectionData sectionData;

    @Override
    public SectionData lithium$getSectionData() {
        if (this.sectionData == null) {
            this.sectionData = new SectionData((LevelChunkSection) (Object) this);
        }
        return this.sectionData;
    }

    @Override
    public SectionData lithium$getSectionDataDirect() {
        if (this.sectionData == null) {
            throw new NullPointerException("SectionData has not been created yet!");
        }
        return this.sectionData;
    }
}

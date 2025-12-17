package net.caffeinemc.mods.lithium.mixin.entity.collisions.unpushable_cramming;

import net.caffeinemc.mods.lithium.common.entity.pushable.FeetBlockCachingEntity;
import net.caffeinemc.mods.lithium.common.world.ClimbingMobCachingSection;
import net.caffeinemc.mods.lithium.common.world.WorldHelper;
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntitySection;
import net.minecraft.world.level.entity.EntitySectionStorage;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity implements FeetBlockCachingEntity {

    boolean updateClimbingMobCachingSectionOnChange;

    public LivingEntityMixin(EntityType<?> type, Level world) {
        super(type, world);
    }

    @Override
    public void lithium$SetClimbingMobCachingSectionUpdateBehavior(boolean listenForCachedBlockChanges) {
        this.updateClimbingMobCachingSectionOnChange = listenForCachedBlockChanges;
    }

    @Override
    public void lithium$OnFeetBlockCacheDeleted() {
        if (this.updateClimbingMobCachingSectionOnChange) {
            this.updateClimbingMobCachingSection(null);
        }
    }


    @Override
    public void lithium$OnFeetBlockCacheSet(BlockState newState) {
        if (this.updateClimbingMobCachingSectionOnChange) {
            this.updateClimbingMobCachingSection(newState);
        }
    }

    private void updateClimbingMobCachingSection(BlockState newState) {
        EntitySectionStorage<Entity> entityCacheOrNull = WorldHelper.getEntityCacheOrNull(this.level());
        if (entityCacheOrNull != null) {
            EntitySection<Entity> trackingSection = entityCacheOrNull.getSection(SectionPos.asLong(this.blockPosition()));
            if (trackingSection != null) {
                ((ClimbingMobCachingSection) trackingSection).lithium$onEntityModifiedCachedBlock(this, newState);
            } else {
                this.updateClimbingMobCachingSectionOnChange = false;
            }
        }
    }
}

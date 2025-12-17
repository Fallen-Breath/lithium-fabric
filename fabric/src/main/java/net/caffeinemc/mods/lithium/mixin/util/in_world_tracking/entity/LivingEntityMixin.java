package net.caffeinemc.mods.lithium.mixin.util.in_world_tracking.entity;

import net.caffeinemc.mods.lithium.common.world.in_world_tracking.MaybeInLevelObject;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityEquipment;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity implements MaybeInLevelObject {

    @Shadow
    @Final
    protected EntityEquipment equipment;

    public LivingEntityMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public void lithium$handleAddedToLevel(Level level) {
        if (this.equipment instanceof MaybeInLevelObject) {
            ((MaybeInLevelObject) this.equipment).lithium$handleAddedToLevel(level);
        }
    }

    @Override
    public void lithium$handleRemovedFromLevel(Level level) {
        if (this.equipment instanceof MaybeInLevelObject) {
            ((MaybeInLevelObject) this.equipment).lithium$handleRemovedFromLevel(level);
        }
    }
}

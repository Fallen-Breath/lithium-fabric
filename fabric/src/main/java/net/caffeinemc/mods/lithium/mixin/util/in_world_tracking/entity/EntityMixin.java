package net.caffeinemc.mods.lithium.mixin.util.in_world_tracking.entity;

import net.caffeinemc.mods.lithium.common.world.in_world_tracking.MaybeInLevelObject;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityInLevelCallback;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMixin implements MaybeInLevelObject {

    @Shadow
    private EntityInLevelCallback levelCallback;

    @Shadow
    public abstract Level level();

    @Override
    public boolean lithium$isInLevel() {
        return this.levelCallback != EntityInLevelCallback.NULL;
    }

    @Inject(
            method = "setLevelCallback", at = @At("RETURN")
    )
    private void emitMaybeInLevelEvents(EntityInLevelCallback entityInLevelCallback, CallbackInfo ci) {
        if (this.levelCallback == EntityInLevelCallback.NULL) {
            this.lithium$handleRemovedFromLevel(this.level());
        } else {
            this.lithium$handleAddedToLevel(this.level());
        }
    }
}

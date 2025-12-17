package net.caffeinemc.mods.lithium.mixin.world.explosions.entity_raycast;

import net.caffeinemc.mods.lithium.common.world.explosions.ClipContextAccess;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ClipContext.class)
public class ClipContextMixin implements ClipContextAccess {
    @Mutable
    @Shadow
    @Final
    private Vec3 from;

    @Shadow
    @Final
    private CollisionContext collisionContext;

    @Override
    public void lithium$setFrom(Vec3 from) {
        this.from = from;
    }

    @Override
    public CollisionContext lithium$getCollisionContext() {
        return this.collisionContext;
    }
}

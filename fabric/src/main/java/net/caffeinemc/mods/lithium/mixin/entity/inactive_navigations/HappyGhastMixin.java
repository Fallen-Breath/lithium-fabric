package net.caffeinemc.mods.lithium.mixin.entity.inactive_navigations;

import net.caffeinemc.mods.lithium.common.entity.NavigatingEntity;
import net.minecraft.world.entity.animal.happyghast.HappyGhast;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HappyGhast.class)
public class HappyGhastMixin {
    @Inject(method = "adultGhastSetup()V", at = @At(value = "RETURN"))
    private void updateRegisteredNavigation(CallbackInfo ci) {
        ((NavigatingEntity) this).lithium$updateNavigationRegistration();
    }

    @Inject(method = "babyGhastSetup()V", at = @At(value = "RETURN"))
    private void updateRegisteredNavigation1(CallbackInfo ci) {
        ((NavigatingEntity) this).lithium$updateNavigationRegistration();
    }
}

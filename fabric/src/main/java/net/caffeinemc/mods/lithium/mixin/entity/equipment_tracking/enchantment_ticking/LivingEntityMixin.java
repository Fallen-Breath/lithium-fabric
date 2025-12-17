package net.caffeinemc.mods.lithium.mixin.entity.equipment_tracking.enchantment_ticking;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.caffeinemc.mods.lithium.common.entity.EquipmentInfo;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityEquipment;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Shadow
    @Final
    protected EntityEquipment equipment;

    @WrapWithCondition(
            method = "baseTick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;tickEffects(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;)V")
    )
    private boolean maybeHasAnyTickableEnchantments(ServerLevel world, LivingEntity user) {
        return user instanceof Player || ((EquipmentInfo) this.equipment).lithium$shouldTickEnchantments();
    }
}

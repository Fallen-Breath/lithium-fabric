package net.caffeinemc.mods.lithium.mixin.collections.mob_spawning;

import java.util.Collection;
import java.util.List;

import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import net.caffeinemc.mods.lithium.common.util.collections.HashedReferenceList;
import net.minecraft.util.random.Weighted;
import net.minecraft.util.random.WeightedList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(WeightedList.class)
public class WeightedListMixin<E extends Weighted<?>> {

    @Mutable
    @Shadow
    @Final
    private List<E> items;

    @Redirect(method = "<init>(Ljava/util/List;)V", at = @At(value = "INVOKE", target = "Ljava/util/List;copyOf(Ljava/util/Collection;)Ljava/util/List;"))
    private List<E> init(Collection<E> coll) {
        //We are using reference equality here, because all vanilla implementations of Weighted use reference equality
        return coll.size() > 4 ? new HashedReferenceList<>(coll) : new ReferenceArrayList<>(coll);
    }
}

@MixinConfigOption(
        description = "In chunks with many mobs in ladders a separate list of pushable entities for cramming tests is used",
        depends = {
                @MixinConfigDependency(dependencyPath = "mixin.chunk.entity_class_groups")
        } // Technically broken due to elytra flight making entities pushable, even when they are in a climbable block.
        // But as only players (which are excluded already) fly with elytra in practice, this doesn't make a noticeable difference
        //TODO explicitly exclude entities that are currently flying with elytra
)
package net.caffeinemc.mods.lithium.mixin.entity.collisions.unpushable_cramming;

import net.caffeinemc.gradle.MixinConfigDependency;
import net.caffeinemc.gradle.MixinConfigOption;